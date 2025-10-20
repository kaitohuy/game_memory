package com.mycompany.btl_n6.Server.handle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mycompany.btl_n6.Server.dao.LeaderboardDAO;
import com.mycompany.btl_n6.Server.dao.SoloDAO;

public class SoloManager {
    private static final Map<String, String> soloAnswers = new ConcurrentHashMap<>();
    private static final Map<String, Integer> soloMistakes = new ConcurrentHashMap<>();
    private static final Map<String, Integer> soloTTLs = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> sentSoloRounds = new ConcurrentHashMap<>();
    private static final Map<String, Integer> sessionTotals = new ConcurrentHashMap<>();
    private static final Map<String, Integer> sessionRounds = new ConcurrentHashMap<>();
    // track number of correct rounds per session (for leaderboard roundsWon)
    private static final Map<String, Integer> sessionCorrectRounds = new ConcurrentHashMap<>();
    // map sessionId -> userId when a DB-backed numeric session is created
    private static final Map<String, Integer> sessionUserId = new ConcurrentHashMap<>();
    private static final int SOLO_MAX_MISTAKES = 3;
    private static final double LENGTH_WEIGHT = 1.0;
    private static final double SPEED_WEIGHT = 0.0;
    private static final double SCORE_SCALE = 100.0;

    public static void startSolo(String sessionId, ClientHandler handler) {
        int roundNumber = 1;
        String key = sessionId + ":" + roundNumber;
        String displayed = GameUtils.generateRandomString(4);
        int ttl = GameUtils.computeTtlForLength(4);
        soloAnswers.put(key, displayed);
        soloTTLs.put(key, ttl);
        sentSoloRounds.put(key, Boolean.TRUE);
        // If sessionId is numeric, remember mapping to userId for leaderboard update later
        try {
            Integer sid = Integer.parseInt(sessionId);
            int uid = handler.getUserId();
            if (uid > 0) sessionUserId.put(sessionId, uid);
        } catch (NumberFormatException ex) {
            // ignore non-numeric session ids (guests)
        }
        handler.sendMessage("SOLO_ROUND:" + sessionId + ":" + roundNumber + ":" + displayed + ":" + ttl);
    }

    public static void handleSubmit(String[] command, ClientHandler handler) {
        if (command.length < 2 || command[1].split(":", 4).length < 3) return;
        String[] parts = command[1].split(":", 4);
        String sessionId = parts[0];
        int roundNumber = parseIntSafe(parts[1]);
        String answer = parts[2];
        int timeMillis = parts.length >= 4 ? parseIntSafe(parts[3]) : 0;

        String key = sessionId + ":" + roundNumber;
        String expected = soloAnswers.getOrDefault(key, "");
        String expectedNorm = expected.trim().toUpperCase();
        String answerNorm = answer != null ? answer.trim().toUpperCase() : "";
        boolean correct = expectedNorm.equals(answerNorm);

        // Tính điểm
        int longest = longestCommonContiguousSubstring(expectedNorm, answerNorm);
        int displayedLen = Math.max(1, expectedNorm.length());
        double timeSeconds = Math.max(0.001, timeMillis / 1000.0);
        double frac = (double) longest / displayedLen;
        int points = (int) Math.round((frac * LENGTH_WEIGHT + timeSeconds * SPEED_WEIGHT) * SCORE_SCALE);
        if (points < 0) points = 0;

        // Cập nhật tổng điểm
        int newTotal = sessionTotals.merge(sessionId, points, Integer::sum);
    // Cập nhật số vòng thắng (correct rounds)
    if (correct) sessionCorrectRounds.merge(sessionId, 1, Integer::sum);

        // Lưu kết quả vòng chơi
        try {
            int sid = Integer.parseInt(sessionId);
            SoloDAO sdao = new SoloDAO();
            if (sdao.insertRound(sid, roundNumber, expected, displayedLen, soloTTLs.getOrDefault(key, 3),
                    answer, correct, timeMillis == 0 ? null : timeMillis, points)) {
                sdao.addToSessionTotals(sid, points, 1);
                sessionRounds.merge(sessionId, 1, Integer::sum);
            }
        } catch (NumberFormatException ignored) {}

        if (correct) {
            soloMistakes.remove(sessionId);
            handler.sendMessage("SOLO_RESULT:" + sessionId + ":" + roundNumber + ":TRUE:" + points + ":" + newTotal);
            sendNextRound(sessionId, roundNumber + 1, handler, true);
        } else {
            int mistakes = soloMistakes.merge(sessionId, 1, Integer::sum);
            int remaining = Math.max(0, SOLO_MAX_MISTAKES - mistakes);
            handler.sendMessage("SOLO_RESULT:" + sessionId + ":" + roundNumber + ":FALSE:" + points + ":" + newTotal + ":REMAIN:" + remaining);
            if (mistakes >= SOLO_MAX_MISTAKES) {
                handler.sendMessage("SOLO_END:" + sessionId + ":Kết thúc trò chơi");
                finalizeSession(sessionId);
            } else {
                sendNextRound(sessionId, roundNumber + 1, handler, false);
            }
        }
    }

