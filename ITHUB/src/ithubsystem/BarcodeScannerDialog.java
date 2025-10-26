package ithubsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BarcodeScannerDialog extends JDialog {

    // 1. Components
    private JTextField txtBarcode;
    private JButton btnFind;
    
    // 2. ตัวแปรอ้างอิง
    private JFrame ownerFrame;
    private ProductManagementPanel mainPanel; // (สำคัญ) หน้าตารางสินค้า

    /**
     * Constructor
     * @param owner หน้าต่างแม่
     * @param mainPanel หน้า ProductManagementPanel (สำหรับสั่งไฮไลต์)
     */
    public BarcodeScannerDialog(JFrame owner, ProductManagementPanel mainPanel) {
        super(owner, "ค้นหาด้วยบาร์โค้ด", true); // Modal
        this.ownerFrame = owner;
        this.mainPanel = mainPanel;

        setSize(350, 150);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 14);

        // --- 1. Panel กรอกข้อมูล (CENTER) ---
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JLabel lblInfo = new JLabel("สแกนบาร์โค้ด:");
        lblInfo.setFont(thaiFont);
        inputPanel.add(lblInfo);
        
        txtBarcode = new JTextField(20);
        txtBarcode.setFont(thaiFont);
        inputPanel.add(txtBarcode);
        
        add(inputPanel, BorderLayout.CENTER);

        // --- 2. Panel ปุ่ม (SOUTH) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnFind = new JButton("ค้นหา");
        btnFind.setFont(thaiFont);
        buttonPanel.add(btnFind);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- 3. เพิ่ม Listeners ---
        
        // (เมื่อกด Enter ในช่อง Text)
        txtBarcode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findAndSelectProduct();
            }
        });
        
        // (เมื่อกดปุ่ม "ค้นหา")
        btnFind.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findAndSelectProduct();
            }
        });
    }

    /**
     * (สำคัญ) ค้นหาสินค้า และสั่งให้ MainPanel ไฮไลต์
     */
    private void findAndSelectProduct() {
        String productId = txtBarcode.getText().trim();
        if (productId.isEmpty()) {
            return;
        }

        // 1. ตรวจสอบว่ามีสินค้านี้ใน DB หรือไม่
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT COUNT(*) FROM products WHERE product_id = ?";

        try {
            conn = DbConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, productId);
            rs = pstmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                // --- 2. ถ้าเจอ ---
                // สั่งให้หน้าหลัก (mainPanel) ไฮไลต์แถวนี้
                mainPanel.selectProductById(productId);
                
                // ปิด Pop-up
                dispose();
                
            } else {
                // --- 3. ถ้าไม่เจอ ---
                JOptionPane.showMessageDialog(this, "ไม่พบรหัสสินค้านี้ในระบบ", "ไม่พบข้อมูล", JOptionPane.ERROR_MESSAGE);
                txtBarcode.selectAll();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "เกิดข้อผิดพลาดในการค้นหา", "Database Error", JOptionPane.ERROR_MESSAGE);
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
}