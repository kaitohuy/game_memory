package com.mycompany.btl_n6.Client.views;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ModeSelectionDialog extends JDialog {
    private final JRadioButton rbSuddenDeath = new JRadioButton("Sinh tử (vô hạn)");
    private final JRadioButton rb10 = new JRadioButton("10 round");
    private final JRadioButton rb20 = new JRadioButton("20 round");
    private final JRadioButton rbCustom = new JRadioButton("Tự nhập");
    private final JTextField tfCustom = new JTextField(5);
    private int chosenRounds = -1; // -1 = chưa chọn

    public ModeSelectionDialog(Window owner) {
        super(owner, "⚙️ Chọn chế độ thi đấu", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setAlwaysOnTop(true);

        // màu chủ đạo
        Color mainBg = new Color(245, 250, 255);
        Color card = Color.WHITE;
        Color accent = new Color(80, 140, 255);

        JPanel root = new JPanel(new BorderLayout(12,12));
        root.setBorder(new EmptyBorder(14,14,14,14));
        root.setBackground(mainBg);
        setContentPane(root);

        JLabel title = new JLabel("Chế độ & số round", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(30,60,120));
        root.add(title, BorderLayout.NORTH);

        JPanel cardPanel = new JPanel(new GridLayout(0,1,8,8));
        cardPanel.setBorder(new EmptyBorder(12,12,12,12));
        cardPanel.setBackground(card);
        cardPanel.setOpaque(true);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225,230,240)),
                new EmptyBorder(12,12,12,12)
        ));

        ButtonGroup g = new ButtonGroup();
        rbSuddenDeath.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        rb10.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        rb20.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        rbCustom.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        rb10.setSelected(true); // mặc định

        g.add(rbSuddenDeath); g.add(rb10); g.add(rb20); g.add(rbCustom);

        JPanel customRow = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        customRow.setOpaque(false);
        tfCustom.setColumns(6);
        tfCustom.setEnabled(false);
        rbCustom.addActionListener(e -> tfCustom.setEnabled(true));
        rb10.addActionListener(e -> tfCustom.setEnabled(false));
        rb20.addActionListener(e -> tfCustom.setEnabled(false));
        rbSuddenDeath.addActionListener(e -> tfCustom.setEnabled(false));
        customRow.add(rbCustom);
        customRow.add(new JLabel("Số round:"));
        customRow.add(tfCustom);

        cardPanel.add(rbSuddenDeath);
        cardPanel.add(rb10);
        cardPanel.add(rb20);
        cardPanel.add(customRow);

        root.add(cardPanel, BorderLayout.CENTER);

        JButton ok = new JButton("Bắt đầu");
        ok.setBackground(accent);
        ok.setForeground(Color.WHITE);
        ok.setFocusPainted(false);
        ok.addActionListener(e -> {
            if (rbSuddenDeath.isSelected()) chosenRounds = 0;
            else if (rb10.isSelected()) chosenRounds = 10;
            else if (rb20.isSelected()) chosenRounds = 20;
            else if (rbCustom.isSelected()) {
                try {
                    int v = Integer.parseInt(tfCustom.getText().trim());
                    if (v > 0) chosenRounds = v;
                } catch (NumberFormatException ignore) {}
            }
            if (chosenRounds >= 0) dispose();
            else JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập số round > 0 hoặc chọn Sinh tử.",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.add(ok);
        root.add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    public int getChosenRounds() { return chosenRounds; }
}
