package com.mycompany.btl_n6.Client.controllers;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.mycompany.btl_n6.Client.ClientSocket;
import com.mycompany.btl_n6.Client.manager.ClientSocketManager;
import com.mycompany.btl_n6.Client.manager.MessageListener;
import com.mycompany.btl_n6.Client.views.SoloView;

public class SoloGameController implements MessageListener {

    private final SoloView soloView;
    private final ClientSocketManager socketManager;
    private final ClientSocket clientSocket;

    private String currentSessionId;
    private int currentRound;
    private int ttlSeconds;
    private Timer displayTimer;
    private Timer countdownTimer;
    private int countdownRemaining;
    private long answerStartMs = 0;
    private int sessionTotalPoints = 0;

    public SoloGameController(SoloView view, ClientSocketManager socketManager, ClientSocket clientSocket) {
        this.soloView = view;
        this.socketManager = socketManager;
        this.clientSocket = clientSocket;

        this.socketManager.addMessageListener(this);

        this.soloView.getSubmitButton().addActionListener(e -> submitAnswer());
        this.soloView.setInputEnabled(false);

        this.soloView.getExitButton().addActionListener(e -> {
            try {
                clientSocket.sendMessage("STATUS:ONLINE");
            } catch (IOException ex) {}
            SwingUtilities.invokeLater(() -> this.soloView.dispose());
            this.socketManager.removeMessageListener(this);
        });

        this.soloView.getContinueButton().addActionListener(e -> {
            try {
                clientSocket.sendMessage("SOLO_CONTINUE:" + currentSessionId + ":" + currentRound);
                soloView.setContinueVisible(false);
                soloView.setStatusText("Đang chờ vòng kế...");
            } catch (IOException ex) {}
        });
        this.soloView.setContinueVisible(false);
    }

    @Override
    public void onMessageReceived(String message) {
        if (message == null) return;

        if (message.startsWith("SOLO_ROUND:")) {
            String[] parts = message.split(":", 5);
            if (parts.length < 5) return;

            currentSessionId = parts[1];
            currentRound = safeParseInt(parts[2], 1);
            String displayed = parts[3];
            ttlSeconds = safeParseInt(parts[4], 3);
            // increase countdown by 1 second per user request
            ttlSeconds = Math.max(1, ttlSeconds + 1);

            SwingUtilities.invokeLater(() -> startRoundUI(displayed, ttlSeconds));

        } else if (message.startsWith("SOLO_RESULT:")) {
            String[] parts = message.split(":", 7);
            if (parts.length < 6) return;

            String sid = parts[1];
            int points = safeParseInt(parts[4], 0);
            int total = safeParseInt(parts[5].split(":")[0], 0);
            String correctStr = parts[3];
            int remain = extractRemainCount(message);

            if (sid.equals(currentSessionId)) sessionTotalPoints = total;

            SwingUtilities.invokeLater(() -> {
                soloView.addHistoryLine("Kết quả: " + correctStr + " (+" + points + " điểm) Tổng: " + total);
                soloView.showPointsBadge((points >= 0 ? "+" : "") + points + " pts", points > 0);
                new Timer(2000, ev -> { soloView.hidePointsBadge(); ((Timer)ev.getSource()).stop(); }).start();

                if (correctStr.equalsIgnoreCase("TRUE")) {
                    soloView.setStatusText("Đúng! +" + points + " điểm. Tổng: " + total);
                    soloView.setContinueVisible(false);
                } else {
                    soloView.setStatusText("Sai. Tổng: " + total);
                    soloView.setContinueVisible(true);
                    if (remain >= 0) soloView.setMistakesLeft(remain);
                }
            });

        } else if (message.startsWith("SOLO_END:")) {
            String payload = message.substring("SOLO_END:".length());
            SwingUtilities.invokeLater(() -> {
                soloView.addHistoryLine("Trò chơi kết thúc: " + payload);
                soloView.setStatusText("Game Over: " + payload);
                soloView.setInputEnabled(false);
                try { clientSocket.sendMessage("STATUS:ONLINE"); } catch (IOException ex) {}
                socketManager.removeMessageListener(this);
            });
        }
    }

    /** Bắt đầu 1 vòng chơi mới (hiển thị + nhập song song) */
    private void startRoundUI(String displayed, int ttl) {
        if (displayTimer != null && displayTimer.isRunning()) displayTimer.stop();
        if (countdownTimer != null && countdownTimer.isRunning()) countdownTimer.stop();

        soloView.setBannerText(displayed);
        soloView.setAnswer("");
        // disable typing while the string is visible; enable only after it's hidden
        soloView.setInputEnabled(false);
        answerStartMs = 0;

        // Phase 1: hiển thị chuỗi
        countdownRemaining = ttl;
        soloView.setCountdownText("Hiển thị chuỗi trong " + countdownRemaining + "s");
        soloView.setProgress(100);
        soloView.setProgressColor(new Color(80, 180, 255)); // khi hiển thị

        countdownTimer = new Timer(1000, (ActionEvent e) -> {
            countdownRemaining--;
            int percent = (int)(((double)countdownRemaining / ttl) * 100);
            soloView.setCountdownText("Hiển thị chuỗi trong " + Math.max(0, countdownRemaining) + "s");
            soloView.setProgress(percent);
            if (countdownRemaining <= 0) ((Timer)e.getSource()).stop();
        });
        countdownTimer.start();

        // Phase 2: ẩn chuỗi, tiếp tục đếm ngược nhập
        displayTimer = new Timer(ttl * 1000, ae -> {
            soloView.clearBanner();
            // stop any existing countdownTimer (the one used during display) to avoid double-decrement
            if (countdownTimer != null && countdownTimer.isRunning()) {
                countdownTimer.stop();
            }
            // answer time equals the display time (ttl)
            int answerTtl = ttl;
            countdownRemaining = answerTtl;
            soloView.setProgress(100);
            soloView.setProgressColor(new Color(255, 140, 0)); // khi nhập
            soloView.setCountdownText("Còn lại: " + countdownRemaining + "s");

            // enable input now that the banner is hidden and focus the answer field
            soloView.setInputEnabled(true);
            soloView.txtAnswer.requestFocusInWindow(); // Tự động focus vào ô nhập liệu

            countdownTimer = new Timer(1000, (ActionEvent ev) -> {
                countdownRemaining--;
                int percent = (int)(((double)countdownRemaining / answerTtl) * 100);
                soloView.setCountdownText("Còn lại: " + Math.max(0, countdownRemaining) + "s");
                soloView.setProgress(percent);

                if (countdownRemaining <= 0) {
                    ((Timer)ev.getSource()).stop();
                    submitAnswer();
                    soloView.setInputEnabled(false);
                }
            });
            countdownTimer.start();
        });
        displayTimer.setRepeats(false);
        displayTimer.start();
    }

    private void submitAnswer() {
        String ans = soloView.getAnswer();
        if (ans == null) ans = "";

        try {
            int timeMillis = (answerStartMs > 0)
                    ? (int)(System.currentTimeMillis() - answerStartMs)
                    : 0;
            clientSocket.sendMessage("SOLO_SUBMIT:" + currentSessionId + ":" + currentRound + ":" + ans + ":" + timeMillis);
            soloView.setInputEnabled(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int safeParseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private int extractRemainCount(String msg) {
        try {
            int idx = msg.indexOf("REMAIN:");
            if (idx >= 0) return Integer.parseInt(msg.substring(idx + 7).trim().split("\\s+")[0]);
        } catch (Exception ignored) {}
        return -1;
    }
}