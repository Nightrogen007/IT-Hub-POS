package ithubsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder; 
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.FlowLayout; 
import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener; 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import java.text.DecimalFormat; 
import java.util.Calendar; 

// (*** ลบ Import ของ JFreeChart ทั้งหมด ***)

public class ReportPanel extends JPanel {

    private DecimalFormat df = new DecimalFormat("#,##0.00");
    private DecimalFormat dfInt = new DecimalFormat("#,##0");

    // Components สรุปยอดขาย
    private JComboBox<Integer> cmbDay, cmbMonth, cmbYear;
    private JComboBox<Integer> cmbMonth_M, cmbYear_M;
    private JComboBox<Integer> cmbYear_Y;
    private JButton btnSearchDay, btnSearchMonth, btnSearchYear;
    private JLabel lblDailySales, lblDailyBills, lblDailyItems, lblDailyProfit;
    private JLabel lblMonthlySales, lblMonthlyBills, lblMonthlyItems, lblMonthlyProfit;
    private JLabel lblYearlySales, lblYearlyBills, lblYearlyItems, lblYearlyProfit;
    
    // Components ตาราง
    private JTable recentSalesTable; 
    private DefaultTableModel lowStockModel, bestSellerModel, recentSalesModel;
    
    // (ใหม่) ปุ่มเปิดกราฟ
    private JButton btnShowCharts;

