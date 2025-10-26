package ithubsystem;

import javax.swing.*;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

// Import JFreeChart
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;

public class ChartReportDialog extends JFrame {

    // (*** กำหนดค่าสี ***)
    private final Color BG_COLOR_1 = new Color(230, 240, 255); // ฟ้าอ่อน
    private final Color BG_COLOR_2 = new Color(230, 255, 230); // เขียวอ่อน
    private final Color BG_COLOR_3 = new Color(255, 255, 224); // เหลืองอ่อน
    private final Color BG_COLOR_4 = new Color(255, 230, 240); // ชมพูอ่อน
    private final Color BG_COLOR_5 = new Color(245, 245, 245); // เทาอ่อน

    public ChartReportDialog(JFrame owner) {
        setTitle("หน้าต่างแสดงกราฟรายงาน");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width - 50, screenSize.height - 100);
        setLocationRelativeTo(owner);
        setLayout(new GridLayout(3, 2, 10, 10));
        // (*** ตั้งพื้นหลังหลักเป็นสีเทาอ่อนๆ ***)
        getContentPane().setBackground(Color.LIGHT_GRAY);

        // --- แถวที่ 1 ---
        JPanel pnlChart1 = createBestSellerChart();
        add(pnlChart1);
        JPanel pnlChartProfitBar = createProfitBarChart();
        add(pnlChartProfitBar);

        // --- แถวที่ 2 ---
        JPanel pnlChart2 = createCategoryPieChart();
        add(pnlChart2);
        JPanel pnlChartProfitPie = createProfitPieChart();
        add(pnlChartProfitPie);

        // --- แถวที่ 3 ---
        JPanel pnlChart3 = createSalesLineChart();
        add(pnlChart3);

