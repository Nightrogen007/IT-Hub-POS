package ithubsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
        setLayout(new BorderLayout(0, 5));
        getRootPane().setBorder(new EmptyBorder(10, 15, 10, 15));

        Font headerFont = new Font("Tahoma", Font.BOLD, 14);
        Font normalFont = new Font("Tahoma", Font.PLAIN, 12);
        Font footerFontBold = new Font("Tahoma", Font.BOLD, 13);

        // ---------------- HEADER ----------------
        JPanel pnlHeader = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 0);

        JLabel lblShopName = new JLabel("ร้าน IT HUB", SwingConstants.CENTER);
        lblShopName.setFont(headerFont);
        lblShopName.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblInvoice = new JLabel("ใบเสร็จรับเงิน/Tax Invoice (ABB)", SwingConstants.CENTER);
        lblInvoice.setFont(normalFont);

        JLabel lblLine1 = new JLabel("----------------------------------------");
        JLabel lblBillId = new JLabel("เลขที่บิล: " + saleId);
        JLabel lblDate = new JLabel("วันที่: " + dateTimeFormat.format(new Date()));
        JLabel lblCashier = new JLabel("พนักงาน: " + cashierUsername);

        JPanel headerCenter = new JPanel();
        headerCenter.setLayout(new BoxLayout(headerCenter, BoxLayout.Y_AXIS));
        headerCenter.add(lblShopName);
        headerCenter.add(lblInvoice);
        headerCenter.add(lblLine1);
        headerCenter.add(lblBillId);
        headerCenter.add(lblDate);
        headerCenter.add(lblCashier);

        pnlHeader.add(headerCenter, gbc);
        pnlHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        add(pnlHeader, BorderLayout.NORTH);

        // ---------------- TABLE ----------------
        String[] headers = {"สินค้า", "จำนวน", "ราคา", "รวม"};
        itemsTableModel = new DefaultTableModel(headers, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        itemsTable = new JTable(itemsTableModel);
        itemsTable.setFont(normalFont);
        itemsTable.setRowHeight(20);
        itemsTable.getTableHeader().setFont(headerFont);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        itemsTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        itemsTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        itemsTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

        populateItemsTable(cartTableModel);

        JScrollPane scrollPane = new JScrollPane(itemsTable);
        add(scrollPane, BorderLayout.CENTER);

        // ---------------- FOOTER ----------------
        JPanel pnlFooter = new JPanel(new GridBagLayout());
        GridBagConstraints fbc = new GridBagConstraints();
        fbc.gridx = 0;
        fbc.anchor = GridBagConstraints.WEST;
        fbc.insets = new Insets(2, 0, 2, 0);

        JLabel lblLine2 = new JLabel("----------------------------------------");
        JLabel lblTotal = new JLabel("ยอดรวมสุทธิ: " + df.format(totalAmount));
        lblTotal.setFont(footerFontBold);

        JLabel lblReceive = new JLabel("รับเงิน: " + df.format(amountReceived));
        JLabel lblChange = new JLabel("เงินทอน: " + df.format(change));
        lblChange.setFont(footerFontBold);

        JLabel lblLine3 = new JLabel("----------------------------------------");
        JLabel lblThanks = new JLabel("ขอบคุณที่ใช้บริการ", SwingConstants.CENTER);
        lblThanks.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlFooter.add(lblLine2, fbc);
        pnlFooter.add(lblTotal, fbc);
        pnlFooter.add(lblReceive, fbc);
        pnlFooter.add(lblChange, fbc);
        pnlFooter.add(lblLine3, fbc);
        pnlFooter.add(Box.createVerticalStrut(5), fbc);
        pnlFooter.add(lblThanks, fbc);

        JPanel pnlBottom = new JPanel(new BorderLayout());
        pnlBottom.add(pnlFooter, BorderLayout.CENTER);

        // ปุ่มปิดอยู่กลาง
        JPanel pnlButton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnClose = new JButton("ปิด");
        btnClose.setFont(new Font("Tahoma", Font.PLAIN, 14));
        btnClose.addActionListener(e -> dispose());
        pnlButton.add(btnClose);

        pnlBottom.add(pnlButton, BorderLayout.SOUTH);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    private void populateItemsTable(DefaultTableModel cartTableModel) {
        itemsTableModel.setRowCount(0);
        for (int i = 0; i < cartTableModel.getRowCount(); i++) {
            String name = (String) cartTableModel.getValueAt(i, 1);
            int quantity = Integer.parseInt(cartTableModel.getValueAt(i, 2).toString());
            double price = (Double) cartTableModel.getValueAt(i, 3);
            double total = quantity * price;

            Vector<Object> row = new Vector<>();
            row.add(name);
            row.add(quantity);
            row.add(df.format(price));
            row.add(df.format(total));
            itemsTableModel.addRow(row);
        }
    }
}
