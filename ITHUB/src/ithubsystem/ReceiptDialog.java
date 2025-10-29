package ithubsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component; // (*** เพิ่ม import ***)
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class ReceiptDialog extends JDialog {

    private JTable itemsTable;
    private DefaultTableModel itemsTableModel;
    private JButton btnClose;

    private DecimalFormat df = new DecimalFormat("#,##0.00");
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public ReceiptDialog(JFrame owner, int saleId, String cashierUsername,
                         DefaultTableModel cartTableModel, double totalAmount,
                         double amountReceived, double change) {
        super(owner, "ใบเสร็จรับเงิน", true);
        setSize(400, 550);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 5)); // ลด VGap เล็กน้อย
        getRootPane().setBorder(new EmptyBorder(10, 15, 10, 15)); // เพิ่มขอบซ้ายขวา

        Font headerFont = new Font("Tahoma", Font.BOLD, 14);
        Font normalFont = new Font("Tahoma", Font.PLAIN, 12);
        Font tableFont = new Font("Tahoma", Font.PLAIN, 12);
        Font footerTotalFont = new Font("Tahoma", Font.BOLD, 13);

        // --- 1. Header Info (NORTH) ---
        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS)); // เรียงแนวตั้ง

        JLabel lblShopName = new JLabel("ร้าน IT HUB");
        lblShopName.setFont(headerFont);
        lblShopName.setAlignmentX(Component.CENTER_ALIGNMENT); // จัดกลาง

        JLabel lblInvoiceTitle = new JLabel("ใบเสร็จรับเงิน/Tax Invoice (ABB)");
        lblInvoiceTitle.setFont(normalFont);
        lblInvoiceTitle.setAlignmentX(Component.CENTER_ALIGNMENT); // จัดกลาง

        JLabel lblDivider1 = new JLabel("----------------------------------------");
        lblDivider1.setFont(normalFont);
        lblDivider1.setAlignmentX(Component.CENTER_ALIGNMENT); // จัดกลาง

        JLabel lblBillId = new JLabel("เลขที่บิล: " + saleId);
        lblBillId.setFont(normalFont);
        lblBillId.setAlignmentX(Component.LEFT_ALIGNMENT); // ชิดซ้าย

        JLabel lblDate = new JLabel("วันที่: " + dateTimeFormat.format(new Date()));
        lblDate.setFont(normalFont);
        lblDate.setAlignmentX(Component.LEFT_ALIGNMENT); // ชิดซ้าย

        JLabel lblCashier = new JLabel("พนักงาน: " + cashierUsername);
        lblCashier.setFont(normalFont);
        lblCashier.setAlignmentX(Component.LEFT_ALIGNMENT); // ชิดซ้าย

        // เพิ่ม Label ลง Panel Header พร้อมช่องว่าง
        pnlHeader.add(lblShopName);
        pnlHeader.add(lblInvoiceTitle);
        pnlHeader.add(Box.createRigidArea(new Dimension(0, 5)));
        pnlHeader.add(lblDivider1);
        pnlHeader.add(Box.createRigidArea(new Dimension(0, 5)));
        pnlHeader.add(lblBillId);
        pnlHeader.add(lblDate);
        pnlHeader.add(lblCashier);
        pnlHeader.add(Box.createRigidArea(new Dimension(0, 5))); // ช่องว่างล่างก่อนเส้นใต้
        pnlHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY)); // เส้นใต้ Header

        add(pnlHeader, BorderLayout.NORTH);

        // --- 2. Items Table (CENTER) ---
        // (โค้ดส่วนนี้เหมือนเดิม)
        String[] headers = {"สินค้า", "จำนวน", "ราคา", "รวม"};
        itemsTableModel = new DefaultTableModel(headers, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        itemsTable = new JTable(itemsTableModel);
        itemsTable.setFont(tableFont);
        itemsTable.setRowHeight(20);
        itemsTable.getTableHeader().setFont(headerFont);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer(); centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer(); rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        itemsTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); itemsTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        itemsTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer); itemsTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        itemsTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer); itemsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        populateItemsTable(cartTableModel);
        JScrollPane scrollPane = new JScrollPane(itemsTable);
        int rowCount = itemsTableModel.getRowCount();
        int prefHeight = (rowCount + 1) * itemsTable.getRowHeight();
        scrollPane.setPreferredSize(new Dimension(350, Math.min(prefHeight + 5, 300)));
        // (*** เพิ่ม: ขอบซ้ายขวาให้ตาราง ***)
        scrollPane.setBorder(new EmptyBorder(0, 5, 0, 5));
        add(scrollPane, BorderLayout.CENTER);

        // --- 3. Footer Info & Button (SOUTH) ---
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY)); // เส้นเหนือ Footer

        JLabel lblDivider2 = new JLabel("----------------------------------------");
        lblDivider2.setFont(normalFont);
        lblDivider2.setAlignmentX(Component.CENTER_ALIGNMENT);

        // (*** แก้ไข: ใช้ JPanel + BorderLayout ช่วยจัดชิดซ้าย ***)
        JPanel totalPanel = createLeftRightLabel("ยอดรวมสุทธิ:", df.format(totalAmount), footerTotalFont, normalFont);
        JPanel receivedPanel = createLeftRightLabel("รับเงิน:", df.format(amountReceived), normalFont, normalFont);
        JPanel changePanel = createLeftRightLabel("เงินทอน:", df.format(change), footerTotalFont, footerTotalFont); // เงินทอนตัวหนา

        JLabel lblDivider3 = new JLabel("----------------------------------------");
        lblDivider3.setFont(normalFont);
        lblDivider3.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblThankYou = new JLabel("ขอบคุณที่ใช้บริการ");
        lblThankYou.setFont(normalFont);
        lblThankYou.setAlignmentX(Component.CENTER_ALIGNMENT); // จัดกลาง

        // เพิ่ม Label/Panel ลง Panel Footer
        southPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        southPanel.add(lblDivider2);
        southPanel.add(totalPanel); // เพิ่ม Panel ที่จัดแล้ว
        southPanel.add(receivedPanel); // เพิ่ม Panel ที่จัดแล้ว
        southPanel.add(changePanel); // เพิ่ม Panel ที่จัดแล้ว
        southPanel.add(lblDivider3);
        southPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        southPanel.add(lblThankYou);
        southPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Button Panel (แยกส่วน)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnClose = new JButton("ปิด");
        btnClose.setFont(new Font("Tahoma", Font.PLAIN, 14));
        btnClose.addActionListener(e -> dispose());
        buttonPanel.add(btnClose);

        // เพิ่ม Footer Panel และ Button Panel ลง South
        JPanel finalSouthPanel = new JPanel(new BorderLayout());
        finalSouthPanel.add(southPanel, BorderLayout.CENTER);
        finalSouthPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(finalSouthPanel, BorderLayout.SOUTH);
    }

    /** (*** ใหม่: Helper สร้างแถว Label ชิดซ้าย-ขวา ***) */
    private JPanel createLeftRightLabel(String leftText, String rightText, Font leftFont, Font rightFont) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel leftLabel = new JLabel(leftText);
        leftLabel.setFont(leftFont);
        JLabel rightLabel = new JLabel(rightText);
        rightLabel.setFont(rightFont);
        rightLabel.setHorizontalAlignment(SwingConstants.RIGHT); // ชิดขวา
        panel.add(leftLabel, BorderLayout.WEST); // ชิดซ้าย
        panel.add(rightLabel, BorderLayout.EAST); // ชิดขวา
        // (*** เพิ่ม: ทำให้ Panel โปร่งใส ***)
        panel.setOpaque(false);
        // (*** เพิ่ม: จัด Panel ให้ชิดซ้าย ***)
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }


    /** เติมข้อมูลลง JTable (เหมือนเดิม) */
    private void populateItemsTable(DefaultTableModel cartTableModel) {
        itemsTableModel.setRowCount(0);
        for (int i = 0; i < cartTableModel.getRowCount(); i++) {
            String name = (String) cartTableModel.getValueAt(i, 1);
            int quantity = Integer.parseInt(cartTableModel.getValueAt(i, 2).toString());
            double price = (Double) cartTableModel.getValueAt(i, 3);
            double lineTotal = quantity * price;

            Vector<Object> row = new Vector<>();
            row.add(name);
            row.add(quantity);
            row.add(df.format(price));
            row.add(df.format(lineTotal));
            itemsTableModel.addRow(row);
        }
    }
}