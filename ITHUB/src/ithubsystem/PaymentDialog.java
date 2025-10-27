package ithubsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter; // (ใหม่)
import java.awt.event.KeyEvent; // (ใหม่)
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; // (ใหม่)
import java.util.Vector;

public class PaymentDialog extends JDialog {

    // 1. Components
    private JLabel lblTotalAmount, lblChange;
    private JTextField txtAmountReceived;
    private JButton btnConfirmPayment;
    
    // 2. ข้อมูลที่รับมา
    private double totalAmount;
    private String cashierUsername;
    private DefaultTableModel cartTableModel; // (สำคัญ) ตะกร้าสินค้า
    private JFrame ownerFrame; // หน้าต่างแม่ (MainMenuPage)
    private SalePanel salePanel; // หน้าขาย (สำหรับสั่งปิด)

    public PaymentDialog(JFrame owner, SalePanel salePanel, double totalAmount, 
                         String cashierUsername, DefaultTableModel cartTableModel) 
    {
        super(owner, "ชำระเงิน", true); // Modal
        this.ownerFrame = owner;
        this.salePanel = salePanel;
        this.totalAmount = totalAmount;
        this.cashierUsername = cashierUsername;
        this.cartTableModel = cartTableModel;

        setSize(400, 300);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 18);
        Font boldFont = new Font("Tahoma", Font.BOLD, 24);

        // --- 1. Panel แสดงผล (CENTER) ---
        JPanel infoPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        infoPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // ยอดรวม
        JLabel lblTotalTitle = new JLabel("ยอดรวมสุทธิ:");
        lblTotalTitle.setFont(thaiFont);
        infoPanel.add(lblTotalTitle);
        
        lblTotalAmount = new JLabel(String.format("%,.2f บาท", totalAmount));
        lblTotalAmount.setFont(boldFont);
        lblTotalAmount.setForeground(Color.BLUE);
        infoPanel.add(lblTotalAmount);

        // รับเงิน
        JLabel lblReceivedTitle = new JLabel("รับเงินมา (บาท):");
        lblReceivedTitle.setFont(thaiFont);
        infoPanel.add(lblReceivedTitle);
        
        txtAmountReceived = new JTextField();
        txtAmountReceived.setFont(boldFont);
        infoPanel.add(txtAmountReceived);

        // เงินทอน
        JLabel lblChangeTitle = new JLabel("เงินทอน:");
        lblChangeTitle.setFont(thaiFont);
        infoPanel.add(lblChangeTitle);
        
        lblChange = new JLabel("0.00 บาท");
        lblChange.setFont(boldFont);
        lblChange.setForeground(Color.RED);
        infoPanel.add(lblChange);
        
        add(infoPanel, BorderLayout.CENTER);

        // --- 2. Panel ปุ่ม (SOUTH) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnConfirmPayment = new JButton("ยืนยันการชำระเงิน");
        btnConfirmPayment.setFont(thaiFont);
        btnConfirmPayment.setBackground(new Color(46, 204, 113));
        btnConfirmPayment.setForeground(Color.WHITE);
        btnConfirmPayment.setEnabled(false); // (ปิดไว้ก่อน จนกว่าเงินจะพอ)
        buttonPanel.add(btnConfirmPayment);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- 3. เพิ่ม Listeners ---
        
