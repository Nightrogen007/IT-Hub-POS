package ithubsystem;

import javax.swing.*;
import java.awt.GridBagLayout;
import org.jfree.chart.axis.CategoryLabelPositions; // (*** ใหม่ ***)
import java.awt.GridBagConstraints;
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
        // (*** แก้ไข: ลดความสูงลงเล็กน้อย เผื่อ Taskbar ***)
        setSize(screenSize.width - 50, screenSize.height - 100);
        setLocationRelativeTo(owner);

        // (*** เปลี่ยน Layout เป็น GridBagLayout ***)
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; // ให้ Panel ขยายเต็มช่อง
        gbc.weightx = 0.5; // แบ่งความกว้างเท่าๆ กัน (สำหรับ 2 คอลัมน์แรก)
        gbc.weighty = 0.33; // แบ่งความสูงประมาณ 1/3
        gbc.insets = new java.awt.Insets(5, 5, 5, 5); // เพิ่มช่องไฟรอบๆ

        // --- แถวที่ 1 ---
        gbc.gridx = 0; // คอลัมน์ 0
        gbc.gridy = 0; // แถว 0
        JPanel pnlChart1 = createBestSellerChart();
        add(pnlChart1, gbc);

        gbc.gridx = 1; // คอลัมน์ 1
        gbc.gridy = 0; // แถว 0
        JPanel pnlChartProfitBar = createProfitBarChart();
        add(pnlChartProfitBar, gbc);

        // --- แถวที่ 2 ---
        gbc.gridx = 0; // คอลัมน์ 0
        gbc.gridy = 1; // แถว 1
        JPanel pnlChart2 = createCategoryPieChart();
        add(pnlChart2, gbc);

        gbc.gridx = 1; // คอลัมน์ 1
        gbc.gridy = 1; // แถว 1
        JPanel pnlChartProfitPie = createProfitPieChart();
        add(pnlChartProfitPie, gbc);

        // --- แถวที่ 3 (กราฟเส้น - ขยายเต็ม) ---
        gbc.gridx = 0; // เริ่มที่คอลัมน์ 0
        gbc.gridy = 2; // แถว 2
        gbc.gridwidth = 2; // (*** สำคัญ: ให้กินพื้นที่ 2 คอลัมน์ ***)
        gbc.weightx = 1.0; // ให้น้ำหนักเต็มความกว้าง
        JPanel pnlChart3 = createSalesLineChart();
        add(pnlChart3, gbc);

        // (ไม่ต้องมี Panel ว่างแล้ว)

        // (*** แก้ไข: ตั้งพื้นหลังหลักเป็นสีเทาอ่อนๆ ***)
        getContentPane().setBackground(Color.LIGHT_GRAY);
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
    /** กราฟวงกลม (Pie Chart) "สัดส่วนยอดขายตามหมวดหมู่"
     * (*** ฉบับแก้ไข - เอา Label ออก ***)
    */
    private JPanel createCategoryPieChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        String sql = "SELECT c.name, COALESCE(SUM(sd.quantity * sd.price_per_unit), 0) AS total_value FROM sale_details sd JOIN products p ON sd.product_id = p.product_id JOIN categories c ON p.category_id = c.category_id GROUP BY c.name ORDER BY total_value DESC";
        try (Connection conn = DbConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) { dataset.setValue(rs.getString("name"), rs.getDouble("total_value")); }
        } catch (SQLException e) {
            e.printStackTrace(); return createChartPlaceholder("Error loading chart data");
        }

        JFreeChart pieChart = ChartFactory.createPieChart("สัดส่วนยอดขายตามหมวดหมู่", dataset, true, true, false);
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 12);
        Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14);
        pieChart.getTitle().setFont(thaiBoldFont);
        pieChart.getLegend().setItemFont(thaiFont);

        PiePlot plot = (PiePlot) pieChart.getPlot();
        // plot.setLabelFont(thaiFont); // <-- ไม่จำเป็นแล้ว
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);

        // (*** แก้ไข: ปิด Label อย่างชัดเจน ***)
        plot.setLabelGenerator(null); // <-- สั่งไม่ให้มี Generator
        plot.setSimpleLabels(false); // <-- ปิด Simple Labels ด้วย
        // ---

        final ChartPanel chartPanel = new ChartPanel(pieChart);
        chartPanel.setPreferredSize(new Dimension(380, 280));
        chartPanel.setBackground(BG_COLOR_3);

        // --- (Listener ใหม่: คลิกที่ Panel) ---
        chartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(chartPanel);
                CategorySelectorDialog selectorDialog = new CategorySelectorDialog(owner, "sales");
                selectorDialog.setVisible(true);
            }
            @Override
            public void chartMouseMoved(ChartMouseEvent event) { /* ไม่ใช้ */ }
        });
        // --- (สิ้นสุด Listener ใหม่) ---

        return chartPanel;

    } // <--- ปิดเมธอด createCategoryPieChart


    /** กราฟวงกลม (Pie Chart) "สัดส่วนกำไรตามหมวดหมู่"
     * (*** ฉบับแก้ไข - เอา Label ออก ***)
    */
    private JPanel createProfitPieChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        String sql = "SELECT c.name, COALESCE(SUM(sd.quantity * (p.sale_price - p.cost_price)), 0) AS total_profit FROM sale_details sd JOIN products p ON sd.product_id = p.product_id JOIN categories c ON p.category_id = c.category_id GROUP BY c.name HAVING total_profit > 0 ORDER BY total_profit DESC";
        try (Connection conn = DbConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) { dataset.setValue(rs.getString("name"), rs.getDouble("total_profit")); }
        } catch (SQLException e) {
            e.printStackTrace(); return createChartPlaceholder("Error loading profit chart data");
        }

        JFreeChart pieChart = ChartFactory.createPieChart("สัดส่วนกำไรตามหมวดหมู่", dataset, true, true, false);
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 12);
        Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14);
        pieChart.getTitle().setFont(thaiBoldFont);
        pieChart.getLegend().setItemFont(thaiFont);

        PiePlot plot = (PiePlot) pieChart.getPlot();
        // plot.setLabelFont(thaiFont); // <-- ไม่จำเป็นแล้ว
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);

        // (*** แก้ไข: ปิด Label อย่างชัดเจน ***)
        plot.setLabelGenerator(null); // <-- สั่งไม่ให้มี Generator
        plot.setSimpleLabels(false); // <-- ปิด Simple Labels ด้วย
        // ---

        final ChartPanel chartPanel = new ChartPanel(pieChart);
        chartPanel.setPreferredSize(new Dimension(380, 280));
        chartPanel.setBackground(BG_COLOR_4);

        // --- (Listener ใหม่: คลิกที่ Panel) ---
        chartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(chartPanel);
                CategorySelectorDialog selectorDialog = new CategorySelectorDialog(owner, "profit");
                selectorDialog.setVisible(true);
            }
            @Override
            public void chartMouseMoved(ChartMouseEvent event) { /* ไม่ใช้ */ }
        });
        // --- (สิ้นสุด Listener ใหม่) ---

        return chartPanel;

    } // <--- ปิดเมธอด createProfitPieChart
    /** กราฟเส้น (Line Chart) "ยอดขาย 30 วันย้อนหลัง" */
    /** กราฟเส้น (Line Chart) "ยอดขาย 30 วันย้อนหลัง"
     * (*** ฉบับแก้ไข - เพิ่มโค้ดหมุนแกน X ***)
    */
    private JPanel createSalesLineChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String sql = "SELECT DATE(sale_date) AS sale_day, SUM(total_amount) AS daily_total FROM sales WHERE sale_date >= CURDATE() - INTERVAL 30 DAY GROUP BY sale_day ORDER BY sale_day ASC";
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");
        try (Connection conn = DbConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
             while (rs.next()) { dataset.setValue(rs.getDouble("daily_total"), "ยอดขาย", dateFormat.format(rs.getDate("sale_day"))); }
        } catch (SQLException e) {
             e.printStackTrace(); return createChartPlaceholder("Error loading chart data");
        }

        JFreeChart lineChart = ChartFactory.createLineChart("แนวโน้มยอดขาย 30 วันย้อนหลัง", "วันที่", "ยอดขาย (บาท)", dataset, PlotOrientation.VERTICAL, false, true, false);
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 12);
        Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14);
        lineChart.getTitle().setFont(thaiBoldFont);

        CategoryPlot plot = lineChart.getCategoryPlot(); // <-- ดึง Plot

        // --- (*** เพิ่ม 2 บรรทัดนี้ตรงนี้ ***) ---
        org.jfree.chart.axis.CategoryAxis domainAxis = plot.getDomainAxis(); // ดึงแกน X
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90); // สั่งให้ Label แนวตั้ง
        // --- (*** สิ้นสุดส่วนที่เพิ่ม ***) ---

        // (โค้ดปรับแต่งที่เหลือ)
        domainAxis.setLabelFont(thaiBoldFont);
        domainAxis.setTickLabelFont(thaiFont); // Font ของวันที่ (จะแสดงแนวตั้ง)
        plot.getRangeAxis().setLabelFont(thaiBoldFont);
        plot.getRangeAxis().setTickLabelFont(thaiFont);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesStroke(0, new java.awt.BasicStroke(2.0f));
        renderer.setSeriesPaint(0, Color.BLUE);

        final ChartPanel chartPanel = new ChartPanel(lineChart);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        chartPanel.setPreferredSize(new Dimension(screenSize.width - 100, 280));
        chartPanel.setBackground(BG_COLOR_5);
        return chartPanel;
    }
}