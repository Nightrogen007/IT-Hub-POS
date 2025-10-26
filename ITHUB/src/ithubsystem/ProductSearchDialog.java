package ithubsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter; // (ใหม่)
import java.awt.event.MouseEvent;  // (ใหม่)
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

public class ProductSearchDialog extends JDialog {

    // 1. Components
    private JTable searchResultTable;
    private DefaultTableModel searchTableModel;
    private JScrollPane tableScrollPane;
    private JButton btnSelect;

    // 2. ข้อมูล
    private SalePanel salePanel; // หน้าขายหลัก
    private String initialSearchText;

    /**
     * Constructor
     * @param owner หน้าต่างแม่
     * @param salePanel หน้า SalePanel (สำหรับส่ง ID กลับไป)
     * @param searchText คำค้นหาเริ่มต้น
     */
    public ProductSearchDialog(JFrame owner, SalePanel salePanel, String searchText) {
        super(owner, "ค้นหาสินค้า", true); // Modal
        this.salePanel = salePanel;
        this.initialSearchText = searchText;

        setSize(600, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        Font thaiFont = new Font("Tahoma", Font.PLAIN, 14);

        // --- 1. ตารางแสดงผล (CENTER) ---
        String[] headers = {"รหัสสินค้า", "ชื่อสินค้า", "ราคาขาย", "สต็อก"};
        searchTableModel = new DefaultTableModel(headers, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        searchResultTable = new JTable(searchTableModel);
        searchResultTable.setFont(thaiFont);
        searchResultTable.setRowHeight(25);
        searchResultTable.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 14));
        searchResultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tableScrollPane = new JScrollPane(searchResultTable);
        add(tableScrollPane, BorderLayout.CENTER);

        // --- 2. ปุ่มเลือก (SOUTH) ---
        JPanel buttonPanel = new JPanel();
        btnSelect = new JButton("เลือกสินค้านี้");
        btnSelect.setFont(thaiFont);
        buttonPanel.add(btnSelect);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- 3. โหลดข้อมูล ---
        searchProducts();

        // --- 4. เพิ่ม Listeners ---

        // (เมื่อกดปุ่ม "เลือก")
        btnSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectAndClose();
            }
        });

        // (*** ใหม่: เมื่อดับเบิลคลิกที่แถวในตาราง ***)
        searchResultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // ตรวจสอบดับเบิลคลิก
                    selectAndClose();
                }
            }
        });
    }

    /**
     * ค้นหาสินค้าใน DB ตาม initialSearchText
     */
    private void searchProducts() {
        searchTableModel.setRowCount(0); // ล้างตารางเก่า
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        // ค้นหาทั้งใน product_id และ name (ใช้ LIKE '%...%')
        String sql = "SELECT product_id, name, sale_price, stock " +
                     "FROM products " +
                     "WHERE product_id LIKE ? OR name LIKE ? " +
                     "ORDER BY name ASC";

        try {
            conn = DbConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            // ใส่ % เพื่อให้ค้นหา 'คำที่ขึ้นต้นด้วย' หรือ 'คำที่มีคำนี้อยู่'
            pstmt.setString(1, "%" + initialSearchText + "%");
            pstmt.setString(2, "%" + initialSearchText + "%");
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("product_id"));
                row.add(rs.getString("name"));
                row.add(rs.getDouble("sale_price"));
                row.add(rs.getInt("stock"));
                searchTableModel.addRow(row);
            }

            // (ถ้าไม่เจอเลย)
            if (searchTableModel.getRowCount() == 0) {
                 JOptionPane.showMessageDialog(this, "ไม่พบสินค้าที่ตรงกับ '" + initialSearchText + "'", "ไม่พบข้อมูล", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "เกิดข้อผิดพลาดในการค้นหา", "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // ปิด Connection
             try { if (rs != null) rs.close(); } catch (SQLException ex) { ex.printStackTrace(); }
             try { if (pstmt != null) pstmt.close(); } catch (SQLException ex) { ex.printStackTrace(); }
             try { if (conn != null) conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    /**
     * (ใหม่) ดึง ID สินค้าที่เลือก ส่งกลับไปให้ SalePanel แล้วปิด Dialog
     */
    private void selectAndClose() {
        int selectedRow = searchResultTable.getSelectedRow();
        if (selectedRow != -1) {
            // ดึง product_id จากคอลัมน์ 0
            String selectedProductId = (String) searchTableModel.getValueAt(selectedRow, 0);

            // สั่งให้ SalePanel เพิ่มสินค้านี้ลงตะกร้า
            salePanel.addProductToCartById(selectedProductId);

            // ปิดหน้าต่าง Pop-up นี้
            dispose();
        } else {
             JOptionPane.showMessageDialog(this, "กรุณาเลือกสินค้าจากรายการ", "ยังไม่ได้เลือก", JOptionPane.WARNING_MESSAGE);
        }
    }
}