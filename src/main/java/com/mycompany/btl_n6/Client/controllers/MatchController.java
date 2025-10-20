package com.mycompany.btl_n6.Client.controllers;

import java.io.IOException;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JOptionPane;   // NEW: để hiện dialog
import java.awt.event.ActionListener;

import com.mycompany.btl_n6.Client.ClientSocket;
import com.mycompany.btl_n6.Client.manager.ClientSocketManager;
import com.mycompany.btl_n6.Client.manager.MessageListener;
import com.mycompany.btl_n6.Client.views.MatchView;
import com.mycompany.btl_n6.Client.views.ModeSelectionDialog;

public class MatchController implements MessageListener {

    private final MatchView view;
    private final ClientSocketManager manager;
    private final ClientSocket socket;

    private String currentMatchId;
    private int currentRound;

    // timing client-side
    private int showMs = 1500;
    private int answerMs = 2000;
    private long answerPhaseStart = -1; // để đo timeMillis
    private boolean answeredThisRound = false;

    private Timer uiTimer; // cập nhật progress mỗi 50ms
    private long phaseEndAtMs = 0;

    private enum Phase {
        SHOW, ANSWER, IDLE
    }
    private Phase phase = Phase.IDLE;

    public MatchController(MatchView view, ClientSocketManager manager, ClientSocket socket) {
        this.view = view;
        this.manager = manager;
        this.socket = socket;

        this.manager.addMessageListener(this);
        this.view.getSubmitButton().addActionListener(e -> submit());
        this.view.getReadyButton().addActionListener(e -> sendReady());
        this.view.getSurrenderButton().addActionListener(e -> surrender());
        SwingUtilities.invokeLater(() -> {
            view.setVisible(true);
            view.setInputEnabled(false);
            view.setCountdownText("Chờ trận bắt đầu…");
            view.setProgressPercent(0);
        });
    }

