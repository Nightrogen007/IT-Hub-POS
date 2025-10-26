package ithubsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Vector;

public class CategoryDetailDialog extends JDialog {

    // Components
    private JLabel lblHeader;
    private JTable detailTable;
    private DefaultTableModel detailTableModel;
    private JLabel lblFooter;

    private String categoryName;
    private String dataType; // "sales" หรือ "profit"
    private DecimalFormat df = new DecimalFormat("#,##0.00");

    /**
     * Constructor
     * @param owner หน้าต่างแม่
     * @param categoryName ชื่อหมวดหมู่ที่คลิก
     * @param dataType ประเภทข้อมูล ("sales" หรือ "profit")
     */
    public CategoryDetailDialog(JFrame owner, String categoryName, String dataType) {
        // (ตั้ง Title ตามประเภทข้อมูล)
        super(owner, "รายละเอียด" + (dataType.equals("sales") ? "ยอดขาย" : "กำไร") + ": " + categoryName, true);
        this.categoryName = categoryName;
        this.dataType = dataType;

        setSize(700, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        Font thaiFont = new Font("Tahoma", Font.PLAIN, 14);
        Font boldFont = new Font("Tahoma", Font.BOLD, 16);

        // --- 1. Header (NORTH) ---
        lblHeader = new JLabel("รายละเอียด" + (dataType.equals("sales") ? "ยอดขาย" : "กำไร") + " หมวดหมู่: " + categoryName);
        lblHeader.setFont(boldFont);
        lblHeader.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(lblHeader, BorderLayout.NORTH);

        // --- 2. ตาราง (CENTER) ---
        // (กำหนด Header ตารางตาม dataType)
        String[] headers;
        if (dataType.equals("sales")) {
            headers = new String[]{"รหัสสินค้า", "ชื่อสินค้า", "จำนวนขาย", "ยอดขายรวม"};
        } else { // profit
            headers = new String[]{"รหัสสินค้า", "ชื่อสินค้า", "จำนวนขาย", "กำไรรวม"};
        }

        detailTableModel = new DefaultTableModel(headers, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        detailTable = new JTable(detailTableModel);
        detailTable.setFont(thaiFont);
        detailTable.setRowHeight(25);
        detailTable.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 14));

        JScrollPane sp = new JScrollPane(detailTable);
        sp.setBorder(new EmptyBorder(0, 10, 0, 10));
        add(sp, BorderLayout.CENTER);

        // --- 3. Footer (SOUTH) ---
        lblFooter = new JLabel("กำลังโหลดข้อมูล...");
        lblFooter.setFont(boldFont);
        lblFooter.setHorizontalAlignment(SwingConstants.RIGHT);
        lblFooter.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(lblFooter, BorderLayout.SOUTH);

        // --- 4. โหลดข้อมูล ---
        loadCategoryDetails();
    }

    /**
     * ดึงข้อมูลรายสินค้าในหมวดหมู่นี้
     */
    private void loadCategoryDetails() {
        detailTableModel.setRowCount(0);
        double totalValue = 0; // ยอดรวมสำหรับ Footer

        // (เลือก SQL ตาม dataType)
        String sql;
        if (dataType.equals("sales")) {
            sql = "SELECT p.product_id, p.name, " +
                  "  SUM(sd.quantity) AS total_qty, " +
                  "  SUM(sd.quantity * sd.price_per_unit) AS total_value " +
                  "FROM sale_details sd " +
                  "JOIN products p ON sd.product_id = p.product_id " +
                  "JOIN categories c ON p.category_id = c.category_id " +
                  "WHERE c.name = ? " +
                  "GROUP BY p.product_id, p.name " +
                  "ORDER BY total_value DESC";
        } else { // profit
            sql = "SELECT p.product_id, p.name, " +
                  "  SUM(sd.quantity) AS total_qty, " +
                  "  SUM(sd.quantity * (p.sale_price - p.cost_price)) AS total_profit " +
                  "FROM sale_details sd " +
                  "JOIN products p ON sd.product_id = p.product_id " +
                  "JOIN categories c ON p.category_id = c.category_id " +
                  "WHERE c.name = ? " +
                  "GROUP BY p.product_id, p.name " +
                  "HAVING total_profit > 0 " + // เอาเฉพาะที่มีกำไร
                  "ORDER BY total_profit DESC";
        }

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, this.categoryName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("product_id"));
                row.add(rs.getString("name"));
                row.add(rs.getInt("total_qty"));

                double value;
                if (dataType.equals("sales")) {
                    value = rs.getDouble("total_value");
                } else {
                    value = rs.getDouble("total_profit");
                }
                row.add(df.format(value)); // จัดรูปแบบตัวเลข
                detailTableModel.addRow(row);
                totalValue += value; // บวกยอดรวม
            }
            rs.close();

            // อัปเดต Footer
            lblFooter.setText("ยอดรวม" + (dataType.equals("sales") ? "ขาย" : "กำไร") + " ทั้งหมด: " + df.format(totalValue) + " บาท");

        } catch (SQLException e) {
            e.printStackTrace();
            lblFooter.setText("Error: ไม่สามารถโหลดข้อมูลได้");
            JOptionPane.showMessageDialog(this, "ไม่สามารถโหลดรายละเอียดได้", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}