/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.btl_n6.Client.controllers;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.mycompany.btl_n6.Client.ClientSocket;
import com.mycompany.btl_n6.Client.manager.ClientSocketManager;
import com.mycompany.btl_n6.Client.manager.MessageListener;
import com.mycompany.btl_n6.Client.sessions.UserSession;
import com.mycompany.btl_n6.Client.views.HomeView;
import com.mycompany.btl_n6.Client.views.LoginView;
import com.mycompany.btl_n6.Server.model.User;

/**
 *
 * @author Hi
 */
public class LoginController implements MessageListener {
    private final LoginView loginView;
    private final ClientSocket clientSocket;
    private final ClientSocketManager clientSocketManager;

    public LoginController(LoginView loginView, ClientSocketManager clientSocketManager, ClientSocket clientSocket) {
        this.loginView = loginView;
        this.clientSocketManager = clientSocketManager;
        this.clientSocket = clientSocket;

        // Đăng ký controller này lắng nghe tin nhắn từ server
        this.clientSocketManager.addMessageListener(this);

        // Sự kiện khi bấm nút Login
        this.loginView.getLoginButton().addActionListener(e -> login());
    // Sự kiện khi bấm nút Register
    this.loginView.getRegisterButton().addActionListener(e -> register());

        // Sự kiện khi đóng cửa sổ
        this.loginView.addWindowCloseListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                closeClientSocket();
                System.exit(0);
            }
        });
    }

    @Override
    public void onMessageReceived(String message) {
        if (message == null) return;
        if (message.startsWith("REGISTER:")) {
            String[] parts = message.split(":", 2);
            String status = parts.length > 1 ? parts[1] : "";
            if ("SUCCESS".equals(status)) {
                JOptionPane.showMessageDialog(loginView, "Đăng ký thành công. Bạn có thể đăng nhập ngay.");
            } else {
                String reason = parts.length > 1 ? parts[1] : "";
                JOptionPane.showMessageDialog(loginView, "Đăng ký thất bại: " + reason, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        if (message.startsWith("LOGIN:")) {
            String[] parts = message.split(":", 3); 
            // parts[0] = LOGIN
            // parts[1] = SUCCESS / FAILURE
            // parts[2] = data (nếu thành công)

            String status = parts[1];

            if ("SUCCESS".equals(status)) {
                // format: LOGIN:SUCCESS:userId,username,avatar
                String[] data = parts[2].split(",");
                int userId = Integer.parseInt(data[0]);
                String username = data[1];
                int totalScore = Integer.parseInt(data[2]);
                int totalWins = Integer.parseInt(data[3]);
                String statusUser = data[4];
                User user = new User(userId, username, "",totalScore , totalWins, statusUser);
                UserSession.createSession(user);

                // chuyển sang HomeView
                showHomeView(username, totalScore, statusUser);

            } else if ("FAILURE".equals(status)) {
                JOptionPane.showMessageDialog(loginView, "Sai tài khoản hoặc mật khẩu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void login() {
        String username = loginView.getUsername();
        String password = loginView.getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(loginView, "Tên đăng nhập hoặc mật khẩu đang trống!");
            return;
        }

        try {
            clientSocket.sendMessage("LOGIN:" + username + "," + password);
        } catch (IOException ex) {
            Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void register() {
        String username = loginView.getUsername();
        String password = loginView.getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(loginView, "Tên đăng nhập hoặc mật khẩu đang trống!");
            return;
        }

        try {
            clientSocket.sendMessage("REGISTER:" + username + "," + password);
        } catch (IOException ex) {
            Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void showHomeView(String username, int totalScore, String statusUser) {
        SwingUtilities.invokeLater(() -> {
            HomeView homeView = new HomeView(username,totalScore, statusUser);
            new HomeController(homeView, this.clientSocketManager, this.clientSocket);
            homeView.setVisible(true);
            loginView.setVisible(false);
        });
    }

    private void closeClientSocket() {
        clientSocketManager.stopReceivingMessages();
    }
}
