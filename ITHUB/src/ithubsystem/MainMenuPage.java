package ithubsystem; // <--- Package ของคุณ

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.Font;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement; // <--- เพิ่ม import
import java.sql.ResultSet;        // <--- เพิ่ม import
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;    // <--- เพิ่ม import
import java.util.Date;             // <--- เพิ่ม import
import java.text.SimpleDateFormat; // <--- เพิ่ม import
import java.util.Vector;           // <--- เพิ่ม import
import java.awt.Component;         // <--- เพิ่ม import
import java.awt.Container;        // <--- เพิ่ม import
import javax.swing.JViewport;      // <--- เพิ่ม import

public class MainMenuPage extends JFrame {

    private String username;
    private String role;
    private DecimalFormat df = new DecimalFormat("#,##0.00"); // <--- เพิ่ม df

    // Panels
    private JPanel pnlHeader;
    private JPanel pnlMenu;
    private JPanel pnlMainContent;

    // Header Components
    private JLabel lblUser, lblTitle;

    // Menu Components
    private JButton btnDashboard, btnManageProducts, btnSales, btnReports, btnLogout;

    public MainMenuPage(String username, String role) {
        this.username = username;
        this.role = role;

        setTitle("IT HUB - Main Menu");
        // (ปรับขนาด Frame หลักให้กว้างขึ้นเล็กน้อย เผื่อ Dashboard)
        setSize(1150, 700); // <--- อาจจะต้องปรับตามขนาดจอของคุณ
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        createHeaderPanel();
        add(pnlHeader, BorderLayout.NORTH);

        createMenuPanel();
        add(pnlMenu, BorderLayout.WEST);

        pnlMainContent = new JPanel(new BorderLayout());
        add(pnlMainContent, BorderLayout.CENTER);

        showDashboardPanel();

        addMenuActions();
    }

