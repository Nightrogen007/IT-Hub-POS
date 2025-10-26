package ithubsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent; // (ใหม่)
import java.awt.event.ItemListener; // (ใหม่)
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.HashMap; // (ใหม่)

public class CategorySelectorDialog extends JDialog {

    // Components
    private JComboBox<String> cmbCategory;
    private JTable detailTable;
    private DefaultTableModel detailTableModel;
    private JLabel lblFooter;

    private String dataType; // "sales" หรือ "profit"
    private HashMap<String, Integer> categoryMap; // (ชื่อ -> ID)
    private DecimalFormat df = new DecimalFormat("#,##0.00");

    /**
     * Constructor
     * @param owner หน้าต่างแม่
     * @param dataType ประเภทข้อมูล ("sales" หรือ "profit")
     */
    public CategorySelectorDialog(JFrame owner, String dataType) {
        super(owner, "เลือกหมวดหมู่เพื่อดูรายละเอียด" + (dataType.equals("sales") ? "ยอดขาย" : "กำไร"), true);
        this.dataType = dataType;

        setSize(700, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 10)); // เพิ่ม VGap
        getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10)); // เพิ่มขอบให้ Dialog

        Font thaiFont = new Font("Tahoma", Font.PLAIN, 14);
        Font boldFont = new Font("Tahoma", Font.BOLD, 14);

        // --- 1. Panel เลือกหมวดหมู่ (NORTH) ---
        JPanel pnlSelector = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblSelect = new JLabel("เลือกหมวดหมู่:");
        lblSelect.setFont(boldFont);
        cmbCategory = new JComboBox<>();
        cmbCategory.setFont(thaiFont);
        cmbCategory.setPreferredSize(new Dimension(250, 25)); // กำหนดขนาด ComboBox
        pnlSelector.add(lblSelect);
        pnlSelector.add(cmbCategory);
        add(pnlSelector, BorderLayout.NORTH);

        // --- 2. ตาราง (CENTER) ---
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
        detailTable.getTableHeader().setFont(boldFont);
        JScrollPane sp = new JScrollPane(detailTable);
        add(sp, BorderLayout.CENTER);

        // --- 3. Footer (SOUTH) ---
        lblFooter = new JLabel(" "); // เริ่มต้นว่างๆ
        lblFooter.setFont(boldFont);
        lblFooter.setHorizontalAlignment(SwingConstants.RIGHT);
        add(lblFooter, BorderLayout.SOUTH);

        // --- 4. โหลดหมวดหมู่ & เพิ่ม Listener ---
        loadCategories();

        // (สำคัญ) เมื่อมีการเลือก Item ใน ComboBox
        cmbCategory.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                // ตรวจสอบว่าเป็นการ "เลือก" (ไม่ใช่เอาออก หรือโหลดครั้งแรก)
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedCategory = (String) cmbCategory.getSelectedItem();
                    // ถ้าไม่ใช่ "-- เลือก --" ให้โหลดข้อมูล
                    if (!selectedCategory.equals("-- เลือกหมวดหมู่ --")) {
                        loadCategoryDetails(selectedCategory);
                    } else {
                        // ถ้าเลือก "-- เลือก --" ให้ล้างตาราง
                        detailTableModel.setRowCount(0);
                        lblFooter.setText(" ");
                    }
                }
            }
        });
    }

    /** ดึงหมวดหมู่ทั้งหมดมาใส่ ComboBox */
    private void loadCategories() {
        categoryMap = new HashMap<>();
        cmbCategory.addItem("-- เลือกหมวดหมู่ --"); // ค่าเริ่มต้น

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT category_id, name FROM categories ORDER BY name ASC";

        try {
            conn = DbConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                int id = rs.getInt("category_id");
                cmbCategory.addItem(name);
                categoryMap.put(name, id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
             try { if (rs != null) rs.close(); } catch (SQLException ex) { ex.printStackTrace(); }
             try { if (pstmt != null) pstmt.close(); } catch (SQLException ex) { ex.printStackTrace(); }
             try { if (conn != null) conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    /** ดึงข้อมูลรายสินค้าในหมวดหมู่ที่ "เลือก" */
    private void loadCategoryDetails(String selectedCategoryName) {
        detailTableModel.setRowCount(0);
        double totalValue = 0;

        String sql;
        if (dataType.equals("sales")) {
            sql = "SELECT p.product_id, p.name, SUM(sd.quantity) AS total_qty, SUM(sd.quantity * sd.price_per_unit) AS total_value FROM sale_details sd JOIN products p ON sd.product_id = p.product_id JOIN categories c ON p.category_id = c.category_id WHERE c.name = ? GROUP BY p.product_id, p.name ORDER BY total_value DESC";
        } else { // profit
            sql = "SELECT p.product_id, p.name, SUM(sd.quantity) AS total_qty, SUM(sd.quantity * (p.sale_price - p.cost_price)) AS total_profit FROM sale_details sd JOIN products p ON sd.product_id = p.product_id JOIN categories c ON p.category_id = c.category_id WHERE c.name = ? GROUP BY p.product_id, p.name HAVING total_profit > 0 ORDER BY total_profit DESC";
        }

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, selectedCategoryName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("product_id"));
                row.add(rs.getString("name"));
                row.add(rs.getInt("total_qty"));
                double value = dataType.equals("sales") ? rs.getDouble("total_value") : rs.getDouble("total_profit");
                row.add(df.format(value));
                detailTableModel.addRow(row);
                totalValue += value;
            }
            rs.close();

            lblFooter.setText("ยอดรวม" + (dataType.equals("sales") ? "ขาย" : "กำไร") + " ทั้งหมด: " + df.format(totalValue) + " บาท");

        } catch (SQLException e) {
            e.printStackTrace();
            lblFooter.setText("Error: ไม่สามารถโหลดข้อมูลได้");
            JOptionPane.showMessageDialog(this, "ไม่สามารถโหลดรายละเอียดได้", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}