    @Override
    public void onMessageReceived(String message) {
        if (message == null) {
            return;
        }

        // 1) MATCH_START: KHÔNG gửi READY ở đây nữa
        if (message.startsWith("MATCH_START:")) {
            String[] parts = message.split(":");
            if (parts.length > 1) {
                currentMatchId = parts[1];
                SwingUtilities.invokeLater(() -> {
                    view.appendFeed("Trận " + currentMatchId + " đã tạo. Đang chờ cấu hình…");
                    view.setReadyVisible(false);
                    view.setWaitingVisible(false);
                });
            }
        } else if (message.startsWith("MATCH_MODE_REQUEST:")) {
            String[] parts = message.split(":");
            if (parts.length >= 2) {
                currentMatchId = parts[1];
            }
            SwingUtilities.invokeLater(() -> {
                ModeSelectionDialog dlg = new ModeSelectionDialog(view);
                dlg.setVisible(true); // modal
                int rounds = dlg.getChosenRounds();
                if (rounds < 0) {
                    return; // không gửi nếu tắt hộp thoại
                }
                safeSend("MATCH_MODE:" + currentMatchId + ":" + rounds);
                String text = (rounds == 0 ? "Sinh tử" : (rounds + " round"));
                view.setModeTitle("Chế độ: " + text);
                view.appendFeed("Bạn đã chọn: " + text);
            });

        }// 2) MATCH_CONFIG: hiện nút SẴN SÀNG
        else if (message.startsWith("MATCH_CONFIG:")) {
            // MATCH_CONFIG:<matchId>:<finalRounds or 0 for sudden>
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                currentMatchId = parts[1];
                int finalRounds = parseIntSafe(parts[2]);
                String modeText = (finalRounds == 0) ? "Sinh tử" : (finalRounds + " round");
                SwingUtilities.invokeLater(() -> {
                    view.setModeTitle("Chế độ: " + modeText);
                    view.setReadyVisible(true);            // show nút SẴN SÀNG
                    view.setWaitingVisible(false);         // ẩn dòng chờ
                    view.setCountdownText("Nhấn SẴN SÀNG để bắt đầu khi cả hai đã sẵn sàng.");
                });
            }
        } // 3) ROUND_START: parse V2 (showMs, answerMs) + bắt đầu pha hiển thị
        else if (message.startsWith("ROUND_START:")) {
            try {
                // payload: matchId:round:displayed:showMs:answerMs   (5 phần)
                String payload = message.substring("ROUND_START:".length());
                String[] parts = payload.split(":");
                if (parts.length < 4) {
                    return;
                }

                currentMatchId = parts[0];
                currentRound = Integer.parseInt(parts[1]);
                String displayed = parts[2];

                if (parts.length >= 5) {
                    // v2: có cả showMs và answerMs
                    showMs = Integer.parseInt(parts[3]);
                    answerMs = Integer.parseInt(parts[4]);
                } else {
                    // fallback v1 (hiếm khi dùng nữa)
                    int ttl = 3000; // mặc định an toàn
                    int len = Math.max(4, displayed.length());
                    showMs = Math.max(1200, (int) (ttl * 0.4));
                    answerMs = Math.max(1500, ttl - showMs);
                }

                SwingUtilities.invokeLater(() -> {
                    answeredThisRound = false;
                    view.setWaitingVisible(false);   // đã có ROUND_START => cả 2 đã ready
                    view.setReadyVisible(false);

                    view.setBanner(displayed);
                    view.clearAnswer();
                    view.setInputEnabled(false);

                    startPhaseShow(); // pha SHOW (hiển thị rồi tự chuyển ANSWER)
                    view.appendFeed("Vòng " + currentRound + " bắt đầu.");
                });
            } catch (Exception ignore) {
            }
        } else if (message.startsWith("ROUND_RESULT:")) {
            // ROUND_RESULT:<matchId>:<round>:<mePts>:<oppPts>:<correctFlag>
            String[] p = message.split(":");
            if (p.length >= 6) {
                int round = parseIntSafe(p[2]);
                int me = parseIntSafe(p[3]);
                int opp = parseIntSafe(p[4]);
                boolean correct = "1".equals(p[5]);
                SwingUtilities.invokeLater(() -> {
                    view.appendFeed("Vòng " + round + ": Bạn +" + me + " | Đối thủ +" + opp + (correct ? " ✅" : " ❌"));
                    stopUiTimer();
                });
            }

        } else if (message.startsWith("MATCH_RESULT:")) {
            // MATCH_RESULT:<matchId>:<myTotal>,<oppTotal>,<winnerId>
            String[] p = message.split(":");
            if (p.length >= 3) {
                String[] sc = p[2].split(",");
                int my = parseIntSafe(sc[0]);
                int opp = parseIntSafe(sc[1]);
                int winnerId = (sc.length >= 3) ? parseIntSafe(sc[2]) : 0;

                String verdict;
                if (my > opp) {
                    verdict = "WIN 🎉";
                } else if (my < opp) {
                    verdict = "LOSE 😢";
                } else {
                    verdict = "DRAW 🤝";
                }

                String finalMsg = (winnerId == 0 ? "Hòa!\n" : "") + "Kết quả: " + verdict;
                SwingUtilities.invokeLater(() -> {
                    view.appendFeed("KẾT QUẢ CUỐI: Bạn " + my + " – " + opp + " → " + verdict);
                    JOptionPane.showMessageDialog(view, finalMsg, "Kết thúc trận", JOptionPane.INFORMATION_MESSAGE);
                    manager.removeMessageListener(this);
                    stopUiTimer();
                });
            }

        } else if (message.startsWith("MATCH_ENDED_SURRENDER:")) {
            // Dạng mới: MATCH_ENDED_SURRENDER:matchId:winnerId:surrenderName:verdictForMe
            String[] p = message.split(":");
            SwingUtilities.invokeLater(() -> {
                if (p.length >= 5) {
                    String surrenderName = p[3];
                    String verdict = p[4]; // WIN/LOSE (đã là của "mình")
                    view.appendFeed("Trận kết thúc do " + surrenderName + " đầu hàng.");
                    JOptionPane.showMessageDialog(view,
                            surrenderName + " đã đầu hàng.\nKết quả: " + verdict,
                            "Kết thúc trận",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // fallback cũ
                    view.appendFeed("Trận kết thúc do đầu hàng.");
                    JOptionPane.showMessageDialog(view,
                            "Trận kết thúc do đầu hàng.",
                            "Kết thúc trận",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                manager.removeMessageListener(this);
                stopUiTimer();
            });
        }
    }

    private void startPhaseShow() {
        phase = Phase.SHOW;
        long now = System.currentTimeMillis();
        phaseEndAtMs = now + showMs;

        view.setCountdownText("ĐANG HIỂN THỊ… (cấm nhập)");
        view.setProgressPercent(100);
        ensureUiTimer();
    }

    private void startPhaseAnswer() {
        phase = Phase.ANSWER;
        long now = System.currentTimeMillis();
        phaseEndAtMs = now + answerMs;
        answerPhaseStart = now;

        view.clearBanner(); // biến mất chuỗi
        view.setCountdownText("NHẬP CÂU TRẢ LỜI…");
        view.setInputEnabled(true);
        view.focusAnswer();
        view.setProgressPercent(100);
    }

    private void ensureUiTimer() {
        stopUiTimer();
        uiTimer = new Timer(50, e -> {
            long now = System.currentTimeMillis();
            long remain = Math.max(0, phaseEndAtMs - now);
            int percent = (int) Math.round(100.0 * remain / (phase == Phase.SHOW ? showMs : answerMs));
            view.setProgressPercent(percent);

            if (remain == 0) {
                if (phase == Phase.SHOW) {
                    startPhaseAnswer();
                } else if (phase == Phase.ANSWER) {
                    // auto-submit nếu chưa gửi
                    if (!answeredThisRound) {
                        autoSubmitEmpty();
                    }
                    stopUiTimer();
                    phase = Phase.IDLE;
                }
            }
        });
        uiTimer.start();
    }

    private void stopUiTimer() {
        if (uiTimer != null) {
            uiTimer.stop();
            uiTimer = null;
        }
    }

    private void submit() {
        if (phase != Phase.ANSWER || answeredThisRound) {
            return;
        }

        String ans = view.getAnswer();
        if (ans == null) {
            ans = "";
        }
        int timeMillis = (int) Math.max(0, System.currentTimeMillis() - answerPhaseStart);
        safeSend("MATCH_SUBMIT:" + currentMatchId + ":" + currentRound + ":" + ans + ":" + timeMillis);
        answeredThisRound = true;
        view.setInputEnabled(false);
        view.setCountdownText("Đã gửi. Chờ đối thủ…");
    }

    private void autoSubmitEmpty() {
        String ans = view.getAnswer();
        if (ans == null || ans.isEmpty()) {
            ans = ""; // xem như sai
        }
        int timeMillis = (int) Math.max(0, System.currentTimeMillis() - answerPhaseStart);
        safeSend("MATCH_SUBMIT:" + currentMatchId + ":" + currentRound + ":" + ans + ":" + timeMillis);
        answeredThisRound = true;
        view.setInputEnabled(false);
        view.setCountdownText("Hết giờ. Đã tự gửi.");
    }

    private void surrender() {
        safeSend("MATCH_SURRENDER:" + currentMatchId);
        view.getSurrenderButton().setEnabled(false);
        view.getSubmitButton().setEnabled(false);
        stopUiTimer();
    }

    private void safeSend(String s) {
        try {
            socket.sendMessage(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void sendReady() {
        if (currentMatchId == null) {
            return;
        }
        safeSend("MATCH_READY:" + currentMatchId);
        view.setReadyVisible(false);
        view.setWaitingVisible(true); // hiện “vui lòng chờ đối thủ…”
        view.setCountdownText("Đang chờ đối thủ sẵn sàng…");
    }
}