    /** สร้าง Panel ส่วนหัว (NORTH) */
    private void createHeaderPanel() {
        pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(Color.WHITE);
        pnlHeader.setBorder(new EmptyBorder(10, 20, 10, 20));

        lblUser = new JLabel("USER: " + username.toUpperCase() + " (Role: " + role + ")");
        lblUser.setFont(new Font("Tahoma", Font.PLAIN, 12));
        pnlHeader.add(lblUser, BorderLayout.WEST);

        lblTitle = new JLabel("IT HUB");
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 24));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        pnlHeader.add(lblTitle, BorderLayout.CENTER);
    }

    /** สร้าง Panel เมนู (WEST) */
    private void createMenuPanel() {
        pnlMenu = new JPanel();
        pnlMenu.setLayout(new BoxLayout(pnlMenu, BoxLayout.Y_AXIS));
        pnlMenu.setPreferredSize(new Dimension(150, 0));
        pnlMenu.setBackground(new Color(240, 240, 240));
        pnlMenu.setBorder(new EmptyBorder(10, 10, 10, 10));

        btnDashboard = new JButton("หน้าหลัก");
        btnManageProducts = new JButton("จัดการสินค้า");
        btnSales = new JButton("จัดการขาย");
        btnReports = new JButton("รายงาน");
        btnLogout = new JButton("LOGOUT");

        Dimension btnSize = new Dimension(130, 40);
        Font btnFont = new Font("Tahoma", Font.BOLD, 14);

        btnDashboard.setMaximumSize(btnSize); btnDashboard.setFont(btnFont);
        btnManageProducts.setMaximumSize(btnSize); btnManageProducts.setFont(btnFont);
        btnSales.setMaximumSize(btnSize); btnSales.setFont(btnFont);
        btnReports.setMaximumSize(btnSize); btnReports.setFont(btnFont);
        btnLogout.setMaximumSize(btnSize); btnLogout.setFont(btnFont);
        btnLogout.setBackground(new Color(220, 50, 50)); btnLogout.setForeground(Color.WHITE);

        pnlMenu.add(btnDashboard);
        pnlMenu.add(Box.createRigidArea(new Dimension(0, 10)));
        pnlMenu.add(btnManageProducts); // สินค้า
        pnlMenu.add(Box.createRigidArea(new Dimension(0, 10)));
        pnlMenu.add(btnSales); // ขาย
        pnlMenu.add(Box.createRigidArea(new Dimension(0, 10)));
        pnlMenu.add(btnReports); // รายงาน
        pnlMenu.add(Box.createVerticalGlue());
        pnlMenu.add(btnLogout);

        if (role.equals("staff")) {
            btnReports.setEnabled(false);
            btnReports.setToolTipText("สำหรับ Admin เท่านั้น");
        }
    }

    /** สลับ Panel เนื้อหาหลัก */
    private void switchPanel(JPanel panel) {
        pnlMainContent.removeAll();
        pnlMainContent.add(panel, BorderLayout.CENTER);
        pnlMainContent.revalidate();
        pnlMainContent.repaint();
    }

    /** เพิ่ม Action Listeners ให้ปุ่มเมนู */
    private void addMenuActions() {
        btnLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int choice = JOptionPane.showConfirmDialog(MainMenuPage.this, "ยืนยันการออกจากระบบ", "ยืนยัน", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    dispose();
                    new LoginPage().setVisible(true);
                }
            }
        });

        btnDashboard.addActionListener(e -> showDashboardPanel());
        btnManageProducts.addActionListener(e -> showProductManagementPanel());
        btnSales.addActionListener(e -> showSalesPanel());
        btnReports.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (role.equals("admin")) {
                    MainMenuPage.this.showReportsPanel(); // เรียกใช้เมธอดของ MainMenuPage
                }
            }
        });
    }

    // --- เมธอดสำหรับสลับหน้า ---

    /** แสดงหน้า Dashboard */
    public void showDashboardPanel() {
        JPanel dashboardPanel = createDashboardPanel();
        switchPanel(dashboardPanel);
        // (*** สำคัญ: โหลดข้อมูลหลังจาก Panel ถูกแสดงผลแล้ว ***)
        loadDashboardData(dashboardPanel);
    }

    /** แสดงหน้าจัดการสินค้า */
    private void showProductManagementPanel() {
        ProductManagementPanel prodPanel = new ProductManagementPanel();
        switchPanel(prodPanel);
    }

    /** แสดงหน้าจัดการขาย */
    private void showSalesPanel() {
        SalePanel salePanel = new SalePanel(username);
        switchPanel(salePanel);
    }

    /** แสดงหน้ารายงาน */
    private void showReportsPanel() { // ชื่อเมธอดคือ Reports (มี s)
        ReportPanel reportPanel = new ReportPanel();
        switchPanel(reportPanel);
    }

    // --- โค้ดส่วน Dashboard (ทั้งหมด) ---

    /**
     * (*** ฉบับแก้ไข - จำกัดความสูงตาราง ***)
     * สร้าง Panel เนื้อหาหลัก (CENTER) - Dashboard พร้อม KPIs, สินค้าใกล้หมด, สินค้าขายดี
     */
    private JPanel createDashboardPanel() {
        // Panel หลักใช้ BorderLayout
        JPanel mainDashboardPanel = new JPanel(new BorderLayout(10, 10));
        mainDashboardPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainDashboardPanel.setBackground(Color.WHITE);

        // --- 1. Panel KPIs วันนี้ (NORTH) ---
        JPanel pnlKpis = createKpiPanel(); // (จะแก้ไขเพิ่มวันที่)
        mainDashboardPanel.add(pnlKpis, BorderLayout.NORTH);

        // --- 2. Panel สำหรับตาราง (CENTER) ---
        // (*** กลับมาใช้ GridLayout(1, 2) ***)
        JPanel pnlTables = new JPanel(new GridLayout(1, 2, 10, 10));
        pnlTables.setOpaque(false);

        // 2.1 ตารางสินค้าใกล้หมด (ซ้าย)
        JPanel pnlLowStock = createLowStockTablePanel();
        pnlTables.add(pnlLowStock);

        // 2.2 ตารางสินค้าขายดีวันนี้ (ขวา) (*** นำกลับมา ***)
        JPanel pnlTopSelling = createTopSellingTablePanel(); // <-- เรียกใช้เมธอดนี้
        pnlTables.add(pnlTopSelling);

        mainDashboardPanel.add(pnlTables, BorderLayout.CENTER);

        // --- 3. โหลดข้อมูล ---
        // (ย้ายไปเรียกใน showDashboardPanel)

        return mainDashboardPanel;
    }

     /** (Helper) สร้าง Panel KPI (NORTH)
     * (*** ฉบับแก้ไข - เพิ่มวันที่ ***)
    */
    private JPanel createKpiPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14);
        Font labelFont = new Font("Tahoma", Font.PLAIN, 14);
        Font kpiFont = new Font("Tahoma", Font.BOLD, 16);

        // (ดึงและจัดรูปแบบวันที่)
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String currentDate = sdf.format(new Date());

        // (เพิ่มวันที่ใน Title)
        panel.setBorder(BorderFactory.createTitledBorder(null, "สรุปข้อมูลวันนี้ (" + currentDate + ")",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, thaiBoldFont));
        panel.setOpaque(false);

        // (โค้ดส่วน Label และ Text ของ KPI เหมือนเดิม)
        JLabel lblSalesTitle = new JLabel("ยอดขาย:"); lblSalesTitle.setFont(labelFont); panel.add(lblSalesTitle);
        JLabel lblTodaySales = new JLabel("0.00 บาท"); lblTodaySales.setFont(kpiFont); lblTodaySales.setForeground(Color.BLUE); lblTodaySales.setName("lblTodaySales"); panel.add(lblTodaySales);
        JLabel lblBillsTitle = new JLabel("จำนวนบิล:"); lblBillsTitle.setFont(labelFont); panel.add(lblBillsTitle);
        JLabel lblTodayBills = new JLabel("0"); lblTodayBills.setFont(kpiFont); lblTodayBills.setName("lblTodayBills"); panel.add(lblTodayBills);
        JLabel lblProfitTitle = new JLabel("กำไร (ประมาณ):"); lblProfitTitle.setFont(labelFont); panel.add(lblProfitTitle);
        JLabel lblTodayProfit = new JLabel("0.00 บาท"); lblTodayProfit.setFont(kpiFont); lblTodayProfit.setForeground(Color.GREEN.darker()); lblTodayProfit.setName("lblTodayProfit"); panel.add(lblTodayProfit);

        return panel;
    }

    /** (Helper) สร้าง Panel ตารางสินค้าใกล้หมด (CENTER-LEFT)
     * (*** ฉบับแก้ไข - จำกัดความสูง ScrollPane ***)
    */
    private JPanel createLowStockTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14);
        Font headerFont = new Font("Tahoma", Font.BOLD, 12);

        panel.setBorder(BorderFactory.createTitledBorder(null, "สินค้าใกล้หมด (<= 10 ชิ้น)",
             TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, thaiBoldFont));
        panel.setOpaque(false);

        DefaultTableModel lowStockModel = new DefaultTableModel(new String[]{"ชื่อสินค้า", "คงเหลือ"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable lowStockTable = new JTable(lowStockModel);
        lowStockTable.setFont(new Font("Tahoma", Font.PLAIN, 12));
        lowStockTable.getTableHeader().setFont(headerFont);
        lowStockTable.setName("lowStockTable");

        JScrollPane sp = new JScrollPane(lowStockTable);

        // (กำหนดความสูงที่ต้องการ)
        int rowHeight = lowStockTable.getRowHeight();
        if (rowHeight <= 0) rowHeight = 16; // ค่า Default ถ้ายังไม่ได้กำหนด
        int preferredHeight = (5 + 1) * rowHeight; // แสดง 6 แถว + Header
        sp.setPreferredSize(new Dimension(sp.getPreferredSize().width, preferredHeight));

        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    /** (Helper) สร้าง Panel ตารางสินค้าขายดีวันนี้ (CENTER-RIGHT)
     * (*** ฉบับแก้ไข - จำกัดความสูง ScrollPane ***)
    */
    private JPanel createTopSellingTablePanel() { // <-- เมธอดนี้ต้องมี
        JPanel panel = new JPanel(new BorderLayout());
        Font thaiBoldFont = new Font("Tahoma", Font.BOLD, 14);
        Font headerFont = new Font("Tahoma", Font.BOLD, 12);

        panel.setBorder(BorderFactory.createTitledBorder(null, "สินค้าขายดีวันนี้ (Top 5)",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, thaiBoldFont));
        panel.setOpaque(false);

        DefaultTableModel topSellingModel = new DefaultTableModel(new String[]{"ชื่อสินค้า", "จำนวน"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable topSellingTable = new JTable(topSellingModel);
        topSellingTable.setFont(new Font("Tahoma", Font.PLAIN, 12));
        topSellingTable.getTableHeader().setFont(headerFont);
        topSellingTable.setName("topSellingTable");

        JScrollPane sp = new JScrollPane(topSellingTable);

        // (กำหนดความสูงที่ต้องการ)
        int rowHeight = topSellingTable.getRowHeight();
         if (rowHeight <= 0) rowHeight = 16;
        int preferredHeight = (5 + 1) * rowHeight; // แสดง 6 แถว + Header
        sp.setPreferredSize(new Dimension(sp.getPreferredSize().width, preferredHeight));

        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    /** (ใหม่) เมธอดสำหรับโหลดข้อมูล Dashboard ทั้งหมด */
    private void loadDashboardData(JPanel dashboardPanel) {
        loadKpiData(dashboardPanel);
        loadLowStockTableData(dashboardPanel);
        loadTopSellingTableData(dashboardPanel);
    }

    /** (SQL Helper) โหลดข้อมูล KPI วันนี้ */
    private void loadKpiData(JPanel dashboardPanel) {
        String sql = "SELECT COALESCE(SUM(s.total_amount), 0) AS totalSales, COALESCE(COUNT(DISTINCT s.sale_id), 0) AS billCount, COALESCE(SUM(sd.quantity * (p.sale_price - p.cost_price)), 0) AS totalProfit FROM sales s LEFT JOIN sale_details sd ON s.sale_id = sd.sale_id LEFT JOIN products p ON sd.product_id = p.product_id WHERE DATE(s.sale_date) = CURDATE()";
        try (Connection conn = DbConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                JLabel lblTodaySales = findLabelByName(dashboardPanel, "lblTodaySales");
                JLabel lblTodayBills = findLabelByName(dashboardPanel, "lblTodayBills");
                JLabel lblTodayProfit = findLabelByName(dashboardPanel, "lblTodayProfit");
                if (lblTodaySales != null) lblTodaySales.setText(df.format(rs.getDouble("totalSales")) + " บาท");
                if (lblTodayBills != null) lblTodayBills.setText(String.valueOf(rs.getInt("billCount")));
                if (lblTodayProfit != null) lblTodayProfit.setText(df.format(rs.getDouble("totalProfit")) + " บาท");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** (SQL Helper) โหลดข้อมูลตารางสินค้าใกล้หมด */
    private void loadLowStockTableData(JPanel dashboardPanel) {
        JTable lowStockTable = findTableByName(dashboardPanel, "lowStockTable");
        if (lowStockTable == null) return;
        DefaultTableModel model = (DefaultTableModel) lowStockTable.getModel();
        model.setRowCount(0);
        String sql = "SELECT name, stock FROM products WHERE stock <= 10 ORDER BY stock ASC LIMIT 5";
        try (Connection conn = DbConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("name"));
                row.add(rs.getInt("stock"));
                model.addRow(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

     /** (SQL Helper) โหลดข้อมูลตารางสินค้าขายดีวันนี้ */
    private void loadTopSellingTableData(JPanel dashboardPanel) {
        JTable topSellingTable = findTableByName(dashboardPanel, "topSellingTable");
         if (topSellingTable == null) return;
        DefaultTableModel model = (DefaultTableModel) topSellingTable.getModel();
        model.setRowCount(0);
        String sql = "SELECT p.name, SUM(sd.quantity) AS total_sold FROM sale_details sd JOIN products p ON sd.product_id = p.product_id JOIN sales s ON sd.sale_id = s.sale_id WHERE DATE(s.sale_date) = CURDATE() GROUP BY p.name ORDER BY total_sold DESC LIMIT 5";
        try (Connection conn = DbConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("name"));
                row.add(rs.getInt("total_sold"));
                model.addRow(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- (เมธอด Helper สำหรับค้นหา Component จากชื่อ) ---
    private JLabel findLabelByName(Container container, String name) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel && name.equals(comp.getName())) { return (JLabel) comp; }
            else if (comp instanceof Container) { JLabel found = findLabelByName((Container) comp, name); if (found != null) return found; }
        } return null;
    }
    private JTable findTableByName(Container container, String name) {
         for (Component comp : container.getComponents()) {
            if (comp instanceof JScrollPane) { JViewport viewport = ((JScrollPane) comp).getViewport(); Component view = viewport.getView(); if (view instanceof JTable && name.equals(view.getName())) { return (JTable) view; } }
            else if (comp instanceof Container) { JTable found = findTableByName((Container) comp, name); if (found != null) return found; }
         } return null;
     }
} // ปิดคลาส MainMenuPage