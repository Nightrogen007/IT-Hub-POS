package ithubsystem;

import javax.swing.*;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginPage extends JFrame {

    // 1. ประกาศตัวแปร UI Components
    private JLabel lblTitle, lblUsername, lblPassword;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JLabel lblStatus;

    public LoginPage() {
        // 2. ตั้งค่าหน้าต่าง (JFrame)
        setTitle("IT HUB - Login System");
        setSize(400, 350); // ขยายความสูงเล็กน้อย
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        setLayout(null); 
        
        // --- ปรับปรุงหน้าตา UI ตาม Mockup ---
        //////

        // หัวข้อ "IT HUB"
        lblTitle = new JLabel("IT HUB");
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 30));
        lblTitle.setBounds(0, 20, 400, 40); // x, y, width, height
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER); // จัดกลาง
        add(lblTitle);

        // Username
        lblUsername = new JLabel("USERNAME");
        lblUsername.setFont(new Font("Tahoma", Font.PLAIN, 12));
        lblUsername.setBounds(80, 80, 100, 25);
        add(lblUsername);

        txtUsername = new JTextField();
        txtUsername.setFont(new Font("Tahoma", Font.PLAIN, 14));
        txtUsername.setBounds(80, 105, 220, 30); // ปรับขนาดให้ใหญ่ขึ้น
        add(txtUsername);

        // Password
        lblPassword = new JLabel("PASS");
        lblPassword.setFont(new Font("Tahoma", Font.PLAIN, 12));
        lblPassword.setBounds(80, 145, 100, 25);
        add(lblPassword);

        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Tahoma", Font.PLAIN, 14));
        txtPassword.setBounds(80, 170, 220, 30); // ปรับขนาดให้ใหญ่ขึ้น
        add(txtPassword);

        // Login Button
        btnLogin = new JButton("LOG-IN");
        btnLogin.setFont(new Font("Tahoma", Font.BOLD, 14));
        btnLogin.setBounds(140, 220, 100, 30);
        add(btnLogin);
        
        // Status Label (แสดงข้อความ error)
        lblStatus = new JLabel("");
        lblStatus.setForeground(Color.RED); 
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        lblStatus.setBounds(80, 260, 220, 25);
        add(lblStatus);

        // --- สิ้นสุดการปรับปรุง UI ---
        
        // 4. เพิ่ม Event Listener ให้ปุ่ม Login (เหมือนเดิม)
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = txtUsername.getText();
                String password = new String(txtPassword.getPassword()); 
                validateLogin(username, password);
            }
        });
    }

    /**
     * เมธอดสำหรับตรวจสอบ Username/Password กับฐานข้อมูล
     * (ส่วนนี้เหมือนเดิม 100% ไม่ต้องแก้ไข)
     */
    private void validateLogin(String username, String password) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        String sql = "SELECT role FROM users WHERE username = ? AND password = ?";

        try {
            conn = DbConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                // --- Login สำเร็จ ---
                String role = rs.getString("role");
                
                // 1. เปิดหน้าหลัก (Main Menu)
                new MainMenuPage(username, role).setVisible(true);
                
                // 2. ปิดหน้า Login นี้
                this.dispose(); 
                
            } else {
                // --- Login ไม่สำเร็จ ---
                lblStatus.setText("Username หรือ Password ไม่ถูกต้อง!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            lblStatus.setText("Error: ไม่สามารถเชื่อมต่อฐานข้อมูลได้");
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
     * เมธอด Main สำหรับรันโปรแกรม (เหมือนเดิม)
     */
    public static void main(String[] args) {
    	
    	Font thaiFont = new Font("Tahoma", Font.PLAIN, 14);
        UIManager.put("OptionPane.messageFont", thaiFont);
        UIManager.put("OptionPane.buttonFont", thaiFont);
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginPage().setVisible(true);
            }
        });
    }
}
