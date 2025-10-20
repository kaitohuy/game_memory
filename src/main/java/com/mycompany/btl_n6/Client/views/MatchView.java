package com.mycompany.btl_n6.Client.views;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class MatchView extends JFrame {

    private final JLabel lblModeTitle;     // tiêu đề chế độ ở giữa trên cùng
    private final JButton btnSurrender;    // góc trên bên phải
    private final JLabel lblRoundBanner;   // banner chuỗi ngẫu nhiên

    private final JLabel lblCountdown;     // text đếm
    private final JProgressBar progressBar;// thanh tiến trình

    private final JTextField txtAnswer;    // input
    private final JButton btnSubmit;       // gửi
    private final JTextArea txtFeed;       // lịch sử/nhật ký

    // Overlay giữa màn: Ready + Waiting
    private final JButton btnCenterReady;
    private final JLabel lblCenterWait;

    // giữ tham chiếu stack để ẩn/hiện đúng
    private final JPanel stack;

    public MatchView() {
        setTitle("⚔️ Thi đấu 1v1");
        setSize(980, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        Color mainBg = new Color(245, 250, 255);
        Color accent = new Color(80, 140, 255);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(mainBg);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        // ===== TOP BAR =====
        lblModeTitle = new JLabel("Chế độ: ...", SwingConstants.CENTER);
        lblModeTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblModeTitle.setForeground(new Color(25, 90, 160));

        btnSurrender = new JButton("Đầu hàng");
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topRight.setOpaque(false);
        topRight.add(btnSurrender);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblModeTitle, BorderLayout.CENTER);
        top.add(topRight, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        // ===== CENTER: Banner + Overlay (OverlayLayout) + Timing =====
        // Banner
        lblRoundBanner = new JLabel("Vòng đấu", SwingConstants.CENTER);
        lblRoundBanner.setFont(new Font("Consolas", Font.BOLD, 40));
        lblRoundBanner.setOpaque(true);
        lblRoundBanner.setBackground(Color.WHITE);
        lblRoundBanner.setForeground(new Color(30, 60, 120));
        lblRoundBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 2),
                new EmptyBorder(24, 18, 24, 18)
        ));
        lblRoundBanner.setAlignmentX(0.5f);
        lblRoundBanner.setAlignmentY(0.5f);

        // Overlay (Ready + Waiting) đặt chồng lên banner
        btnCenterReady = new JButton("SẴN SÀNG");
        btnCenterReady.setFont(new Font("Segoe UI", Font.BOLD, 24));
        btnCenterReady.setForeground(Color.WHITE);
        btnCenterReady.setBackground(accent);
        btnCenterReady.setFocusPainted(false);
        btnCenterReady.setContentAreaFilled(true);
        btnCenterReady.setBorder(BorderFactory.createEmptyBorder(12, 28, 12, 28));
        btnCenterReady.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCenterReady.setPreferredSize(new Dimension(260, 72));
        btnCenterReady.setVisible(false);

        lblCenterWait = new JLabel("Vui lòng chờ đối thủ của bạn sẵn sàng...", SwingConstants.CENTER);
        lblCenterWait.setFont(new Font("Segoe UI", Font.ITALIC, 18));
        lblCenterWait.setForeground(new Color(150, 80, 0));
        lblCenterWait.setVisible(false);

        JPanel overlayPanel = new JPanel(new GridBagLayout());
        overlayPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.insets = new Insets(6, 6, 6, 6);
        overlayPanel.add(btnCenterReady, gbc);
        gbc.gridy = 1;
        overlayPanel.add(lblCenterWait, gbc);
        overlayPanel.setAlignmentX(0.5f);
        overlayPanel.setAlignmentY(0.5f);

        // Stack = banner (dưới) + overlay (trên)
        stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new OverlayLayout(stack));
        // Quan trọng: add banner TRƯỚC, overlay SAU => overlay nằm TRÊN
        stack.add(lblRoundBanner); // dưới
        stack.add(overlayPanel);   // trên

        // Timing (đồng hồ + progress)
        lblCountdown = new JLabel("Chuẩn bị...", SwingConstants.CENTER);
        lblCountdown.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setValue(100);

        JPanel timing = new JPanel(new BorderLayout(6, 6));
        timing.setOpaque(false);
        timing.add(lblCountdown, BorderLayout.NORTH);
        timing.add(progressBar, BorderLayout.CENTER);

        // Khu vực trung tâm (stack + timing)
        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);
        center.add(stack, BorderLayout.CENTER);
        center.add(timing, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        // ===== FEED (bên phải) =====
        txtFeed = new JTextArea();
        txtFeed.setEditable(false);
        txtFeed.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane feedScroll = new JScrollPane(txtFeed);
        feedScroll.setPreferredSize(new Dimension(280, 0));
        root.add(feedScroll, BorderLayout.EAST);

        // ===== BOTTOM (input) =====
        // ===== BOTTOM (input) =====
        txtAnswer = new JTextField();
        txtAnswer.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        txtAnswer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 2),
                new EmptyBorder(6, 10, 6, 10)
        ));
        btnSubmit = new JButton("GỬI");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 16));

        /* Enter để gửi */
        txtAnswer.addActionListener(e -> {
            if (btnSubmit.isEnabled()) {
                btnSubmit.doClick();
            }
        });
        getRootPane().setDefaultButton(btnSubmit); // Enter = GỬI (tiện ở ô input)

        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.setOpaque(false);
        bottom.add(txtAnswer, BorderLayout.CENTER);
        bottom.add(btnSubmit, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);
    }

    // ===== API cho Controller =====
    public void setModeTitle(String s) {
        lblModeTitle.setText(s);
    }

    public void setBanner(String s) {
        lblRoundBanner.setText(s);
    }

    public void clearBanner() {
        lblRoundBanner.setText("");
    }

    public void setCountdownText(String s) {
        lblCountdown.setText(s);
    }

    public void setProgressPercent(int p) {
        progressBar.setValue(Math.max(0, Math.min(100, p)));
    }

    public String getAnswer() {
        return txtAnswer.getText().trim();
    }

    public void clearAnswer() {
        txtAnswer.setText("");
    }

    public void setInputEnabled(boolean en) {
        txtAnswer.setEnabled(en);
        btnSubmit.setEnabled(en);
    }

    public JButton getSubmitButton() {
        return btnSubmit;
    }

    public JButton getSurrenderButton() {
        return btnSurrender;
    }

    public void appendFeed(String line) {
        txtFeed.append(line + "\n");
        txtFeed.setCaretPosition(txtFeed.getDocument().getLength());
    }

    public JButton getReadyButton() {
        return btnCenterReady;
    }

    /**
     * Hiện nút SẴN SÀNG ở giữa và ẩn banner để không còn “khung xanh” phía sau
     */
    public void setReadyVisible(boolean v) {
        btnCenterReady.setVisible(v);
        if (v) {
            lblCenterWait.setVisible(false);
            lblRoundBanner.setVisible(false);  // ẩn lớp phía sau
        } else {
            // chỉ bật lại banner nếu cũng không còn chờ
            if (!lblCenterWait.isVisible()) {
                lblRoundBanner.setVisible(true);
            }
        }
        stack.revalidate();
        stack.repaint();
    }

    /**
     * Hiện dòng chờ đối thủ và ẩn banner
     */
    public void setWaitingVisible(boolean v) {
        lblCenterWait.setVisible(v);
        if (v) {
            btnCenterReady.setVisible(false);
            lblRoundBanner.setVisible(false);
        } else {
            // chỉ bật lại banner nếu cũng không còn nút ready
            if (!btnCenterReady.isVisible()) {
                lblRoundBanner.setVisible(true);
            }
        }
        stack.revalidate();
        stack.repaint();
    }

    public void focusAnswer() {
        SwingUtilities.invokeLater(() -> {
            txtAnswer.requestFocusInWindow();
            txtAnswer.selectAll();
        });
    }
}