    public static void handleContinue(String[] command, ClientHandler handler) {
        if (command.length < 2 || command[1].split(":", 2).length < 2) return;
        String[] parts = command[1].split(":", 2);
        String sessionId = parts[0];
        int lastRound = parseIntSafe(parts[1]);
        // Khi tiếp tục sau khi sai, giữ nguyên độ dài chuỗi
        sendNextRound(sessionId, lastRound + 1, handler, false);
    }

    private static void sendNextRound(String sessionId, int roundNumber, ClientHandler handler, boolean wasCorrect) {
        String nextKey = sessionId + ":" + roundNumber;
        if (sentSoloRounds.containsKey(nextKey)) return;
        int prevLen = 4;
        if (roundNumber > 1) {
            String prevKey = sessionId + ":" + (roundNumber - 1);
            String prevDisplayed = soloAnswers.get(prevKey);
            if (prevDisplayed != null) prevLen = prevDisplayed.length();
        }
        int nextLen = wasCorrect ? Math.min(12, prevLen + 1) : prevLen;
        String nextDisplayed = GameUtils.generateRandomString(nextLen);
        int nextTtl = GameUtils.computeTtlForLength(nextLen);
        soloAnswers.put(nextKey, nextDisplayed);
        soloTTLs.put(nextKey, nextTtl);
        sentSoloRounds.put(nextKey, Boolean.TRUE);
        handler.sendMessage("SOLO_ROUND:" + sessionId + ":" + roundNumber + ":" + nextDisplayed + ":" + nextTtl);
    }

    private static void finalizeSession(String sessionId) {
        try {
            int sid = Integer.parseInt(sessionId);
            SoloDAO sdao = new SoloDAO();
            int rounds = sessionRounds.getOrDefault(sessionId, 0);
            int total = sessionTotals.getOrDefault(sessionId, 0);
            sdao.finalizeSession(sid, total, rounds);
            // Also update solo leaderboard if we know the userId
            Integer uid = sessionUserId.get(sessionId);
            if (uid != null && uid > 0) {
                int roundsWon = sessionCorrectRounds.getOrDefault(sessionId, 0);
                try {
                    LeaderboardDAO lbd = new LeaderboardDAO();
                    lbd.recordSoloResult(uid, roundsWon, total);
                } catch (Exception ex) {
                    System.err.println("Failed to update solo leaderboard: " + ex.getMessage());
                }
            }
            // Notify connected clients about updated solo ranking
            try {
                com.mycompany.btl_n6.Server.handle.ClientHandler.broadcastSoloRanking();
            } catch (Exception ex) {
                System.err.println("Failed to broadcast solo ranking: " + ex.getMessage());
            }
        } catch (NumberFormatException ignored) {}
        sentSoloRounds.keySet().removeIf(k -> k.startsWith(sessionId + ":"));
        sessionRounds.remove(sessionId);
        sessionTotals.remove(sessionId);
        sessionCorrectRounds.remove(sessionId);
        sessionUserId.remove(sessionId);
        soloMistakes.remove(sessionId);
    }

    private static int longestCommonContiguousSubstring(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0;
        int n = a.length(), m = b.length();
        int max = 0;
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    max = Math.max(max, dp[i][j]);
                }
            }
        }
        return max;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 1; // Mặc định cho roundNumber
        }
    }
}