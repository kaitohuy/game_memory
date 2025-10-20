package com.mycompany.btl_n6.Client.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.mycompany.btl_n6.Client.ClientSocket;
import com.mycompany.btl_n6.Client.manager.ClientSocketManager;
import com.mycompany.btl_n6.Client.manager.MessageListener;
import com.mycompany.btl_n6.Client.sessions.UserSession; // <<< ADD
import com.mycompany.btl_n6.Client.views.HomeView;
import com.mycompany.btl_n6.Client.views.MatchView;
import com.mycompany.btl_n6.Client.views.SoloView;

public class HomeController implements MessageListener {

    private final HomeView homeView;
    private final ClientSocketManager clientSocketManager;
    private final ClientSocket clientSocket;

    public HomeController(HomeView homeView, ClientSocketManager clientSocketManager, ClientSocket clientSocket) {
        this.homeView = homeView;
        this.clientSocketManager = clientSocketManager;
        this.clientSocket = clientSocket;

        this.clientSocketManager.addMessageListener(this);

        this.homeView.getBtnSolo().addActionListener(e -> startSolo());
        this.homeView.getBtn1v1().addActionListener(e -> invitePlayer());
        this.homeView.getBtnLogout().addActionListener(e -> logout());
        this.homeView.getBtnRefreshRanking().addActionListener(e -> requestRanking());

        requestOnlineUsers();
        requestRanking();
    }

    @Override
    public void onMessageReceived(String message) {
        try {
            if (message == null || message.isEmpty()) {
                return;
            }

            if (message.startsWith("ONLINE_USERS:")) {
                String payload = message.substring("ONLINE_USERS:".length());
                Object[][] data = parseRows(payload, 3);
                SwingUtilities.invokeLater(() -> homeView.updateOnlineUsers(data));

            } else if (message.startsWith("RANKING:")) {
                String payload = message.substring("RANKING:".length());
                Object[][] data = parseRows(payload, 3);
                SwingUtilities.invokeLater(() -> homeView.updateCompetitiveRanking(data));

            } else if (message.startsWith("RANKING_SOLO:")) {
                String payload = message.substring("RANKING_SOLO:".length());
                Object[][] data = parseRows(payload, 3);
                SwingUtilities.invokeLater(() -> homeView.updateSoloRanking(data));

            } else if (message.startsWith("RANKING_COMPETITIVE:")) {
                String payload = message.substring("RANKING_COMPETITIVE:".length());
                Object[][] data = parseRows(payload, 3);
                SwingUtilities.invokeLater(() -> homeView.updateCompetitiveRanking(data));

            } else if (message.startsWith("INVITE_FROM:")) {
                String from = message.substring("INVITE_FROM:".length());
                SwingUtilities.invokeLater(() -> {
                    int opt = JOptionPane.showConfirmDialog(homeView,
                            "Bạn có chấp nhận lời mời từ " + from + "?",
                            "Lời mời thi đấu",
                            JOptionPane.YES_NO_OPTION);
                    try {
                        if (opt == JOptionPane.YES_OPTION) {
                            clientSocket.sendMessage("INVITE_RESPONSE:" + from + ":OK");
                        } else {
                            clientSocket.sendMessage("INVITE_RESPONSE:" + from + ":REJECT");
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });

                // <<< NEW: hiển thị lỗi mời từ server
            } else if (message.startsWith("INVITE:FAILURE:")) {
                String reason = message.substring("INVITE:FAILURE:".length());
                SwingUtilities.invokeLater(()
                        -> JOptionPane.showMessageDialog(homeView, reason, "Không thể mời", JOptionPane.WARNING_MESSAGE)
                );

            } else if (message.startsWith("MATCH_START:")) {
                String payload = message.substring("MATCH_START:".length());
                String[] parts = payload.split(":", 2);
                String matchId = parts.length > 0 ? parts[0] : "";

                try {
                    SwingUtilities.invokeAndWait(() -> {
                        try {
                            clientSocket.sendMessage("STATUS:BUSY");
                        } catch (IOException ex) {
                        }
                        MatchView mv = new MatchView();
                        MatchController mc = new MatchController(mv, clientSocketManager, clientSocket);
                        mv.setVisible(true);
                        try {
                            clientSocket.sendMessage("MATCH_READY:" + matchId);
                        } catch (IOException ex) {
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            clientSocket.sendMessage("STATUS:BUSY");
                        } catch (IOException ex) {
                        }
                        MatchView mv = new MatchView();
                        MatchController mc = new MatchController(mv, clientSocketManager, clientSocket);
                        mv.setVisible(true);
                    });
                }

            } else if (message.startsWith("MATCH_RESULT:")) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        clientSocket.sendMessage("STATUS:ONLINE");
                    } catch (IOException ex) {
                    }
                });
            }

        } catch (Exception ex) {
            Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE,
                    "Lỗi khi xử lý message: " + message, ex);
        }
    }

    private void startSolo() {
        SwingUtilities.invokeLater(() -> {
            SoloView soloView = new SoloView();
            SoloGameController soloController = new SoloGameController(soloView, clientSocketManager, clientSocket);
            soloView.setVisible(true);
        });
        SwingUtilities.invokeLater(() -> {
            try {
                clientSocket.sendMessage("START_SOLO");
            } catch (IOException ex) {
                Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(homeView, "Không thể gửi yêu cầu START_SOLO tới server.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // <<< UPDATED: chỉ mời người ONLINE và không mời chính mình
    private void invitePlayer() {
        int row = homeView.getOnlineUsersTable().getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(homeView, "Hãy chọn một người chơi để mời!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String opponent = String.valueOf(homeView.getOnlineUsersTable().getValueAt(row, 0));
        String status = String.valueOf(homeView.getOnlineUsersTable().getValueAt(row, 2));
        String me = (UserSession.getCurrentUser() != null) ? UserSession.getCurrentUser().getUsername() : "";

        if (opponent.equalsIgnoreCase(me)) {
            JOptionPane.showMessageDialog(homeView, "Bạn không thể mời chính mình.", "Không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!"ONLINE".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(homeView, "Chỉ có thể mời người đang ONLINE.", "Không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            clientSocket.sendMessage("INVITE:" + opponent);
            JOptionPane.showMessageDialog(homeView, "Đã gửi lời mời tới " + opponent);
        } catch (IOException ex) {
            Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(homeView, "Không thể gửi lời mời.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        try {
            clientSocket.sendMessage("LOGOUT");
        } catch (IOException ex) {
            Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            clientSocketManager.removeMessageListener(this);
            clientSocketManager.stopReceivingMessages();
            JOptionPane.showMessageDialog(homeView, "Bạn đã đăng xuất!");
            System.exit(0);
        }
    }

    private void requestRanking() {
        try {
            clientSocket.sendMessage("GET_RANKING");
        } catch (IOException ex) {
            Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void requestOnlineUsers() {
        try {
            clientSocket.sendMessage("GET_USERS");
        } catch (IOException ex) {
            Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Object[][] parseRows(String payload, int columnCount) {
        if (payload == null || payload.isEmpty()) {
            return new Object[0][0];
        }
        String[] rows = payload.split(";");
        ArrayList<Object[]> list = new ArrayList<>();
        for (String r : rows) {
            if (r == null || r.trim().isEmpty()) {
                continue;
            }
            String[] cols = r.trim().split(",");
            Object[] row = new Object[columnCount];
            for (int i = 0; i < columnCount; i++) {
                row[i] = i < cols.length ? cols[i].trim() : "";
            }
            list.add(row);
        }
        Object[][] result = new Object[list.size()][columnCount];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }
}
