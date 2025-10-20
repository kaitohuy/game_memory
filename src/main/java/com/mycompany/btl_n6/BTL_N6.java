/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.btl_n6;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
/**
 *
 * @author Hi
 */
public class BTL_N6 {

    public static void main(String[] args) {
        // Khởi động Server trước
        System.out.println("Khởi động Server...");
        Thread serverThread = new Thread(() -> {
            try {
                com.mycompany.btl_n6.Server.MainServer.main(args); // Chạy Server
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        // Đợi Server khởi động (thêm delay ngắn để đảm bảo Server sẵn sàng)
        try {
            Thread.sleep(2000); // Đợi 2 giây
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Khởi động Client
        System.out.println("Khởi động Client...");
        com.mycompany.btl_n6.Client.MainClient.main(args); // Chạy Client
    }
}
