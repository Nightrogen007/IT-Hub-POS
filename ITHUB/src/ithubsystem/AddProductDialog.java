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
import java.util.HashMap; // <--- Import เพิ่ม

public class AddProductDialog extends JDialog {

    // 1. Components
    private JTextField txtProductId, txtName, txtCostPrice, txtSalePrice, txtStock;
    private JComboBox<String> cmbCategory;
    private JButton btnSave, btnCancel;

    // (ใหม่) สำหรับเก็บชื่อหมวดหมู่ (Key) และ ID (Value)
    private HashMap<String, Integer> categoryMap;

    // (ใหม่) ตัวแปรสำหรับอ้างอิงกลับไปที่หน้าหลัก
    private ProductManagementPanel mainPanel;

    /**
     * Constructor
     * @param owner (JFrame) หน้าต่างแม่ของมัน (MainMenuPage)
     * @param mainPanel (JPanel) หน้า ProductManagementPanel (สำหรับ Refresh)
     */
    public AddProductDialog(JFrame owner, ProductManagementPanel mainPanel) {
        super(owner, "เพิ่มสินค้าใหม่", true); // true = Modal (ต้องปิดหน้าต่างนี้ก่อน)
        this.mainPanel = mainPanel;

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
        formPanel.add(createLabel("สต็อกเริ่มต้น:", thaiFont));
        txtStock = new JTextField();
        txtStock.setFont(thaiFont);
        formPanel.add(txtStock);

        add(formPanel, BorderLayout.CENTER);

        // 3. สร้าง Panel สำหรับปุ่ม (SOUTH)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EmptyBorder(0, 0, 10, 10));
        
        btnSave = new JButton("บันทึก");
        btnSave.setFont(thaiFont);
        
        btnCancel = new JButton("ยกเลิก");
        btnCancel.setFont(thaiFont);
        
        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);
        add(buttonPanel, BorderLayout.SOUTH);

        // 4. โหลดข้อมูลหมวดหมู่มาใส่ ComboBox
        loadCategories();

        // 5. เพิ่ม Action Listeners
        btnCancel.addActionListener(e -> dispose()); // กด "ยกเลิก" = ปิดหน้าต่าง
        
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveProduct();
            }
        });
    }

    // (Helper) สร้าง JLabel พร้อม Font
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
                
                cmbCategory.addItem(name); // เพิ่ม "ชื่อ" ลง ComboBox
                categoryMap.put(name, id); // เก็บ "ชื่อ" (Key) และ "ID" (Value)
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "ไม่สามารถโหลดข้อมูลหมวดหมู่ได้", "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * (สำคัญ) เมธอดสำหรับบันทึกข้อมูลสินค้าใหม่
     */
    private void saveProduct() {
        // 1. ดึงข้อมูลจากฟอร์ม
        String productId = txtProductId.getText();
        String name = txtName.getText();
        String costPriceStr = txtCostPrice.getText();
        String salePriceStr = txtSalePrice.getText();
        String stockStr = txtStock.getText();
        String selectedCategoryName = (String) cmbCategory.getSelectedItem();

        // 2. ตรวจสอบข้อมูลเบื้องต้น
        if (productId.isEmpty() || name.isEmpty() || costPriceStr.isEmpty() || 
            salePriceStr.isEmpty() || stockStr.isEmpty() || 
            selectedCategoryName.equals("--- เลือกหมวดหมู่ ---")) {
            
            JOptionPane.showMessageDialog(this, "กรุณากรอกข้อมูลให้ครบทุกช่อง", "ข้อมูลไม่ครบ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 3. ดึง Category ID จาก HashMap
        int categoryId = categoryMap.get(selectedCategoryName);
        
        // 4. แปลง String เป็นตัวเลข (ต้องดัก Error)
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

        // 5. บันทึกลงฐานข้อมูล
        Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = "INSERT INTO products (product_id, name, description, cost_price, sale_price, stock, category_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            conn = DbConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, productId);
            pstmt.setString(2, name);
            pstmt.setString(3, ""); // (ใส่ Description ว่างๆ ไปก่อน)
            pstmt.setDouble(4, costPrice);
            pstmt.setDouble(5, salePrice);
            pstmt.setInt(6, stock);
            pstmt.setInt(7, categoryId);
            
            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                JOptionPane.showMessageDialog(this, "บันทึกสินค้าใหม่สำเร็จ!");
                
                // (สำคัญ) สั่งให้หน้าหลัก Refresh ตาราง
                mainPanel.loadProductData();
                
                // ปิดหน้าต่าง Pop-up
                dispose();
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            // (เช็ค Error รหัสซ้ำ)
            if (e.getErrorCode() == 1062) { // 1062 = Duplicate entry
                JOptionPane.showMessageDialog(this, "รหัสสินค้า (Barcode) นี้มีในระบบแล้ว", "รหัสซ้ำ", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "เกิดข้อผิดพลาดในการบันทึกข้อมูล", "Database Error", JOptionPane.ERROR_MESSAGE);
            }
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