/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.btl_n6.Server;

/**
 *
 * @author Hi
 */
public class MainServer {
    public static void main(String[] args) {
        // Tạo server và bắt đầu lắng nghe kết nối
        Server server = new Server(12345); // Port 12345
        server.start();
    }
}
