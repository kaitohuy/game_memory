package com.mycompany.btl_n6.Client.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class HomeView extends JFrame {
    private JLabel lblWelcome, lblScore;
    private JButton btnSolo, btn1v1, btnLogout, btnRefreshRanking;
    private JTable tblOnlineUsers, tblRanking;
    private DefaultTableModel onlineUsersModel, rankingModel;

    public HomeView(String username, int totalScore, String status) {
        setTitle("🎮 Trang chủ - Memory Game Online");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // ==== Màu chủ đạo ====
        Color primary = new Color(52, 152, 219);
        Color bg = new Color(245, 248, 255);
        Color darkText = new Color(44, 62, 80);
        Font mainFont = new Font("Segoe UI", Font.PLAIN, 15);
        getContentPane().setBackground(bg);
        setLayout(new BorderLayout(0, 10));

        // ===== HEADER =====
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(primary);
        headerPanel.setBorder(new EmptyBorder(15, 25, 15, 25));

        lblWelcome = new JLabel("👋 Xin chào, " + username);
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblWelcome.setForeground(Color.WHITE);

        lblScore = new JLabel("🏆 Điểm: " + totalScore, SwingConstants.RIGHT);
        lblScore.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblScore.setForeground(Color.WHITE);

        headerPanel.add(lblWelcome, BorderLayout.WEST);
        headerPanel.add(lblScore, BorderLayout.EAST);

        // ===== BẢNG ONLINE =====
        String[] onlineCols = {"👤 Tên", "🏆 Điểm", "📶 Trạng thái"};
        onlineUsersModel = new DefaultTableModel(onlineCols, 0);
        tblOnlineUsers = new JTable(onlineUsersModel);
        styleTable(tblOnlineUsers);
        JScrollPane onlineScroll = new JScrollPane(tblOnlineUsers);
        onlineScroll.setBorder(createTitledBorder("🟢 Người chơi online", primary));

    // ===== BẢNG XẾP HẠNG SOLO =====
    String[] soloCols = {"🏅 Tên", "⭐ Điểm", "🎯 Số vòng thắng"};
    DefaultTableModel soloModel = new DefaultTableModel(soloCols, 0);
    JTable tblSoloRanking = new JTable(soloModel);
    styleTable(tblSoloRanking);
    JScrollPane soloScroll = new JScrollPane(tblSoloRanking);
    soloScroll.setBorder(createTitledBorder("🏆 BXH Solo", new Color(39, 174, 96)));

    // ===== BẢNG XẾP HẠNG ĐỐI KHÁNG =====
    String[] compCols = {"🏅 Tên", "⭐ Điểm", "🥇 Số trận thắng"};
    DefaultTableModel compModel = new DefaultTableModel(compCols, 0);
    JTable tblCompRanking = new JTable(compModel);
    styleTable(tblCompRanking);
    JScrollPane compScroll = new JScrollPane(tblCompRanking);
    compScroll.setBorder(createTitledBorder("🏁 BXH Đối kháng", new Color(231, 76, 60)));

        // ===== PANEL CHÍNH CHỨA 2 BẢNG =====
    JPanel centerPanel = new JPanel(new GridLayout(1, 3, 20, 0));
    centerPanel.setBorder(new EmptyBorder(15, 25, 15, 25));
    centerPanel.setBackground(bg);
    centerPanel.add(onlineScroll);
    centerPanel.add(soloScroll);
    centerPanel.add(compScroll);

        // ===== THANH NÚT Ở DƯỚI =====
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15));
        bottomPanel.setBackground(new Color(235, 243, 255));

        btnSolo = createStyledButton("🎯 Chơi Solo", new Color(52, 152, 219));
        btn1v1 = createStyledButton("⚔ Thi đấu 1v1", new Color(46, 204, 113));
        btnRefreshRanking = createStyledButton("🔄 Cập nhật BXH", new Color(241, 196, 15));
        btnLogout = createStyledButton("🚪 Đăng xuất", new Color(231, 76, 60));

        bottomPanel.add(btnSolo);
        bottomPanel.add(btn1v1);
        bottomPanel.add(btnRefreshRanking);
        bottomPanel.add(btnLogout);

        // ===== THÊM TẤT CẢ LÊN FRAME =====
        add(headerPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ===== TẠO NÚT CÓ KIỂU =====
    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setPreferredSize(new Dimension(180, 45));
        btn.setBorder(new RoundedBorder(15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(color.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    // ===== TẠO BORDER CHO TABLE =====
    private TitledBorder createTitledBorder(String title, Color color) {
        return BorderFactory.createTitledBorder(
            new LineBorder(color, 2, true),
            title,
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            color
        );
    }

    // ===== STYLE TABLE =====
    private void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setGridColor(new Color(230, 230, 230));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(220, 240, 255));

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(52, 152, 219));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setReorderingAllowed(false);
    }

    // ===== BORDER TRÒN =====
    private static class RoundedBorder implements Border {
        private final int radius;
        RoundedBorder(int radius) { this.radius = radius; }
        public Insets getBorderInsets(Component c) {
            return new Insets(radius+1, radius+1, radius+1, radius+1);
        }
        public boolean isBorderOpaque() { return false; }
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.drawRoundRect(x, y, w-1, h-1, radius, radius);
        }
    }

    // ===== GETTERS =====
    public JButton getBtnSolo() { return btnSolo; }
    public JButton getBtn1v1() { return btn1v1; }
    public JButton getBtnLogout() { return btnLogout; }
    public JButton getBtnRefreshRanking() { return btnRefreshRanking; }

    public JTable getOnlineUsersTable() { return tblOnlineUsers; }

    public void updateOnlineUsers(Object[][] data) {
        onlineUsersModel.setRowCount(0);
        for (Object[] row : data) onlineUsersModel.addRow(row);
    }

    // update solo ranking: data each row = {name, points, roundsWon}
    public void updateSoloRanking(Object[][] data) {
        // find the solo table's model by searching scroll components
        JScrollPane sp = (JScrollPane) ((java.awt.Container) getContentPane().getComponent(1)).getComponent(1);
        JTable t = (JTable) sp.getViewport().getView();
        DefaultTableModel m = (DefaultTableModel) t.getModel();
        m.setRowCount(0);
        for (Object[] row : data) m.addRow(row);
    }

    // update competitive ranking: data each row = {name, points, wins}
    public void updateCompetitiveRanking(Object[][] data) {
        JScrollPane sp = (JScrollPane) ((java.awt.Container) getContentPane().getComponent(1)).getComponent(2);
        JTable t = (JTable) sp.getViewport().getView();
        DefaultTableModel m = (DefaultTableModel) t.getModel();
        m.setRowCount(0);
        for (Object[] row : data) m.addRow(row);
    }
}
