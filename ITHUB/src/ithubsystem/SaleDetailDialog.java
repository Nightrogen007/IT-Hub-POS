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

// (เราลืม Import 2 ตัวนี้ใน ReportPanel รบกวนคุณช่วยเพิ่ม
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
// ด้วยนะครับ)

public class SaleDetailDialog extends JDialog {

    // Components
    private JLabel lblHeader;
    private JTable detailTable;
    private DefaultTableModel detailTableModel;
    private JLabel lblTotal;
    
    private int saleId; // ID บิลที่รับมา
    private String saleDate; // วันที่ (รับมา)
    private double totalAmount; // ยอดรวม (รับมา)
    
    private DecimalFormat df = new DecimalFormat("#,##0.00");

    /**
     * Constructor
     * @param owner หน้าต่างแม่
     * @param saleId ID บิลที่จะดู
     * @param saleDate วันที่ (ข้อความ)
     * @param totalAmount ยอดรวม
     */
    public SaleDetailDialog(JFrame owner, int saleId, String saleDate, double totalAmount) {
        super(owner, "รายละเอียดบิล", true); // Modal
        this.saleId = saleId;
        this.saleDate = saleDate;
        this.totalAmount = totalAmount;

        setSize(600, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        
        Font thaiFont = new Font("Tahoma", Font.PLAIN, 14);
        Font boldFont = new Font("Tahoma", Font.BOLD, 16);

        // --- 1. Panel ส่วนหัว (NORTH) ---
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        lblHeader = new JLabel("รายละเอียดบิลเลขที่: " + saleId + " | วันที่: " + saleDate);
        lblHeader.setFont(boldFont);
        pnlHeader.add(lblHeader, BorderLayout.WEST);
        
        add(pnlHeader, BorderLayout.NORTH);

        // --- 2. ตาราง (CENTER) ---
        String[] headers = {"รายการที่", "ชื่อสินค้า", "จำนวน", "ราคา/หน่วย", "ยอดรวม"};
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

        // --- 3. Panel สรุป (SOUTH) ---
        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        lblTotal = new JLabel("รวมเป็นเงินสุทธิ: " + df.format(totalAmount) + " บาท");
        lblTotal.setFont(boldFont);
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlFooter.add(lblTotal, BorderLayout.EAST);
        
        add(pnlFooter, BorderLayout.SOUTH);

        // --- 4. โหลดข้อมูล ---
        loadSaleDetails();
    }

    /**
     * (สำคัญ) ดึงข้อมูลรายละเอียดบิลจาก DB
     */
    private void loadSaleDetails() {
        detailTableModel.setRowCount(0);
        String sql = "SELECT p.name, sd.quantity, sd.price_per_unit " +
                     "FROM sale_details sd " +
                     "JOIN products p ON sd.product_id = p.product_id " +
                     "WHERE sd.sale_id = ? " +
                     "ORDER BY p.name ASC";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, this.saleId);
            ResultSet rs = pstmt.executeQuery();
            
            int itemNumber = 1;
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                String name = rs.getString("name");
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("price_per_unit");
                double lineTotal = quantity * price;

                row.add(itemNumber++);
                row.add(name);
                row.add(quantity);
                row.add(df.format(price));
                row.add(df.format(lineTotal));
                
                detailTableModel.addRow(row);
            }
            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "ไม่สามารถโหลดรายละเอียดบิลได้", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}