        // (สำคัญ) เมื่อพิมพ์ในช่อง "รับเงิน"
        txtAmountReceived.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                calculateChange();
            }
        });
        
        // (สำคัญ) เมื่อกดปุ่ม "ยืนยัน"
        btnConfirmPayment.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processSale();
            }
        });
    }

    /**
     * คำนวณเงินทอน และเปิด/ปิดปุ่มยืนยัน
     */
    private void calculateChange() {
        try {
            double amountReceived = Double.parseDouble(txtAmountReceived.getText());
            if (amountReceived >= totalAmount) {
                double change = amountReceived - totalAmount;
                lblChange.setText(String.format("%,.2f บาท", change));
                btnConfirmPayment.setEnabled(true); // <--- เปิดปุ่ม
            } else {
                lblChange.setText("0.00 บาท");
                btnConfirmPayment.setEnabled(false); // <--- ปิดปุ่ม
            }
        } catch (NumberFormatException e) {
            // ถ้ากรอกตัวอักษร
            lblChange.setText("0.00 บาท");
            btnConfirmPayment.setEnabled(false);
        }
    }

    /**
     * (สำคัญที่สุด) บันทึกการขาย, ตัดสต็อก
     */
    private void processSale() {
        Connection conn = null;
        PreparedStatement pstmtSale = null;
        PreparedStatement pstmtDetail = null;
        PreparedStatement pstmtUpdateStock = null;
        ResultSet rs = null;
        
        // --- (A) ดึง ID ของ User (พนักงาน) ---
        int userId = -1;
        try (PreparedStatement pstmtUser = DbConnection.getConnection().prepareStatement("SELECT user_id FROM users WHERE username = ?")) {
            pstmtUser.setString(1, cashierUsername);
            rs = pstmtUser.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: ไม่พบ User พนักงาน", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (userId == -1) return; // (ไม่ควรเกิดขึ้น)


        // --- (B) เริ่ม Transaction (กันข้อมูลพัง) ---
        try {
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false); // <--- 1. ปิด Auto Commit

            // --- 2. INSERT ลงตาราง 'sales' (ใบเสร็จหลัก) ---
            String sqlSale = "INSERT INTO sales (user_id, total_amount) VALUES (?, ?)";
            pstmtSale = conn.prepareStatement(sqlSale, Statement.RETURN_GENERATED_KEYS); // <--- ขอ Key ที่เพิ่งสร้าง
            pstmtSale.setInt(1, userId);
            pstmtSale.setDouble(2, totalAmount);
            pstmtSale.executeUpdate();

            // --- 3. ดึง 'sale_id' ที่เพิ่งสร้างออกมา ---
            int newSaleId = -1;
            rs = pstmtSale.getGeneratedKeys();
            if (rs.next()) {
                newSaleId = rs.getInt(1);
            }
            if (newSaleId == -1) throw new SQLException("ไม่สามารถสร้าง Sale ID ได้");

            // --- 4. วนลูปตะกร้า (cartTableModel) ---
            String sqlDetail = "INSERT INTO sale_details (sale_id, product_id, quantity, price_per_unit) VALUES (?, ?, ?, ?)";
            pstmtDetail = conn.prepareStatement(sqlDetail);
            
            String sqlUpdateStock = "UPDATE products SET stock = stock - ? WHERE product_id = ?";
            pstmtUpdateStock = conn.prepareStatement(sqlUpdateStock);

            for (int i = 0; i < cartTableModel.getRowCount(); i++) {
                String productId = (String) cartTableModel.getValueAt(i, 4); // รหัส (ซ่อน)
                int quantity = Integer.parseInt(cartTableModel.getValueAt(i, 2).toString());
                double price = (Double) cartTableModel.getValueAt(i, 3);
                
                // 4.1 INSERT ลง 'sale_details'
                pstmtDetail.setInt(1, newSaleId);
                pstmtDetail.setString(2, productId);
                pstmtDetail.setInt(3, quantity);
                pstmtDetail.setDouble(4, price);
                pstmtDetail.addBatch(); // (เพิ่มเข้า Batch)
                
                // 4.2 UPDATE 'products' (ตัดสต็อก)
                pstmtUpdateStock.setInt(1, quantity);
                pstmtUpdateStock.setString(2, productId);
                pstmtUpdateStock.addBatch(); // (เพิ่มเข้า Batch)
            }
            
            // --- 5. สั่งรัน Batch (Detail และ Stock) ---
            pstmtDetail.executeBatch();
            pstmtUpdateStock.executeBatch();

            /// --- 6. (สำเร็จ) สั่ง Commit Transaction ---
            conn.commit();

            // (*** แก้ไข: เปลี่ยนจาก JOptionPane เป็นเปิด ReceiptDialog ***)
            // JOptionPane.showMessageDialog(this, "บันทึกการขายสำเร็จ!\nเงินทอน: " + lblChange.getText());

            // 6.1 ดึงข้อมูลสำหรับใบเสร็จ
            double received = 0;
            try { received = Double.parseDouble(txtAmountReceived.getText()); } catch (Exception ignored) {}
            double calculatedChange = received - totalAmount;

            // 6.2 เปิดหน้าต่างใบเสร็จ
            ReceiptDialog receiptDialog = new ReceiptDialog(
                ownerFrame, // หน้าต่างแม่ (MainMenuPage)
                newSaleId, // ID บิลใหม่
                cashierUsername, // ชื่อพนักงาน
                cartTableModel, // ข้อมูลตะกร้า
                totalAmount, // ยอดรวม
                received, // เงินที่รับ
                calculatedChange // เงินทอน
            );
            dispose(); // ปิดหน้า Pop-up ชำระเงิน (ก่อนแสดงใบเสร็จ)
            receiptDialog.setVisible(true); // แสดงใบเสร็จ

            // 6.3 (สำคัญ) สั่งให้หน้า SalePanel กลับไปหน้าหลัก
            salePanel.saleCompleted();
            // --- (*** สิ้นสุดการแก้ไข ***) ---

        } catch (SQLException e) {
            // --- 7. (ล้มเหลว) สั่ง Rollback ---
            e.printStackTrace();
            try {
                if (conn != null) {
                    conn.rollback();
                    JOptionPane.showMessageDialog(this, "เกิดข้อผิดพลาด! การขายถูกยกเลิก\n" + e.getMessage(), "Transaction Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            // 8. ปิดทุกอย่าง
            try {
                if (rs != null) rs.close();
                if (pstmtSale != null) pstmtSale.close();
                if (pstmtDetail != null) pstmtDetail.close();
                if (pstmtUpdateStock != null) pstmtUpdateStock.close();
                if (conn != null) {
                    conn.setAutoCommit(true); // <--- เปิด Auto Commit คืน
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}