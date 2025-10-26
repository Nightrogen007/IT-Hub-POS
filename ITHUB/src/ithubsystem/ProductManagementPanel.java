package ithubsystem; // <--- Package ของคุณ

import javax.swing.*;
import java.util.regex.Pattern; // (ใหม่) สำหรับ Escape Regex
import java.util.List;
import java.util.ArrayList;
import javax.swing.table.TableRowSorter;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent; // <--- Import เพิ่ม
import javax.swing.event.TableModelListener; // <--- Import เพิ่ม
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import javax.swing.RowFilter;

public class ProductManagementPanel extends JPanel {

    private JPanel pnlTopBar;
    private JTextField txtSearchByName;
    private JButton btnScanBarcode;
    private JComboBox<String> cmbCategory;
    private TableRowSorter<DefaultTableModel> sorter;
    
    private JPanel pnlBottomBar; 
    private JButton btnAdd, btnEdit, btnDelete; // <--- ลบ btnSelect ออก
    
    private JTable productTable;
    private DefaultTableModel tableModel; 
    private JScrollPane tableScrollPane;
    
    // (ใหม่) ตัวแปรสำหรับป้องกัน Loop เวลาอัปเดต Checkbox
    private boolean isUpdatingCheckbox = false;

    public ProductManagementPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        createTopBar();
        add(pnlTopBar, BorderLayout.NORTH);

        createProductTable(); // <--- สร้างตาราง (รวมถึง Model)
        add(tableScrollPane, BorderLayout.CENTER);
        
        createBottomBar(); // <--- สร้างปุ่ม
        
        // (แก้ไข) สร้าง Panel ล่างแบบใหม่ (ที่ไม่มีปุ่ม 'เลือก' แล้ว)
        JPanel pnlBottomContainer = new JPanel(new BorderLayout());
        pnlBottomContainer.add(pnlBottomBar, BorderLayout.WEST); // (ปุ่ม เขียว/แดง/เหลือง ชิดซ้าย)
        add(pnlBottomContainer, BorderLayout.SOUTH); // เพิ่ม Container นี้ลงไปแทน

        loadProductData();
        loadCategoryData();
        
        // (ใหม่) เพิ่ม Listener ให้กับตาราง
        addTableListener();
        