        // (เว้นว่าง)
        JPanel emptyPanel = new JPanel();
        emptyPanel.setBackground(getContentPane().getBackground());
        add(emptyPanel);
    }

    /** (Helper) สร้าง Placeholder */
    private JPanel createChartPlaceholder(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Tahoma", Font.ITALIC, 16));
        label.setForeground(Color.GRAY);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    /** กราฟแท่ง (Bar Chart) "สินค้าขายดี" */
    private JPanel createBestSellerChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String sql = "SELECT p.name, COALESCE(SUM(sd.quantity), 0) AS total_sold FROM sale_details sd JOIN products p ON sd.product_id = p.product_id GROUP BY p.name ORDER BY total_sold DESC LIMIT 5";
        try (Connection conn = DbConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) { dataset.setValue(rs.getInt("total_sold"), "จำนวนที่ขายได้", rs.getString("name")); }
        } catch (SQLException e) { e.printStackTrace(); return createChartPlaceholder("Error loading chart data"); }

        JFreeChart barChart = ChartFactory.createBarChart("สินค้าขายดี 5 อันดับ", "สินค้า", "จำนวน (ชิ้น)", dataset, PlotOrientation.VERTICAL, false, true, false);
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 12); Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14); barChart.getTitle().setFont(thaiBoldFont); CategoryPlot plot = barChart.getCategoryPlot(); plot.getDomainAxis().setLabelFont(thaiBoldFont); plot.getDomainAxis().setTickLabelFont(thaiFont); plot.getRangeAxis().setLabelFont(thaiBoldFont); plot.getRangeAxis().setTickLabelFont(thaiFont); plot.setBackgroundPaint(Color.WHITE); BarRenderer renderer = (BarRenderer) plot.getRenderer(); renderer.setSeriesPaint(0, new Color(79, 129, 189));

        final ChartPanel chartPanel = new ChartPanel(barChart); // <-- ประกาศ final ที่นี่
        chartPanel.setPreferredSize(new Dimension(380, 280));
        chartPanel.setBackground(BG_COLOR_1);
        return chartPanel;
    }

    /** กราฟแท่ง (Bar Chart) "สินค้าทำกำไรสูงสุด 5 อันดับ" */
    private JPanel createProfitBarChart() {
         DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String sql = "SELECT p.name, COALESCE(SUM(sd.quantity * (p.sale_price - p.cost_price)), 0) AS total_profit FROM sale_details sd JOIN products p ON sd.product_id = p.product_id GROUP BY p.name ORDER BY total_profit DESC LIMIT 5";
        try (Connection conn = DbConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) { while (rs.next()) { dataset.setValue(rs.getDouble("total_profit"), "กำไร (บาท)", rs.getString("name")); } } catch (SQLException e) { e.printStackTrace(); return createChartPlaceholder("Error loading profit chart data"); }

        JFreeChart barChart = ChartFactory.createBarChart("สินค้าทำกำไรสูงสุด 5 อันดับ", "สินค้า", "กำไร (บาท)", dataset, PlotOrientation.VERTICAL, false, true, false);
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 12); Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14); barChart.getTitle().setFont(thaiBoldFont); CategoryPlot plot = barChart.getCategoryPlot(); plot.getDomainAxis().setLabelFont(thaiBoldFont); plot.getDomainAxis().setTickLabelFont(thaiFont); plot.getRangeAxis().setLabelFont(thaiBoldFont); plot.getRangeAxis().setTickLabelFont(thaiFont); plot.setBackgroundPaint(Color.WHITE); BarRenderer renderer = (BarRenderer) plot.getRenderer(); renderer.setSeriesPaint(0, new Color(155, 187, 89));

        final ChartPanel chartPanel = new ChartPanel(barChart); // <-- ประกาศ final ที่นี่
        chartPanel.setPreferredSize(new Dimension(380, 280));
        chartPanel.setBackground(BG_COLOR_2);
        return chartPanel;
    }

    /** กราฟวงกลม (Pie Chart) "สัดส่วนยอดขายตามหมวดหมู่" */
    private JPanel createCategoryPieChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        String sql = "SELECT c.name, COALESCE(SUM(sd.quantity * sd.price_per_unit), 0) AS total_value FROM sale_details sd JOIN products p ON sd.product_id = p.product_id JOIN categories c ON p.category_id = c.category_id GROUP BY c.name ORDER BY total_value DESC";
        try (Connection conn = DbConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) { while (rs.next()) { dataset.setValue(rs.getString("name"), rs.getDouble("total_value")); } } catch (SQLException e) { e.printStackTrace(); return createChartPlaceholder("Error loading chart data"); }

        JFreeChart pieChart = ChartFactory.createPieChart("สัดส่วนยอดขายตามหมวดหมู่", dataset, true, true, false);
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 12); Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14); pieChart.getTitle().setFont(thaiBoldFont); pieChart.getLegend().setItemFont(thaiFont); PiePlot plot = (PiePlot) pieChart.getPlot(); plot.setLabelFont(thaiFont); plot.setBackgroundPaint(Color.WHITE); plot.setOutlineVisible(false); plot.setSimpleLabels(true); plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}", NumberFormat.getPercentInstance(), new DecimalFormat("0.0%")));

        // (*** แก้ไข: ประกาศ final ตรงนี้ และมีแค่ครั้งเดียว ***)
        final ChartPanel chartPanel = new ChartPanel(pieChart);
        chartPanel.setPreferredSize(new Dimension(380, 280));
        chartPanel.setBackground(BG_COLOR_3);

        // --- (Listener) ---
        chartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                ChartEntity entity = event.getEntity();
                if (entity instanceof PieSectionEntity) {
                    PieSectionEntity pieEntity = (PieSectionEntity) entity;
                    String categoryKey = pieEntity.getSectionKey().toString();
                    JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(chartPanel); // <-- ใช้ chartPanel ที่เป็น final
                    CategoryDetailDialog detailDialog = new CategoryDetailDialog(owner, categoryKey, "sales");
                    detailDialog.setVisible(true);
                }
            }
            @Override
            public void chartMouseMoved(ChartMouseEvent event) { /* ไม่ใช้ */ }
        });
        // --- (สิ้นสุด Listener) ---

        return chartPanel; // <-- คืนค่า chartPanel ที่สร้างไว้

    } // <--- ปิดเมธอด createCategoryPieChart


    /** กราฟวงกลม (Pie Chart) "สัดส่วนกำไรตามหมวดหมู่" */
    private JPanel createProfitPieChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        String sql = "SELECT c.name, COALESCE(SUM(sd.quantity * (p.sale_price - p.cost_price)), 0) AS total_profit FROM sale_details sd JOIN products p ON sd.product_id = p.product_id JOIN categories c ON p.category_id = c.category_id GROUP BY c.name HAVING total_profit > 0 ORDER BY total_profit DESC";
        try (Connection conn = DbConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) { while (rs.next()) { dataset.setValue(rs.getString("name"), rs.getDouble("total_profit")); } } catch (SQLException e) { e.printStackTrace(); return createChartPlaceholder("Error loading profit chart data"); }

        JFreeChart pieChart = ChartFactory.createPieChart("สัดส่วนกำไรตามหมวดหมู่", dataset, true, true, false);
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 12); Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14); pieChart.getTitle().setFont(thaiBoldFont); pieChart.getLegend().setItemFont(thaiFont); PiePlot plot = (PiePlot) pieChart.getPlot(); plot.setLabelFont(thaiFont); plot.setBackgroundPaint(Color.WHITE); plot.setOutlineVisible(false); plot.setSimpleLabels(true); plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}", NumberFormat.getPercentInstance(), new DecimalFormat("0.0%")));

        // (*** แก้ไข: ประกาศ final ตรงนี้ และมีแค่ครั้งเดียว ***)
        final ChartPanel chartPanel = new ChartPanel(pieChart);
        chartPanel.setPreferredSize(new Dimension(380, 280));
        chartPanel.setBackground(BG_COLOR_4);

        // --- (Listener) ---
        chartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                ChartEntity entity = event.getEntity();
                if (entity instanceof PieSectionEntity) {
                    PieSectionEntity pieEntity = (PieSectionEntity) entity;
                    String categoryKey = pieEntity.getSectionKey().toString();
                    JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(chartPanel); // <-- ใช้ chartPanel ที่เป็น final
                    CategoryDetailDialog detailDialog = new CategoryDetailDialog(owner, categoryKey, "profit"); // <-- profit
                    detailDialog.setVisible(true);
                }
            }
            @Override
            public void chartMouseMoved(ChartMouseEvent event) { /* ไม่ใช้ */ }
        });
        // --- (สิ้นสุด Listener) ---

        return chartPanel; // <-- คืนค่า chartPanel ที่สร้างไว้

    } // <--- ปิดเมธอด createProfitPieChart

    /** กราฟเส้น (Line Chart) "ยอดขาย 30 วันย้อนหลัง" */
    private JPanel createSalesLineChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String sql = "SELECT DATE(sale_date) AS sale_day, SUM(total_amount) AS daily_total FROM sales WHERE sale_date >= CURDATE() - INTERVAL 30 DAY GROUP BY sale_day ORDER BY sale_day ASC";
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");
        try (Connection conn = DbConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) { while (rs.next()) { dataset.setValue(rs.getDouble("daily_total"), "ยอดขาย", dateFormat.format(rs.getDate("sale_day"))); } } catch (SQLException e) { e.printStackTrace(); return createChartPlaceholder("Error loading chart data"); }

        JFreeChart lineChart = ChartFactory.createLineChart("แนวโน้มยอดขาย 30 วันย้อนหลัง", "วันที่", "ยอดขาย (บาท)", dataset, PlotOrientation.VERTICAL, false, true, false);
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 12); Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14); lineChart.getTitle().setFont(thaiBoldFont); CategoryPlot plot = lineChart.getCategoryPlot(); plot.getDomainAxis().setLabelFont(thaiBoldFont); plot.getDomainAxis().setTickLabelFont(thaiFont); plot.getRangeAxis().setLabelFont(thaiBoldFont); plot.getRangeAxis().setTickLabelFont(thaiFont); plot.setBackgroundPaint(Color.WHITE); plot.setRangeGridlinePaint(Color.LIGHT_GRAY); LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer(); renderer.setSeriesStroke(0, new java.awt.BasicStroke(2.0f)); renderer.setSeriesPaint(0, Color.BLUE);

        final ChartPanel chartPanel = new ChartPanel(lineChart); // <-- ประกาศ final ที่นี่
        chartPanel.setPreferredSize(new Dimension(750, 280));
        chartPanel.setBackground(BG_COLOR_5);
        return chartPanel;
    }
}