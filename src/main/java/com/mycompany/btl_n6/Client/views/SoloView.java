package com.mycompany.btl_n6.Client.views;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class SoloView extends JFrame {
    private final JLabel lblBanner;
    public final JTextField txtAnswer;
    private final JButton btnSubmit;
    private final JLabel lblCountdown;
    private final JProgressBar progressBar;
    private final DefaultListModel<String> historyModel;
    private final JList<String> historyList;
    private final JLabel lblStatus;
    private final JLabel lblPointsBadge;
    private final JButton btnExit;
    private final JButton btnContinue;
    private final JLabel lblMistakes;

    public SoloView() {
        setTitle("🎯 Chế độ Solo");
        setSize(900, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        // ==== Màu và font chủ đạo ====
        Color mainBg = new Color(245, 250, 255);
        Color accent = new Color(80, 140, 255);
        Color success = new Color(0, 160, 80);
        Color danger = new Color(200, 50, 50);
        Font mainFont = new Font("Segoe UI", Font.PLAIN, 16);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(mainBg);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(mainPanel);

        // ==== BANNER ====
        lblBanner = new JLabel("CHUỖI KÝ TỰ", SwingConstants.CENTER);
        lblBanner.setFont(new Font("Segoe UI", Font.BOLD, 38));
        lblBanner.setOpaque(true);
        lblBanner.setBackground(Color.WHITE);
        lblBanner.setForeground(new Color(30, 60, 120));
        lblBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 2),
                new EmptyBorder(25, 15, 25, 15)
        ));

        // ==== TRẠNG THÁI & ĐIỂM ====
        lblStatus = new JLabel("Sẵn sàng!", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblStatus.setForeground(new Color(0, 120, 180));

        lblPointsBadge = new JLabel("", SwingConstants.CENTER);
        lblPointsBadge.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblPointsBadge.setOpaque(true);
        lblPointsBadge.setVisible(false);

        lblMistakes = new JLabel("Mạng còn lại: 3", SwingConstants.CENTER);
        lblMistakes.setFont(mainFont);
        lblMistakes.setForeground(danger);

        JPanel topPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        topPanel.setOpaque(false);
        topPanel.add(lblStatus);
        topPanel.add(lblPointsBadge);
        topPanel.add(lblMistakes);

        // ==== COUNTDOWN + PROGRESS ====
        lblCountdown = new JLabel("Chuẩn bị...", SwingConstants.CENTER);
        lblCountdown.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(100);
        progressBar.setForeground(accent);
        progressBar.setBackground(new Color(225, 230, 240));
        progressBar.setStringPainted(true);
        progressBar.setFont(mainFont);

        JPanel countdownPanel = new JPanel(new BorderLayout(5, 5));
        countdownPanel.setOpaque(false);
        countdownPanel.add(lblCountdown, BorderLayout.NORTH);
        countdownPanel.add(progressBar, BorderLayout.CENTER);

        // ==== INPUT AREA ====
        txtAnswer = new JTextField(25);
        txtAnswer.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        txtAnswer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 2),
                new EmptyBorder(5, 10, 5, 10)
        ));

        btnSubmit = new JButton("GỬI");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnSubmit.setBackground(success);
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);

        btnExit = new JButton("Thoát");
        btnExit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnExit.setBackground(danger);
        btnExit.setForeground(Color.WHITE);
        btnExit.setFocusPainted(false);

        btnContinue = new JButton("Tiếp tục");
        btnContinue.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnContinue.setBackground(new Color(255, 200, 60));
        btnContinue.setFocusPainted(false);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        inputPanel.setOpaque(false);
        inputPanel.add(txtAnswer);
        inputPanel.add(btnSubmit);
        inputPanel.add(btnContinue);
        inputPanel.add(btnExit);

        // ==== HISTORY LIST ====
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        historyList.setBorder(BorderFactory.createTitledBorder("📜 Lịch sử chơi"));
        JScrollPane scrollPane = new JScrollPane(historyList);
        scrollPane.setPreferredSize(new Dimension(260, 0));

        // ==== GHÉP LAYOUT ====
        JPanel centerPanel = new JPanel(new BorderLayout(15, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(lblBanner, BorderLayout.CENTER);
        centerPanel.add(countdownPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(scrollPane, BorderLayout.EAST);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
    }

    // ====== Các phương thức hỗ trợ Controller ======
    public void setBannerText(String text) { lblBanner.setText(text); }
    public void clearBanner() { lblBanner.setText(""); }

    public void setCountdownText(String text) { lblCountdown.setText(text); }
    public void setProgress(int percent) { progressBar.setValue(Math.max(0, Math.min(100, percent))); }
    public void setProgressColor(Color color) { progressBar.setForeground(color); }

    public String getAnswer() { return txtAnswer.getText().trim(); }
    public void clearAnswer() { txtAnswer.setText(""); }
    public void setAnswer(String s) { txtAnswer.setText(s == null ? "" : s); txtAnswer.setCaretPosition(txtAnswer.getText().length()); }
    public void setInputEnabled(boolean enabled) { txtAnswer.setEnabled(enabled); btnSubmit.setEnabled(enabled); }

    public JButton getSubmitButton() { return btnSubmit; }
    public JButton getExitButton() { return btnExit; }
    public JButton getContinueButton() { return btnContinue; }

    public void addHistoryLine(String line) { historyModel.add(0, line); }
    public void setStatusText(String s) { lblStatus.setText(s); }

    public void showPointsBadge(String text, boolean positive) {
        lblPointsBadge.setText(text);
        lblPointsBadge.setBackground(positive ? new Color(220, 255, 220) : new Color(255, 220, 220));
        lblPointsBadge.setForeground(positive ? new Color(10, 100, 10) : new Color(150, 20, 20));
        lblPointsBadge.setVisible(true);
    }

    public void hidePointsBadge() { lblPointsBadge.setVisible(false); lblPointsBadge.setText(""); }
    public void setContinueVisible(boolean v) { btnContinue.setVisible(v); }
    public void setMistakesLeft(int left) { lblMistakes.setText("Mạng còn lại: " + left); }
}
