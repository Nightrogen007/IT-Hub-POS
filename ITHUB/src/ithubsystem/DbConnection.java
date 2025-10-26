package ithubsystem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {

    // --- แก้ไขข้อมูลเชื่อมต่อ XAMPP ของคุณตรงนี้ ---
    
    // ฐานข้อมูลที่เราสร้างไว้
    private static final String DB_NAME = "it_hub_db"; 
    
    // URL ของ JDBC
    private static final String URL = "jdbc:mysql://localhost:3307/" + DB_NAME;
    
    // Username ของ XAMPP (ปกติคือ root)
    private static final String USER = "root"; 
    
    // Password ของ XAMPP (ปกติคือเว้นว่างไว้)
    private static final String PASSWORD = ""; 
    
    // Driver Class
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    // --- สิ้นสุดส่วนแก้ไข ---

    /**
     * เมธอดสำหรับเชื่อมต่อฐานข้อมูล
     * @return Connection object
     */
    public static Connection getConnection() {
        Connection connection = null;
        try {
            // 1. โหลด Driver
            Class.forName(DRIVER_CLASS);
            
            // 2. สร้างการเชื่อมต่อ
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            
            // System.out.println("เชื่อมต่อฐานข้อมูลสำเร็จ!"); // (เอาไว้ทดสอบ)
            
        } catch (ClassNotFoundException e) {
            System.err.println("ไม่พบ MySQL JDBC Driver!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("การเชื่อมต่อฐานข้อมูลล้มเหลว!");
            e.printStackTrace();
        }
        return connection;
    }

    // (ทดสอบ) ลองรันไฟล์นี้ดู ถ้าขึ้น "เชื่อมต่อฐานข้อมูลสำเร็จ!" แปลว่าผ่าน
    // public static void main(String[] args) {
    //     Connection conn = DbConnection.getConnection();
    //     if(conn != null) {
    //         System.out.println("เชื่อมต่อฐานข้อมูลสำเร็จ!");
    //     }
    // }
}