    public ReportPanel() {
        setLayout(null); 
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // (*** แก้ไข ***) ขยายความกว้างรวมเป็น 1100 และสูง 560
        setPreferredSize(new Dimension(1100, 560)); 

        // --- 1. สรุปยอดขาย (*** แก้ไข *** ขยายความกว้าง) ---
        JPanel pnlSalesSummary = createSalesSummaryPanel();
        pnlSalesSummary.setBounds(10, 10, 800, 150); // <--- กว้าง 800
        add(pnlSalesSummary);

        // --- 2. สินค้าใกล้หมด (ขนาดเดิม) ---
        JPanel pnlLowStock = createLowStockPanel();
        pnlLowStock.setBounds(10, 170, 325, 180); 
        add(pnlLowStock);
        
        // --- 3. สินค้าขายดี (*** แก้ไข *** ย้าย) ---
        JPanel pnlBestSeller = createBestSellerPanel();
        pnlBestSeller.setBounds(345, 170, 465, 180); // <--- ย้ายและขยาย
        add(pnlBestSeller);

        // --- 4. รายการขายล่าสุด (*** แก้ไข *** ขยาย) ---
        JPanel pnlRecentSales = createRecentSalesPanel();
        pnlRecentSales.setBounds(10, 360, 1070, 180); // <--- กว้าง 1070
        add(pnlRecentSales);
        
        // --- 5. (*** ใหม่ ***) ปุ่มเปิดกราฟ ---
        JPanel pnlChartButton = new JPanel(new BorderLayout());
        btnShowCharts = new JButton("แสดงกราฟรายงาน");
        btnShowCharts.setFont(new Font("Tahoma", Font.BOLD, 16));
        btnShowCharts.setBackground(new Color(79, 129, 189));
        btnShowCharts.setForeground(Color.WHITE);
        pnlChartButton.add(btnShowCharts, BorderLayout.CENTER);
        pnlChartButton.setBounds(820, 10, 260, 340); // (วางแทนที่กราฟ)
        add(pnlChartButton);
        
        // (*** ใหม่ ***) เพิ่ม Action ให้ปุ่ม
        btnShowCharts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // หาหน้าต่างแม่
                JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(ReportPanel.this);
                // เปิด Dialog กราฟ
                ChartReportDialog dialog = new ChartReportDialog(owner);
                dialog.setVisible(true);
            }
        });

        // --- 6. โหลดข้อมูล ---
        populateDatePickers(); 
        loadInitialReportData(); 
    }

    /**
     * (*** แก้ไข ***) "บีบอัด" Layout และแก้ Font
     */
    private JPanel createSalesSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 2)); 
        Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14);
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 12);
        Font thaiDataFont = new Font("Tahoma", Font.BOLD, 12); 

        panel.setBorder(BorderFactory.createTitledBorder(null, "สรุปยอดขาย (เลือกช่วงเวลา)", 
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, thaiBoldFont));
        
        // (โค้ดส่วนนี้เหมือนเดิม 100% ครับ)
        JPanel pnlDaily = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2)); 
        JLabel lblDailyTitle = new JLabel("รายงาน : วัน"); 
        lblDailyTitle.setFont(thaiBoldFont); 
        pnlDaily.add(lblDailyTitle);
        cmbDay = new JComboBox<>(); cmbDay.setFont(thaiFont);
        cmbMonth = new JComboBox<>(); cmbMonth.setFont(thaiFont);
        cmbYear = new JComboBox<>(); cmbYear.setFont(thaiFont);
        btnSearchDay = new JButton("ค้นหา"); btnSearchDay.setFont(thaiFont);
        pnlDaily.add(cmbDay); pnlDaily.add(cmbMonth); pnlDaily.add(cmbYear); pnlDaily.add(btnSearchDay);
        pnlDaily.add(new JSeparator(SwingConstants.VERTICAL));
        JLabel lblDSales_t = new JLabel("ยอดขาย:"); lblDSales_t.setFont(thaiFont); pnlDaily.add(lblDSales_t); 
        lblDailySales = new JLabel("0.00"); lblDailySales.setFont(thaiDataFont); pnlDaily.add(lblDailySales); 
        JLabel lblDBills_t = new JLabel("บิล:"); lblDBills_t.setFont(thaiFont); pnlDaily.add(lblDBills_t); 
        lblDailyBills = new JLabel("0"); lblDailyBills.setFont(thaiDataFont); pnlDaily.add(lblDailyBills); 
        JLabel lblDItems_t = new JLabel("รายการ:"); lblDItems_t.setFont(thaiFont); pnlDaily.add(lblDItems_t); 
        lblDailyItems = new JLabel("0"); lblDailyItems.setFont(thaiDataFont); pnlDaily.add(lblDailyItems); 
        JLabel lblDProfit_t = new JLabel("กำไร:"); lblDProfit_t.setFont(thaiFont); pnlDaily.add(lblDProfit_t); 
        lblDailyProfit = new JLabel("0.00"); lblDailyProfit.setFont(thaiDataFont); lblDailyProfit.setForeground(Color.GREEN.darker()); pnlDaily.add(lblDailyProfit); 
        panel.add(pnlDaily);

        JPanel pnlMonthly = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JLabel lblMonthlyTitle = new JLabel("รายงาน : เดือน"); 
        lblMonthlyTitle.setFont(thaiBoldFont); 
        cmbMonth_M = new JComboBox<>(); cmbMonth_M.setFont(thaiFont);
        cmbYear_M = new JComboBox<>(); cmbYear_M.setFont(thaiFont);
        btnSearchMonth = new JButton("ค้นหา"); btnSearchMonth.setFont(thaiFont);
        pnlMonthly.add(lblMonthlyTitle); pnlMonthly.add(cmbMonth_M); pnlMonthly.add(cmbYear_M); pnlMonthly.add(btnSearchMonth);
        pnlMonthly.add(new JSeparator(SwingConstants.VERTICAL));
        JLabel lblMSales_t = new JLabel("ยอดขาย:"); lblMSales_t.setFont(thaiFont); pnlMonthly.add(lblMSales_t); 
        lblMonthlySales = new JLabel("0.00"); lblMonthlySales.setFont(thaiDataFont); pnlMonthly.add(lblMonthlySales); 
        JLabel lblMBills_t = new JLabel("บิล:"); lblMBills_t.setFont(thaiFont); pnlMonthly.add(lblMBills_t); 
        lblMonthlyBills = new JLabel("0"); lblMonthlyBills.setFont(thaiDataFont); pnlMonthly.add(lblMonthlyBills); 
        JLabel lblMItems_t = new JLabel("รายการ:"); lblMItems_t.setFont(thaiFont); pnlMonthly.add(lblMItems_t); 
        lblMonthlyItems = new JLabel("0"); lblMonthlyItems.setFont(thaiDataFont); pnlMonthly.add(lblMonthlyItems); 
        JLabel lblMProfit_t = new JLabel("กำไร:"); lblMProfit_t.setFont(thaiFont); pnlMonthly.add(lblMProfit_t); 
        lblMonthlyProfit = new JLabel("0.00"); lblMonthlyProfit.setFont(thaiDataFont); lblMonthlyProfit.setForeground(Color.GREEN.darker()); pnlMonthly.add(lblMonthlyProfit); 
        panel.add(pnlMonthly);

        JPanel pnlYearly = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JLabel lblYearlyTitle = new JLabel("รายงาน : ปี"); 
        lblYearlyTitle.setFont(thaiBoldFont); 
        cmbYear_Y = new JComboBox<>(); cmbYear_Y.setFont(thaiFont);
        btnSearchYear = new JButton("ค้นหา"); btnSearchYear.setFont(thaiFont);
        pnlYearly.add(lblYearlyTitle); pnlYearly.add(cmbYear_Y); pnlYearly.add(btnSearchYear);
        pnlYearly.add(new JSeparator(SwingConstants.VERTICAL));
        JLabel lblYSales_t = new JLabel("ยอดขาย:"); lblYSales_t.setFont(thaiFont); pnlYearly.add(lblYSales_t); 
        lblYearlySales = new JLabel("0.00"); lblYearlySales.setFont(thaiDataFont); pnlYearly.add(lblYearlySales); 
        JLabel lblYBills_t = new JLabel("บิล:"); lblYBills_t.setFont(thaiFont); pnlYearly.add(lblYBills_t); 
        lblYearlyBills = new JLabel("0"); lblYearlyBills.setFont(thaiDataFont); pnlYearly.add(lblYearlyBills); 
        JLabel lblYItems_t = new JLabel("รายการ:"); lblYItems_t.setFont(thaiFont); pnlYearly.add(lblYItems_t); 
        lblYearlyItems = new JLabel("0"); lblYearlyItems.setFont(thaiDataFont); pnlYearly.add(lblYearlyItems); 
        JLabel lblYProfit_t = new JLabel("กำไร:"); lblYProfit_t.setFont(thaiFont); pnlYearly.add(lblYProfit_t); 
        lblYearlyProfit = new JLabel("0.00"); lblYearlyProfit.setFont(thaiDataFont); lblYearlyProfit.setForeground(Color.GREEN.darker()); pnlYearly.add(lblYearlyProfit); 
        panel.add(pnlYearly);
        
        btnSearchDay.addActionListener(e -> loadSummaryData("day"));
        btnSearchMonth.addActionListener(e -> loadSummaryData("month"));
        btnSearchYear.addActionListener(e -> loadSummaryData("year"));

        return panel;
    }
    
    //
    // --- (โค้ดส่วนที่เหลือของ ReportPanel.java เหมือนเดิมทุกประการ) ---
    // (*** ยกเว้น 2 เมธอดที่ถูกลบทิ้งไป ***)
    //
    
    /** (ใหม่) เติมข้อมูลให้ ComboBox วัน/เดือน/ปี */
    private void populateDatePickers() {
        Calendar cal = Calendar.getInstance(); 
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        int currentMonth = cal.get(Calendar.MONTH) + 1; 
        int currentYear = cal.get(Calendar.YEAR);
        
        for (int i = 1; i <= 31; i++) cmbDay.addItem(i);
        for (int i = 1; i <= 12; i++) {
            cmbMonth.addItem(i);
            cmbMonth_M.addItem(i);
        }
        for (int i = currentYear - 5; i <= currentYear; i++) {
            cmbYear.addItem(i);
            cmbYear_M.addItem(i);
            cmbYear_Y.addItem(i);
        }
        
        cmbDay.setSelectedItem(currentDay);
        cmbMonth.setSelectedItem(currentMonth);
        cmbYear.setSelectedItem(currentYear);
        cmbMonth_M.setSelectedItem(currentMonth);
        cmbYear_M.setSelectedItem(currentYear);
        cmbYear_Y.setSelectedItem(currentYear);
    }
    
    /** โหลดข้อมูลสรุปตามเงื่อนไข */
    private void loadSummaryData(String interval) {
        String sql = "SELECT " +
                     "  COALESCE(SUM(s.total_amount), 0) AS totalSales, " +
                     "  COALESCE(COUNT(DISTINCT s.sale_id), 0) AS billCount, " +
                     "  COALESCE(SUM(sd.quantity), 0) AS totalItems, " +
                     "  COALESCE(SUM(sd.quantity * (p.sale_price - p.cost_price)), 0) AS totalProfit " +
                     "FROM sales s " +
                     "LEFT JOIN sale_details sd ON s.sale_id = sd.sale_id " +
                     "LEFT JOIN products p ON sd.product_id = p.product_id ";

        String whereClause = "";
        
        try (Connection conn = DbConnection.getConnection()) {
            PreparedStatement pstmt;
            if (interval.equals("day")) {
                int day = (Integer) cmbDay.getSelectedItem();
                int month = (Integer) cmbMonth.getSelectedItem();
                int year = (Integer) cmbYear.getSelectedItem();
                whereClause = "WHERE DAY(s.sale_date) = ? AND MONTH(s.sale_date) = ? AND YEAR(s.sale_date) = ?";
                pstmt = conn.prepareStatement(sql + whereClause);
                pstmt.setInt(1, day); pstmt.setInt(2, month); pstmt.setInt(3, year);
            } else if (interval.equals("month")) {
                int month = (Integer) cmbMonth_M.getSelectedItem();
                int year = (Integer) cmbYear_M.getSelectedItem();
                whereClause = "WHERE MONTH(s.sale_date) = ? AND YEAR(s.sale_date) = ?";
                pstmt = conn.prepareStatement(sql + whereClause);
                pstmt.setInt(1, month); pstmt.setInt(2, year);
            } else { // "year"
                int year = (Integer) cmbYear_Y.getSelectedItem();
                whereClause = "WHERE YEAR(s.sale_date) = ?";
                pstmt = conn.prepareStatement(sql + whereClause);
                pstmt.setInt(1, year);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double totalSales = rs.getDouble("totalSales");
                    int billCount = rs.getInt("billCount");
                    int totalItems = rs.getInt("totalItems");
                    double totalProfit = rs.getDouble("totalProfit");

                    if (interval.equals("day")) {
                        lblDailySales.setText(df.format(totalSales));
                        lblDailyBills.setText(dfInt.format(billCount));
                        lblDailyItems.setText(dfInt.format(totalItems));
                        lblDailyProfit.setText(df.format(totalProfit));
                    } else if (interval.equals("month")) {
                        lblMonthlySales.setText(df.format(totalSales));
                        lblMonthlyBills.setText(dfInt.format(billCount));
                        lblMonthlyItems.setText(dfInt.format(totalItems));
                        lblMonthlyProfit.setText(df.format(totalProfit));
                    } else {
                        lblYearlySales.setText(df.format(totalSales));
                        lblYearlyBills.setText(dfInt.format(billCount));
                        lblYearlyItems.setText(dfInt.format(totalItems));
                        lblYearlyProfit.setText(df.format(totalProfit));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /** โหลดข้อมูลทั้งหมด (ตอนเปิดหน้า) */
    public void loadInitialReportData() {
        loadSummaryData("day");
        loadSummaryData("month");
        loadSummaryData("year");
        try { loadLowStockData(); } catch (SQLException e) { e.printStackTrace(); }
        try { loadBestSellerData(); } catch (SQLException e) { e.printStackTrace(); }
        try { loadRecentSalesData(); } catch (SQLException e) { e.printStackTrace(); }
    }

    /** (Helper) สร้างตารางย่อยๆ */
    private JScrollPane createTable(String[] headers, DefaultTableModel model) {
        model.setColumnIdentifiers(headers);
        JTable table = new JTable(model) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table.setFont(new Font("Tahoma", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        return new JScrollPane(table);
    }
    
    /** (Helper) สร้าง Panel สินค้าใกล้หมด */
    private JPanel createLowStockPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14); 
        panel.setBorder(BorderFactory.createTitledBorder(null, "สินค้าใกล้หมด (<= 20 ชิ้น)", 
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, thaiBoldFont));
        lowStockModel = new DefaultTableModel();
        JScrollPane sp = createTable(new String[]{"ชื่อสินค้า", "คงเหลือ", "ราคา"}, lowStockModel);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }
    
    /** (Helper) สร้าง Panel สินค้าขายดี */
    private JPanel createBestSellerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14); 
        panel.setBorder(BorderFactory.createTitledBorder(null, "สินค้าขายดี (Top 5)", 
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, thaiBoldFont));
        bestSellerModel = new DefaultTableModel();
        JScrollPane sp = createTable(new String[]{"ชื่อสินค้า", "จำนวน", "ยอดขาย"}, bestSellerModel);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }
    
    /** (Helper) สร้าง Panel รายการขายล่าสุด */
    private JPanel createRecentSalesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14); 
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 12); 
        panel.setBorder(BorderFactory.createTitledBorder(null, "รายการขายล่าสุด (10 รายการ)", 
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, thaiBoldFont));
        recentSalesModel = new DefaultTableModel(); 
        String[] headers = {"ID บิล", "วันที่", "ยอดรวม", "รายละเอียด"};
        recentSalesModel.setColumnIdentifiers(headers);
        recentSalesTable = new JTable(recentSalesModel) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        recentSalesTable.setFont(new Font("Tahoma", Font.PLAIN, 12));
        recentSalesTable.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        recentSalesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
        JScrollPane sp = new JScrollPane(recentSalesTable);
        JButton btnViewDetail = new JButton("ดูรายละเอียดบิลที่เลือก");
        btnViewDetail.setFont(thaiFont); 
        btnViewDetail.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSaleDetail();
            }
        });
        JPanel innerPanel = new JPanel(new BorderLayout(0, 5));
        innerPanel.setBorder(new EmptyBorder(0, 5, 5, 5));
        innerPanel.add(btnViewDetail, BorderLayout.NORTH);
        innerPanel.add(sp, BorderLayout.CENTER); 
        panel.add(innerPanel, BorderLayout.CENTER); 
        return panel;
    }

    /** (SQL Helper) ดึงข้อมูลสินค้าใกล้หมด */
    private void loadLowStockData() throws SQLException {
        lowStockModel.setRowCount(0);
        String sql = "SELECT name, stock, sale_price FROM products WHERE stock <= 20 ORDER BY stock ASC LIMIT 5";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("name"));
                row.add(rs.getInt("stock"));
                row.add(df.format(rs.getDouble("sale_price")));
                lowStockModel.addRow(row);
            }
        }
    }

    /** (SQL Helper) ดึงข้อมูลสินค้าขายดี */
    private void loadBestSellerData() throws SQLException {
        bestSellerModel.setRowCount(0);
        String sql = "SELECT p.name, " +
                     "  COALESCE(SUM(sd.quantity), 0) AS total_sold, " +
                     "  COALESCE(SUM(sd.quantity * sd.price_per_unit), 0) AS total_value " +
                     "FROM sale_details sd " +
                     "JOIN products p ON sd.product_id = p.product_id " +
                     "GROUP BY p.name " +
                     "ORDER BY total_sold DESC LIMIT 5";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("name"));
                row.add(dfInt.format(rs.getInt("total_sold")));
                row.add(df.format(rs.getDouble("total_value")));
                bestSellerModel.addRow(row);
            }
        }
    }

    /** (SQL Helper) ดึงข้อมูลรายการขายล่าสุด */
    private void loadRecentSalesData() throws SQLException {
        recentSalesModel.setRowCount(0);
        String sql = "SELECT s.sale_id, DATE_FORMAT(s.sale_date, '%d/%m/%Y %H:%i') AS sale_time, s.total_amount " +
                     "FROM sales s " +
                     "ORDER BY s.sale_date DESC LIMIT 10";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("sale_id"));
                row.add(rs.getString("sale_time"));
                row.add(df.format(rs.getDouble("total_amount")));
                row.add("คลิกปุ่มด้านบน...");
                recentSalesModel.addRow(row);
            }
        }
    }
    
    /** เปิด Pop-up ดูรายละเอียดบิล */
    private void showSaleDetail() {
        int selectedRow = recentSalesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "กรุณาคลิกเลือกบิลที่ต้องการดูก่อน", "ไม่ได้เลือก", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int saleId = (Integer) recentSalesModel.getValueAt(selectedRow, 0);
        String saleDate = (String) recentSalesModel.getValueAt(selectedRow, 1);
        double totalAmount = 0;
        try {
            totalAmount = df.parse(recentSalesModel.getValueAt(selectedRow, 2).toString()).doubleValue();
        } catch (Exception e) { e.printStackTrace(); }
        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
        SaleDetailDialog dialog = new SaleDetailDialog(owner, saleId, saleDate, totalAmount);
        dialog.setVisible(true);
    }
    
    //
    // --- (*** ลบ 3 เมธอดนี้ทิ้ง ***) ---
    // private JPanel createChartPlaceholder(...)
    // private JPanel createBestSellerChart(...)
    // private JPanel createCategoryPieChart(...)
    //
}