        // (ใหม่) ปิดปุ่มแก้ไข/ลบ ตอนเริ่มต้น
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);
    }
    
    /**
     * สร้าง Panel ด้านบน (NORTH)
     * (เหมือนเดิม)
     */
    private void createTopBar() {
        pnlTopBar = new JPanel(new FlowLayout(FlowLayout.LEFT)); 
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 14);

        // 1. ค้นหาด้วยชื่อ
        JLabel lblSearchName = new JLabel("ค้นหาสินค้าด้วยชื่อ:");
        lblSearchName.setFont(thaiFont);
        pnlTopBar.add(lblSearchName);
        
        txtSearchByName = new JTextField(20); 
        txtSearchByName.setFont(thaiFont);
        pnlTopBar.add(txtSearchByName);

        // --- (*** แก้ไข KeyListener ***) ---
        txtSearchByName.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                updateFilters(); // <--- เรียกเมธอดใหม่
            }
        });
        // --- (*** สิ้นสุดการแก้ไข ***) ---

        
        // 2. ค้นหาด้วยบาร์โค้ด (ปุ่ม)
        JLabel lblScan = new JLabel("ค้นหาสินค้าด้วยบาร์โค้ด:");
        lblScan.setFont(thaiFont);
        pnlTopBar.add(lblScan);
        
        btnScanBarcode = new JButton("สแกนบาร์โค้ด"); 
        btnScanBarcode.setFont(thaiFont);
        pnlTopBar.add(btnScanBarcode);
        
        btnScanBarcode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(ProductManagementPanel.this);
                BarcodeScannerDialog dialog = new BarcodeScannerDialog(owner, ProductManagementPanel.this);
                dialog.setVisible(true);
            }
        });

        // 3. หมวดหมู่
        JLabel lblCategory = new JLabel("หมวดหมู่:");
        lblCategory.setFont(thaiFont);
        pnlTopBar.add(lblCategory);
        
        cmbCategory = new JComboBox<>(); 
        cmbCategory.addItem("เลือกหมวดหมู่"); 
        cmbCategory.setFont(thaiFont);
        pnlTopBar.add(cmbCategory);

        // --- (*** เพิ่มโค้ดส่วนนี้ (Listener ของ ComboBox) ***) ---
        cmbCategory.addItemListener(new java.awt.event.ItemListener() {
            @Override
            public void itemStateChanged(java.awt.event.ItemEvent e) {
                // ตรวจสอบว่าเป็นการ "เลือก" (ไม่ใช่ "เอาออก")
                if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                    updateFilters(); // <--- เรียกเมธอดใหม่
                }
            }
        });
        // --- (*** สิ้นสุดส่วนที่เพิ่ม ***) ---
    }
    
    /**
     * สร้าง Panel ด้านล่าง (SOUTH) สำหรับปุ่ม
     * (*** ฉบับแก้ไข - ลบปุ่ม "เลือก" ออก ***)
     */
    private void createBottomBar() {
        pnlBottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT)); 
        Font thaiFont = new Font("Tahoma", Font.BOLD, 14);

        btnAdd = new JButton("เพิ่มสินค้าใหม่");
        btnAdd.setFont(thaiFont);
        btnAdd.setBackground(new Color(46, 204, 113)); // เขียว
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setOpaque(true);
        btnAdd.setBorderPainted(false);
        
        btnDelete = new JButton("ลบ");
        btnDelete.setFont(thaiFont);
        btnDelete.setBackground(new Color(231, 76, 60)); // แดง
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setOpaque(true);
        btnDelete.setBorderPainted(false);
        
        btnEdit = new JButton("แก้ไข");
        btnEdit.setFont(thaiFont);
        btnEdit.setBackground(new Color(241, 196, 15)); // เหลือง
        btnEdit.setOpaque(true);
        btnEdit.setBorderPainted(false);
        
        pnlBottomBar.add(btnAdd);
        pnlBottomBar.add(btnDelete);
        pnlBottomBar.add(btnEdit);
        
        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1. หาหน้าต่างหลัก (MainMenuPage)
                JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(ProductManagementPanel.this);
                
                // 2. สร้างและแสดง Dialog
                // (ส่ง this ซึ่งก็คือ ProductManagementPanel ไปด้วย)
                AddProductDialog dialog = new AddProductDialog(owner, ProductManagementPanel.this);
                dialog.setVisible(true);
            }
        });
        
        btnDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // เรียกเมธอดสำหรับลบสินค้า
                deleteSelectedProduct();
            }
        });
        
        btnEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1. ค้นหาแถวที่ถูกเลือก
                int selectedRow = getSelectedRowIndex();
                if (selectedRow == -1) {
                    return; // (กันเหนียว)
                }

                // 2. ดึง "รหัสสินค้า" (product_id) จากคอลัมน์ที่ 0
                String productIdToEdit = (String) tableModel.getValueAt(selectedRow, 0);

                // 3. หาหน้าต่างหลัก (MainMenuPage)
                JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(ProductManagementPanel.this);
                
                // 4. สร้างและแสดง Edit Dialog
                EditProductDialog dialog = new EditProductDialog(owner, ProductManagementPanel.this, productIdToEdit);
                dialog.setVisible(true);
            }
        });
        
        // --- ลบส่วนของปุ่ม "เลือก" (btnSelect) ทิ้งไป ---
    }

    /**
     * สร้างตารางและ Model (CENTER)
     * (*** ฉบับแก้ไข - เพิ่มคอลัมน์ Checkbox ***)
     */
    private void createProductTable() {
        // (แก้ไข) เพิ่ม "เลือก" เป็นคอลัมน์สุดท้าย
        String[] columnNames = {
            "รหัสสินค้า", "ชื่อสินค้า", "หมวดหมู่", "ราคาซื้อ", "ราคาขาย", "สต็อก", "เลือก"
        };
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            
            // (ใหม่) บอก JTable ว่าคอลัมน์สุดท้าย (index 6) เป็น Boolean (Checkbox)
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 6) {
                    return Boolean.class;
                }
                return String.class;
            }

            // (แก้ไข) ทำให้ช่องอื่นแก้ไขไม่ได้ *ยกเว้น* Checkbox
            @Override
            public boolean isCellEditable(int row, int column) {
               return column == 6; // แก้ไขได้เฉพาะคอลัมน์ "เลือก" (index 6)
            }
        };

        productTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        productTable.setRowSorter(sorter);
        productTable.setFont(new Font("Tahoma", Font.PLAIN, 14));
        productTable.setRowHeight(25);
        productTable.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 14));
        
        // (ใหม่) กำหนดความกว้างของคอลัมน์ Checkbox ให้เล็กๆ
        productTable.getColumnModel().getColumn(6).setPreferredWidth(40);
        productTable.getColumnModel().getColumn(6).setMaxWidth(50);
        
        tableScrollPane = new JScrollPane(productTable);
    }

    /**
     * (ใหม่) เพิ่ม Listener ตรวจจับการเปลี่ยนแปลงในตาราง (การติ๊ก Checkbox)
     */
    private void addTableListener() {
        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                // ถ้ากำลังอัปเดตเอง (กัน Loop) ให้ออกจากฟังก์ชัน
                if (isUpdatingCheckbox) return;

                int row = e.getFirstRow();
                int col = e.getColumn();

                // ตรวจสอบว่ามีการเปลี่ยนแปลงที่คอลัมน์ "เลือก" (index 6)
                if (e.getType() == TableModelEvent.UPDATE && col == 6) {
                    
                    // 1. เอาค่า Boolean (true/false) ที่เพิ่งถูกติ๊ก
                    Boolean isChecked = (Boolean) tableModel.getValueAt(row, col);

                    if (isChecked) {
                        // ถ้าแถวนี้ถูกติ๊ก (true)
                        isUpdatingCheckbox = true; // --- เริ่มการอัปเดต

                        // 2. วนลูปแถวอื่นๆ ทั้งหมด
                        for (int i = 0; i < tableModel.getRowCount(); i++) {
                            if (i != row) {
                                // 3. สั่งให้ Checkbox แถวอื่นเป็น false (ติ๊กออก)
                                tableModel.setValueAt(false, i, 6);
                            }
                        }
                        
                        isUpdatingCheckbox = false; // --- สิ้นสุดการอัปเดต
                        
                        // 4. เปิดปุ่ม
                        btnEdit.setEnabled(true);
                        btnDelete.setEnabled(true);
                    } else {
                        // ถ้าแถวนี้ถูก "ติ๊กออก" (false)
                        // ให้ปิดปุ่ม (ถ้าไม่มีแถวอื่นถูกเลือกอยู่)
                        btnEdit.setEnabled(false);
                        btnDelete.setEnabled(false);
                    }
                }
            }
        });
    }

    /**
     * (ใหม่) เมธอดสำหรับดึงข้อมูล "หมวดหมู่" มาใส่ใน ComboBox
     * (เหมือนเดิม)
     */
    private void loadCategoryData() {
        // (โค้ดส่วนนี้เหมือนเดิมครับ)
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT name FROM categories ORDER BY name ASC";

        try {
            conn = DbConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (cmbCategory.getItemCount() > 1) {
                cmbCategory.removeItemAt(1);
            }
            cmbCategory.addItem("ทั้งหมด");
            while (rs.next()) {
                cmbCategory.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
     * เมธอดสำหรับดึงข้อมูลสินค้าจาก DB มาใส่ในตาราง
     * (*** ฉบับแก้ไข - เพิ่ม 'false' สำหรับ Checkbox ***)
     */
 // ในไฟล์ ProductManagementPanel.java
    public void loadProductData() {
        tableModel.setRowCount(0);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT p.product_id, p.name, c.name AS category_name, " +
                     "p.cost_price, p.sale_price, p.stock " +
                     "FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.category_id " +
                     "ORDER BY p.name ASC";

        try {
            conn = DbConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            // (ดีบัก 1)
            System.out.println("--- เริ่มโหลดข้อมูลสินค้า ---");

            while (rs.next()) {
                // (ดีบัก 2)
                System.out.println("กำลังโหลด: " + rs.getString("product_id") + " - " + rs.getString("name"));

                // (สร้าง Vector)
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("product_id"));
                row.add(rs.getString("name"));
                row.add(rs.getString("category_name"));

                try { // (*** เริ่ม try-catch ***)
                    // (เพิ่มข้อมูลที่เหลือ)
                    row.add(rs.getDouble("cost_price"));
                    row.add(rs.getDouble("sale_price"));
                    row.add(rs.getInt("stock"));
                    row.add(false); // Checkbox

                    // (เพิ่มแถวลงตาราง)
                    tableModel.addRow(row);

                } catch (Exception e) { // (*** Catch Error ***)
                    System.err.println("!!! Error ตอนแปลงข้อมูลแถว: " + rs.getString("product_id") + " - " + e.getMessage());
                    // (ข้ามแถวนี้ไป ไม่ต้อง addRow)
                }
                // (*** ไม่มีโค้ด addRow ซ้ำตรงนี้ ***)
            } // (*** สิ้นสุด while loop ***)

            // (*** ดีบัก 3: ย้ายมาไว้ตรงนี้ ***)
            System.out.println("--- โหลดข้อมูลสินค้าเสร็จสิ้น ---");

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: ไม่สามารถโหลดข้อมูลสินค้าได้\n" + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
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
     * (ใหม่) เมธอดสำหรับค้นหาแถวที่ถูกเลือก (ติ๊ก Checkbox)
     * @return Index ของแถว (int) หรือ -1 ถ้าไม่มีการเลือก
     */
    private int getSelectedRowIndex() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean isChecked = (Boolean) tableModel.getValueAt(i, 6); // คอลัมน์ที่ 6 = เลือก
            if (isChecked) {
                return i; // คืนค่าแถวที่เจอ
            }
        }
        return -1; // ไม่พบแถวที่ถูกเลือก
    }

    /**
     * (ใหม่) เมธอดสำหรับลบสินค้าที่ถูกเลือก
     */
    private void deleteSelectedProduct() {
        // 1. ค้นหาแถวที่ถูกเลือก
        int selectedRow = getSelectedRowIndex();
        
        // (ป้องกัน) ถ้า selectedRow = -1 (คือไม่มีแถวไหนถูกเลือกเลย)
        if (selectedRow == -1) {
            // (ปกติปุ่มจะถูกปิดอยู่ แต่กันเหนียวไว้)
            JOptionPane.showMessageDialog(this, "กรุณาเลือกสินค้าที่ต้องการลบ", "ไม่ได้เลือก", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. ดึง "รหัสสินค้า" (product_id) จากคอลัมน์ที่ 0
        String productId = (String) tableModel.getValueAt(selectedRow, 0);
        String productName = (String) tableModel.getValueAt(selectedRow, 1);

        // 3. แสดงหน้าต่างยืนยัน
        int choice = JOptionPane.showConfirmDialog(
            this,
            "คุณแน่ใจหรือไม่ว่าต้องการลบสินค้า:\n" + productName + " (รหัส: " + productId + ")?",
            "ยืนยันการลบ",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        // 4. ถ้าผู้ใช้กด "YES" (choice == 0)
        if (choice == JOptionPane.YES_OPTION) {
            Connection conn = null;
            PreparedStatement pstmt = null;
            String sql = "DELETE FROM products WHERE product_id = ?";

            try {
                conn = DbConnection.getConnection();
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, productId);
                
                int rowsDeleted = pstmt.executeUpdate();
                if (rowsDeleted > 0) {
                    JOptionPane.showMessageDialog(this, "ลบสินค้า '" + productName + "' สำเร็จ");
                    
                    // (สำคัญ) โหลดตารางใหม่
                    loadProductData();
                    
                    // (สำคัญ) ปิดปุ่มกลับเป็นสีเทา
                    btnEdit.setEnabled(false);
                    btnDelete.setEnabled(false);
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                // (Error ถ้ามี sale_details อ้างอิงอยู่)
                if (e.getErrorCode() == 1451) { // 1451 = Constraint fail
                    JOptionPane.showMessageDialog(this, 
                        "ไม่สามารถลบสินค้านี้ได้ เนื่องจากมีประวัติการขายในระบบ", 
                        "การลบล้มเหลว", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "เกิดข้อผิดพลาดในการลบข้อมูล", "Database Error", JOptionPane.ERROR_MESSAGE);
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
    
    /**
     * (ใหม่) เมธอดสำหรับปิดปุ่ม (เรียกใช้จากภายนอก)
     */
    public void disableEditDeleteButtons() {
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);
    }
    
    /**
     * (ใหม่) ถูกเรียกโดย BarcodeScannerDialog
     * เพื่อค้นหาและไฮไลต์แถวในตาราง
     */
    public void selectProductById(String productId) {
        // 1. เคลียร์ช่องค้นหา (เผื่อกรองอยู่)
        sorter.setRowFilter(null);
        txtSearchByName.setText("");

        // 2. วนลูปหาแถวใน Model (ข้อมูลดิบ)
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String idInModel = (String) tableModel.getValueAt(i, 0); // คอลัมน์ 0 = รหัสสินค้า
            
            if (idInModel.equals(productId)) {
                // 3. ถ้าเจอ -> แปลง Model Index (i) เป็น View Index
                // (View Index คือแถวที่แสดงบนจอจริงๆ หลังจากการเรียงลำดับ)
                int viewIndex = productTable.convertRowIndexToView(i);
                
                if (viewIndex != -1) { // (กันเหนียว)
                    // 4. สั่งให้ตารางไฮไลต์แถวนั้น
                    productTable.setRowSelectionInterval(viewIndex, viewIndex);
                    
                    // 5. (สำคัญ) สั่งให้เลื่อนจอ (Scroll) ไปหาแถวที่ไฮไลต์
                    productTable.scrollRectToVisible(productTable.getCellRect(viewIndex, 0, true));
                }
                return; // ออกจากฟังก์ชัน
            }}
        }
        
        /**
         * (ใหม่) เมธอดสำหรับกรองตาราง (รวมทั้ง 2 ช่อง)
         * จะถูกเรียกใช้เมื่อมีการพิมพ์ หรือมีการเลือก ComboBox
         */
        private void updateFilters() {
            // 1. สร้าง List สำหรับเก็บ Filter
            List<RowFilter<Object, Object>> filters = new ArrayList<>();

            // 2. กรองจากช่อง "ค้นหาด้วยชื่อ"
            String searchText = txtSearchByName.getText().trim();
            if (!searchText.isEmpty()) {
                // กรองคอลัมน์ "ชื่อสินค้า" (index 1)
                // "(?i)" = ไม่สนใจตัวพิมพ์เล็ก/ใหญ่
                filters.add(RowFilter.regexFilter("(?i)" + searchText, 1));
            }

         // 3. กรองจาก "หมวดหมู่"
            String selectedCategory = (String) cmbCategory.getSelectedItem();

            // (*** เพิ่ม Debug Output ***)
            System.out.println("ComboBox selected: [" + selectedCategory + "]"); // พิมพ์ค่าที่เลือก

            if (selectedCategory != null &&
                !selectedCategory.equals("ทั้งหมด") &&
                !selectedCategory.equals("เลือกหมวดหมู่")) {

                // (*** เพิ่ม Debug Output ***)
                // ลองพิมพ์ค่าในตารางออกมาดู (เฉพาะแถวแรกที่ควรจะเจอ)
                boolean foundMatch = false;
                for(int i=0; i<tableModel.getRowCount(); i++){
                    String categoryInTable = (String) tableModel.getValueAt(i, 2); // คอลัมน์ 2 = หมวดหมู่
                    if(i < 5){ // พิมพ์แค่ 5 แถวแรกพอ
                         System.out.println("  - Table row " + i + " category: [" + categoryInTable + "]");
                    }
                    if(categoryInTable != null && categoryInTable.equals(selectedCategory)){
                        foundMatch = true;
                        // (*** เพิ่มเติม: ถ้าเจอแล้ว ออก loop ได้เลย ***)
                        break; 
                    }
                }
                System.out.println("  - Exact match found in table? " + foundMatch);
                // --- สิ้นสุด Debug Output ---


                // กรองคอลัมน์ "หมวดหมู่" (index 2)
                // "^" + ... + "$" = ต้องตรงกันเป๊ะๆ ทั้งคำ
             // (*** แก้ไข ***) ใช้ Pattern.quote() เพื่อ Escape ตัวอักษรพิเศษ
                filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(selectedCategory), 2));
            }

            // 4. รวม Filter ทั้งหมด
            if (filters.isEmpty()) {
                sorter.setRowFilter(null); // ไม่มี filter
            } else {
                // ใช้ andFilter เพื่อให้ต้อง "ตรง" กับทุกเงื่อนไข
                sorter.setRowFilter(RowFilter.andFilter(filters)); 
            }
        
    }
}