package ithubsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class EditProductDialog extends JDialog {

    // 1. Components
    private JTextField txtProductId, txtName, txtCostPrice, txtSalePrice, txtStock;
    private JComboBox<String> cmbCategory;
    private JButton btnSave, btnCancel;

    private HashMap<String, Integer> categoryMap; // (ชื่อ -> ID)
    private HashMap<Integer, String> categoryIdMap; // (ID -> ชื่อ) (ใหม่)

    private ProductManagementPanel mainPanel;
    private String currentProductId; // (ใหม่) รหัสสินค้าที่กำลังจะแก้ไข

    /**
     * Constructor
     * @param owner (JFrame) หน้าต่างแม่
     * @param mainPanel (JPanel) หน้าหลัก (สำหรับ Refresh)
     * @param productIdToEdit (String) รหัสสินค้าที่จะแก้ไข
     */
    public EditProductDialog(JFrame owner, ProductManagementPanel mainPanel, String productIdToEdit) {
        super(owner, "แก้ไขข้อมูลสินค้า", true); // true = Modal
        this.mainPanel = mainPanel;
        this.currentProductId = productIdToEdit;

        setSize(450, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 14);

        // 2. สร้าง Panel สำหรับกรอกข้อมูล (CENTER)
        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Row 1: รหัสสินค้า (บาร์โค้ด)
        formPanel.add(createLabel("รหัสสินค้า (Barcode):", thaiFont));
        txtProductId = new JTextField();
        txtProductId.setFont(thaiFont);
        txtProductId.setEditable(false); // (สำคัญ) ห้ามแก้ไขรหัสสินค้า
        formPanel.add(txtProductId);

        // Row 2: ชื่อสินค้า
        formPanel.add(createLabel("ชื่อสินค้า:", thaiFont));
        txtName = new JTextField();
        txtName.setFont(thaiFont);
        formPanel.add(txtName);

        // Row 3: หมวดหมู่
        formPanel.add(createLabel("หมวดหมู่:", thaiFont));
        cmbCategory = new JComboBox<>();
        cmbCategory.setFont(thaiFont);
        formPanel.add(cmbCategory);

        // Row 4: ราคาซื้อ (ต้นทุน)
        formPanel.add(createLabel("ราคาซื้อ (ต้นทุน):", thaiFont));
        txtCostPrice = new JTextField();
        txtCostPrice.setFont(thaiFont);
        formPanel.add(txtCostPrice);

        // Row 5: ราคาขาย
        formPanel.add(createLabel("ราคาขาย:", thaiFont));
        txtSalePrice = new JTextField();
        txtSalePrice.setFont(thaiFont);
        formPanel.add(txtSalePrice);

        // Row 6: สต็อกเริ่มต้น
        formPanel.add(createLabel("สต็อก:", thaiFont));
        txtStock = new JTextField();
        txtStock.setFont(thaiFont);
        formPanel.add(txtStock);

        add(formPanel, BorderLayout.CENTER);

        // 3. สร้าง Panel สำหรับปุ่ม (SOUTH)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // ... (โค้ดส่วนนี้เหมือน AddProductDialog)
        buttonPanel.setBorder(new EmptyBorder(0, 0, 10, 10));
        btnSave = new JButton("บันทึกการแก้ไข");
        btnSave.setFont(thaiFont);
        btnCancel = new JButton("ยกเลิก");
        btnCancel.setFont(thaiFont);
        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);
        add(buttonPanel, BorderLayout.SOUTH);

        // 4. โหลดข้อมูลหมวดหมู่มาใส่ ComboBox
        loadCategories();

        // 5. (สำคัญ) โหลดข้อมูลสินค้าเก่ามาแสดง
        loadProductDetails();

        // 6. เพิ่ม Action Listeners
        btnCancel.addActionListener(e -> dispose()); // กด "ยกเลิก" = ปิดหน้าต่าง
        
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateProduct();
            }
        });
    }

    // (Helper) สร้าง JLabel
    private JLabel createLabel(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        return label;
    }

    /**
     * ดึงข้อมูลหมวดหมู่จาก DB มาใส่ ComboBox
     */
    private void loadCategories() {
        categoryMap = new HashMap<>();
        categoryIdMap = new HashMap<>();
        cmbCategory.addItem("--- เลือกหมวดหมู่ ---");
        
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
                
                cmbCategory.addItem(name); // เพิ่ม "ชื่อ"
                categoryMap.put(name, id); // เก็บ (ชื่อ -> ID)
                categoryIdMap.put(id, name); // เก็บ (ID -> ชื่อ)
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // (ปิดการเชื่อมต่อ)
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    /**
     * (ใหม่) ดึงข้อมูลสินค้าที่เลือก มาใส่ในฟอร์ม
     */
    private void loadProductDetails() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT * FROM products WHERE product_id = ?";

        try {
            conn = DbConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, currentProductId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                // ดึงข้อมูลมาใส่ในฟอร์ม
                txtProductId.setText(rs.getString("product_id"));
                txtName.setText(rs.getString("name"));
                txtCostPrice.setText(String.valueOf(rs.getDouble("cost_price")));
                txtSalePrice.setText(String.valueOf(rs.getDouble("sale_price")));
                txtStock.setText(String.valueOf(rs.getInt("stock")));
                
                // (สำคัญ) ตั้งค่า ComboBox ให้ตรงกับหมวดหมู่เก่า
                int categoryId = rs.getInt("category_id");
                String categoryName = categoryIdMap.get(categoryId);
                cmbCategory.setSelectedItem(categoryName);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "ไม่สามารถโหลดข้อมูลสินค้าได้", "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // (ปิดการเชื่อมต่อ)
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    /**
     * (ใหม่) เมธอดสำหรับ "อัปเดต" ข้อมูลสินค้า
     */
    private void updateProduct() {
        // 1. ดึงข้อมูลจากฟอร์ม
        String name = txtName.getText();
        String costPriceStr = txtCostPrice.getText();
        String salePriceStr = txtSalePrice.getText();
        String stockStr = txtStock.getText();
        String selectedCategoryName = (String) cmbCategory.getSelectedItem();

        // 2. ตรวจสอบข้อมูล (เหมือนเดิม)
        if (name.isEmpty() || costPriceStr.isEmpty() || salePriceStr.isEmpty() || 
            stockStr.isEmpty() || selectedCategoryName.equals("--- เลือกหมวดหมู่ ---")) {
            JOptionPane.showMessageDialog(this, "กรุณากรอกข้อมูลให้ครบทุกช่อง", "ข้อมูลไม่ครบ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 3. ดึง Category ID
        int categoryId = categoryMap.get(selectedCategoryName);
        
        // 4. แปลง String เป็นตัวเลข (เหมือนเดิม)
        double costPrice;
        double salePrice;
        int stock;
        try {
            costPrice = Double.parseDouble(costPriceStr);
            salePrice = Double.parseDouble(salePriceStr);
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ราคาและสต็อกต้องเป็นตัวเลขเท่านั้น", "ข้อมูลผิดพลาด", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 5. บันทึกลงฐานข้อมูล (ใช้คำสั่ง UPDATE)
        Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = "UPDATE products SET name = ?, cost_price = ?, sale_price = ?, " +
                     "stock = ?, category_id = ? " +
                     "WHERE product_id = ?"; // <--- อัปเดตโดยใช้ ID เดิม

        try {
            conn = DbConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setDouble(2, costPrice);
            pstmt.setDouble(3, salePrice);
            pstmt.setInt(4, stock);
            pstmt.setInt(5, categoryId);
            pstmt.setString(6, currentProductId); // <--- WHERE ID
            
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                JOptionPane.showMessageDialog(this, "อัปเดตข้อมูลสินค้าสำเร็จ!");
                
                // (สำคัญ) สั่งให้หน้าหลัก Refresh ตาราง
                mainPanel.loadProductData();
                
                // (สำคัญ) ปิดปุ่มกลับเป็นสีเทา
                mainPanel.disableEditDeleteButtons();
                
                // ปิดหน้าต่าง Pop-up
                dispose();
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "เกิดข้อผิดพลาดในการอัปเดตข้อมูล", "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}