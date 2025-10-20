package com.mycompany.btl_n6.Client;

import com.mycompany.btl_n6.Client.controllers.LoginController;
import com.mycompany.btl_n6.Client.manager.ClientSocketManager;
import com.mycompany.btl_n6.Client.views.LoginView;

public class MainClient {
    public static void main(String[] args) {
        try {
            // Khởi tạo client socket và kết nối đến server
            ClientSocket clientSocket = new ClientSocket("localhost", 12345);

            // Tạo một ClientSocketManager mới cho mỗi instance của MainClient
            ClientSocketManager clientSocketManager = new ClientSocketManager(clientSocket);
            clientSocketManager.startReceivingMessages();

            // Khởi tạo giao diện đăng nhập
            LoginView loginView = new LoginView();
            new LoginController(loginView, clientSocketManager, clientSocket);

            // Hiển thị giao diện đăng nhập
            loginView.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
