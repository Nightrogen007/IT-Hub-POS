package ithubsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

public class SalePanel extends JPanel {

    // 1. Components
    private JTextField txtBarcodeScan;
    private JTable cartTable;
    private DefaultTableModel cartTableModel;
    private JScrollPane tableScrollPane;
    private JButton btnSearchProduct;
    
    private JButton btnNext;
    private JButton btnDeleteItem; // (ใหม่) ปุ่มลบที่ง่ายกว่า
    
    private String cashierUsername; // (ใหม่) พนักงานที่ล็อกอินอยู่

    public SalePanel(String username) {
        this.cashierUsername = username;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        Font thaiFont = new Font("Tahoma", Font.PLAIN, 14);
        Font titleFont = new Font("Tahoma", Font.BOLD, 20);

        // --- 1. Panel ด้านบน (NORTH) (*** แก้ไข ***) ---
        JPanel pnlTop = new JPanel(new BorderLayout(5, 0)); // (เพิ่ม HGap)

        JLabel lblTitle = new JLabel("กรุณาสแกนบาร์โค้ดสินค้า หรือ กดค้นหา"); // (แก้ไขข้อความ)
        lblTitle.setFont(titleFont);
        lblTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        pnlTop.add(lblTitle, BorderLayout.NORTH);

        txtBarcodeScan = new JTextField();
        txtBarcodeScan.setFont(new Font("Tahoma", Font.PLAIN, 16));
        txtBarcodeScan.setToolTipText("สแกนบาร์โค้ดที่นี่ แล้วกด Enter หรือ กดปุ่มค้นหา");
        pnlTop.add(txtBarcodeScan, BorderLayout.CENTER);

        // (*** เพิ่มปุ่มค้นหา ***)
        btnSearchProduct = new JButton("🔎 ค้นหา"); // ใช้ Emoji หรือ Icon ก็ได้
        btnSearchProduct.setFont(thaiFont);
        pnlTop.add(btnSearchProduct, BorderLayout.EAST); // วางไว้ด้านขวา

        add(pnlTop, BorderLayout.NORTH);

        // --- 2. ตาราง (CENTER) (เหมือนเดิม) ---
        createCartTable();
        add(tableScrollPane, BorderLayout.CENTER);

        // --- 3. Panel ด้านล่าง (SOUTH) (เหมือนเดิม) ---
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnDeleteItem = new JButton("ลบรายการที่เลือก");
        btnDeleteItem.setFont(thaiFont);
        btnDeleteItem.setBackground(Color.RED);
        btnDeleteItem.setForeground(Color.WHITE);
        pnlBottom.add(btnDeleteItem);
        btnNext = new JButton("ถัดไป");
        btnNext.setFont(thaiFont);
        pnlBottom.add(btnNext);
        add(pnlBottom, BorderLayout.SOUTH);

        // --- 4. เพิ่ม Action Listeners ---

        // (เมื่อกด Enter ที่ช่องสแกน - เหมือนเดิม)
        txtBarcodeScan.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String productId = txtBarcodeScan.getText().trim();
                if (!productId.isEmpty()) {
                    addProductToCart(productId); // เรียกเมธอดเดิม
                }
            }
        });

        // (*** เพิ่ม: เมื่อกดปุ่มค้นหา ***)
        btnSearchProduct.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1. ดึงข้อความที่พิมพ์ค้างไว้
                String searchText = txtBarcodeScan.getText().trim();
                // 2. หาหน้าต่างหลัก
                JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(SalePanel.this);
                // 3. เปิด Dialog ค้นหา
                ProductSearchDialog dialog = new ProductSearchDialog(owner, SalePanel.this, searchText);
                dialog.setVisible(true);
                // (เมื่อ Dialog ปิด มันจะเรียก addProductToCartById เอง)
            }
        });

        // (เมื่อกดปุ่มลบ - เหมือนเดิม)
        btnDeleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedItem();
            }
        });

        // (เมื่อกดปุ่มถัดไป - เหมือนเดิม)
     // (*** ฉบับแทนที่ - เพิ่มการตรวจสอบสต็อก ***)
        btnNext.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1. เช็คว่าตะกร้าว่างหรือไม่
                if (cartTableModel.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(SalePanel.this,
                            "กรุณาเพิ่มสินค้าลงในตะกร้าก่อน",
                            "ตะกร้าว่าง", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // --- (*** ใหม่: Loop ตรวจสอบสต็อก ***) ---
                Connection conn = null;
                PreparedStatement pstmtCheckStock = null;
                ResultSet rsStock = null;
                boolean stockOk = true; // ตัวแปรเช็คสถานะสต็อก

                try {
                    conn = DbConnection.getConnection();
                    String sqlCheckStock = "SELECT name, stock FROM products WHERE product_id = ?";
                    pstmtCheckStock = conn.prepareStatement(sqlCheckStock);

                    // วนลูปตรวจสอบทุกรายการในตะกร้า
                    for (int i = 0; i < cartTableModel.getRowCount(); i++) {
                        String productId = (String) cartTableModel.getValueAt(i, 4); // คอลัมน์ product_id ที่ซ่อนอยู่
                        int quantityInCart = Integer.parseInt(cartTableModel.getValueAt(i, 2).toString()); // จำนวนในตะกร้า
                        String productName = (String) cartTableModel.getValueAt(i, 1); // ชื่อสินค้า

                        // ดึงสต็อกปัจจุบันจาก DB
                        pstmtCheckStock.setString(1, productId);
                        rsStock = pstmtCheckStock.executeQuery();

                        if (rsStock.next()) {
                            int currentStock = rsStock.getInt("stock");
                            // เปรียบเทียบสต็อก
                            if (quantityInCart > currentStock) {
                                // --- พบว่าสต็อกไม่พอ! ---
                                JOptionPane.showMessageDialog(SalePanel.this,
                                        "สินค้า \"" + productName + "\" มีไม่เพียงพอในสต็อก!\n" +
                                        "ต้องการ: " + quantityInCart + ", คงเหลือ: " + currentStock,
                                        "สต็อกไม่พอ", JOptionPane.ERROR_MESSAGE);
                                stockOk = false; // ตั้งค่าสถานะเป็น false
                                rsStock.close(); // ปิด ResultSet ก่อน
                                break; // ออกจาก loop ทันที
                            }
                        } else {
                            // ไม่พบสินค้าใน DB (ไม่ควรเกิดขึ้นถ้าเพิ่มถูกต้อง)
                            JOptionPane.showMessageDialog(SalePanel.this,
                                    "เกิดข้อผิดพลาด: ไม่พบสินค้า \"" + productName + "\" ในฐานข้อมูล",
                                    "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
                            stockOk = false;
                            rsStock.close();
                            break;
                        }
                        rsStock.close(); // ปิด ResultSet ของรายการนี้
                    } // จบ loop for

                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(SalePanel.this,
                            "เกิดข้อผิดพลาดในการตรวจสอบสต็อก", "Database Error", JOptionPane.ERROR_MESSAGE);
                    stockOk = false; // หยุดการทำงานถ้าเกิด Error DB
                } finally {
                    // ปิดทรัพยากรฐานข้อมูลที่ใช้ตรวจสอบสต็อก
                    try { if (rsStock != null) rsStock.close(); } catch (SQLException ex) { ex.printStackTrace(); }
                    try { if (pstmtCheckStock != null) pstmtCheckStock.close(); } catch (SQLException ex) { ex.printStackTrace(); }
                    try { if (conn != null) conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
                }

                // --- (*** สิ้นสุดการตรวจสอบสต็อก ***) ---

                // 3. ถ้าการตรวจสอบสต็อกไม่ผ่าน ให้หยุดตรงนี้
                if (!stockOk) {
                    return; // ไม่ไปต่อที่หน้าชำระเงิน
                }

                // 4. คำนวณยอดรวม (ทำเฉพาะเมื่อสต็อก OK)
                double totalAmount = 0;
                for (int i = 0; i < cartTableModel.getRowCount(); i++) {
                    int quantity = Integer.parseInt(cartTableModel.getValueAt(i, 2).toString());
                    double price = (Double) cartTableModel.getValueAt(i, 3);
                    totalAmount += (quantity * price);
                }
                ////

                // 5. หาหน้าต่างแม่
                JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(SalePanel.this);

                // 6. เปิดหน้าต่างชำระเงิน
                PaymentDialog dialog = new PaymentDialog(owner, SalePanel.this,
                    totalAmount, cashierUsername, cartTableModel);
                dialog.setVisible(true);
            }
        }); // จบ ActionListener ของ btnNext
    } // (*** นี่คือวงเล็บปิดของ Constructor ***)(นี่คือวงเล็บปิดของ Constructor)
    

    /**
     * สร้างตารางและ Model (CENTER)
     */
    private void createCartTable() {
        String[] columnNames = {
            "รายการที่", "ชื่อสินค้า", "จำนวน", "ราคา/หน่วย", "รหัสสินค้า (ซ่อน)"
        };
        
        cartTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
               // แก้ไขได้เฉพาะคอลัมน์ "จำนวน" (index 2)
               return column == 2;
            }
        };

        cartTable = new JTable(cartTableModel);
        cartTable.setFont(new Font("Tahoma", Font.PLAIN, 14));
        cartTable.setRowHeight(25);
        cartTable.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 14));
        
        // (สำคัญ) ซ่อนคอลัมน์ "รหัสสินค้า" (เราใช้เก็บ ID แต่ไม่โชว์)
        cartTable.getColumnModel().getColumn(4).setMinWidth(0);
        cartTable.getColumnModel().getColumn(4).setMaxWidth(0);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(0);
        
        tableScrollPane = new JScrollPane(cartTable);
    }
    //
    /**
     * ค้นหาสินค้าใน DB แล้วเพิ่มลงตาราง (ตะกร้า)
     */
    private void addProductToCart(String productId) {
        
        // 1. ตรวจสอบว่ามีสินค้านี้ในตะกร้า "แล้วหรือยัง"
        for (int i = 0; i < cartTableModel.getRowCount(); i++) {
            String idInCart = (String) cartTableModel.getValueAt(i, 4); // คอลัมน์ที่ 4 (ซ่อน)
            
            if (idInCart.equals(productId)) {
                // ถ้ามีแล้ว -> ให้บวก "จำนวน" (คอลัมน์ 2)
                int currentQty = Integer.parseInt(cartTableModel.getValueAt(i, 2).toString());
                cartTableModel.setValueAt(currentQty + 1, i, 2);
                
                txtBarcodeScan.setText(""); // ล้างช่องสแกน
                return; // จบการทำงาน
            }
        }

        // 2. ถ้ายังไม่มี -> ค้นหาในฐานข้อมูล
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT name, sale_price, stock FROM products WHERE product_id = ?";

        try {
            conn = DbConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, productId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                // 3. ถ้าเจอสินค้า
                int stock = rs.getInt("stock");
                
                // (ป้องกัน) เช็คสต็อก
                if (stock <= 0) {
                    JOptionPane.showMessageDialog(this, "สินค้าหมดสต็อก!", "Error", JOptionPane.ERROR_MESSAGE);
                    txtBarcodeScan.setText("");
                    return;
                }
                
                String name = rs.getString("name");
                double salePrice = rs.getDouble("sale_price");
                
                // 4. เพิ่มแถวใหม่ลงตาราง
                Vector<Object> row = new Vector<>();
                row.add(cartTableModel.getRowCount() + 1); // รายการที่
                row.add(name);
                row.add(1); // จำนวนเริ่มต้น
                row.add(salePrice);
                row.add(productId); // รหัสสินค้า (ซ่อน)
                
                cartTableModel.addRow(row);
                
            } else {
                // 5. ถ้าไม่เจอสินค้า
                JOptionPane.showMessageDialog(this, "ไม่พบรหัสสินค้านี้ในระบบ!", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: ไม่สามารถเชื่อมต่อฐานข้อมูลได้", "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        
        txtBarcodeScan.setText(""); // ล้างช่องสแกน
    }
    
    /**
     * (ใหม่) ลบแถวที่เลือกในตาราง
     */
    private void deleteSelectedItem() {
        int selectedRow = cartTable.getSelectedRow();
        
        if (selectedRow != -1) { // (-1 คือไม่ได้เลือก)
            cartTableModel.removeRow(selectedRow);
            
            // (อัปเดต) เรียง "รายการที่" (คอลัมน์ 0) ใหม่
            for (int i = 0; i < cartTableModel.getRowCount(); i++) {
                cartTableModel.setValueAt(i + 1, i, 0);
            }
        } else {
            JOptionPane.showMessageDialog(this, "กรุณาคลิกเลือกแถวที่ต้องการลบ", "ไม่ได้เลือก", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * (ใหม่) ถูกเรียกเมื่อการขายเสร็จสิ้น (โดย PaymentDialog)
     * สั่งให้กลับไปหน้า Dashboard
     */
    public void saleCompleted() {
        // 1. หาหน้าต่างแม่ (MainMenuPage)
        MainMenuPage mainMenu = (MainMenuPage) SwingUtilities.getWindowAncestor(this);
        
        // 2. สั่งให้ MainMenu กลับไปแสดงหน้า Dashboard
        if (mainMenu != null) {
            mainMenu.showDashboardPanel();
        }
    }
    
    /**
     * (*** ใหม่: ถูกเรียกโดย ProductSearchDialog ***)
     * เมื่อมีการเลือกสินค้าจากหน้าต่างค้นหา
     */
    public void addProductToCartById(String productId) {
        // 1. เติม ID ลงในช่องสแกน (เผื่อไว้เฉยๆ)
        txtBarcodeScan.setText(productId);
        // 2. เรียกเมธอดเดิมเพื่อเพิ่มลงตะกร้า
        addProductToCart(productId);
        // 3. (Optional) Focus กลับไปที่ช่องสแกน เผื่อสแกนต่อ
        txtBarcodeScan.requestFocusInWindow();
    }
}