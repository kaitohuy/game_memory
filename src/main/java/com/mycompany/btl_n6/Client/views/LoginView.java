/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.btl_n6.Client.views;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginView extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin, btnCancel, btnRegister;

    public LoginView() {
        setTitle("Đăng nhập hệ thống");
        setSize(350, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // Panel chính
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        JLabel lblUsername = new JLabel("Tên đăng nhập:");
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblUsername, gbc);

        txtUsername = new JTextField(15);
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(txtUsername, gbc);

        // Password
        JLabel lblPassword = new JLabel("Mật khẩu:");
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(lblPassword, gbc);

        txtPassword = new JPasswordField(15);
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(txtPassword, gbc);

        // Buttons
    btnLogin = new JButton("Đăng nhập");
    btnCancel = new JButton("Hủy");
    btnRegister = new JButton("Đăng ký");

        JPanel buttonPanel = new JPanel();
    buttonPanel.add(btnLogin);
    buttonPanel.add(btnRegister);
    buttonPanel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        add(panel);
    }

    // Getter dữ liệu
    public String getUsername() {
        return txtUsername.getText().trim();
    }

    public String getPassword() {
        return new String(txtPassword.getPassword());
    }

    // Gắn listener cho các nút
    public void addLoginListener(ActionListener listener) {
        btnLogin.addActionListener(listener);
    }

    public void addCancelListener(ActionListener listener) {
        btnCancel.addActionListener(listener);
    }

    public void addRegisterListener(ActionListener listener) {
        btnRegister.addActionListener(listener);
    }

    public void addWindowCloseListener(java.awt.event.WindowAdapter adapter) {
        addWindowListener(adapter);
    }

    // Getter cho controller sử dụng trực tiếp nút
    public JButton getLoginButton() {
        return btnLogin;
    }

    public JButton getCancelButton() {
        return btnCancel;
    }

    public JButton getRegisterButton() {
        return btnRegister;
    }

    // Hiển thị thông báo
    public void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }
}
