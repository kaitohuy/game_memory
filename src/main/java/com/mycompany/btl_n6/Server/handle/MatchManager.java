package com.mycompany.btl_n6.Server.handle;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.mycompany.btl_n6.Server.dao.MatchDAO;
import com.mycompany.btl_n6.Server.dao.UserDAO;
import com.mycompany.btl_n6.Server.dao.LeaderboardDAO; // NEW

public class MatchManager {

    private static final Map<String, MatchState> matches = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private static final MatchDAO matchDAO = new MatchDAO();
    private static final Map<String, Set<ClientHandler>> readyClients = new ConcurrentHashMap<>();

    /** Cấu hình tăng độ khó và thời gian */
    private static final int BASE_LEN = 4;         // độ dài khởi điểm
    private static final int LEN_STEP_EVERY = 3;   // mỗi 3 vòng tăng 1 ký tự
    private static final int MAX_LEN = 100;

    // 1s/char, min 4s (đúng như bạn đang dùng)
    private static final int SHOW_PER_CHAR = 1000;
    private static final int SHOW_MIN = 4000, SHOW_MAX = 100000;
    private static final int ANSWER_PER_CHAR = 1000;
    private static final int ANSWER_MIN = 4000, ANSWER_MAX = 100000;

    public static void createMatch(String matchId, ClientHandler a, ClientHandler b) {
        MatchState state = new MatchState(matchId, a, b);
        matches.put(matchId, state);
        System.out.println("[DEBUG] Match created: " + matchId + " for " + a.getUsername() + " and " + b.getUsername());
    }

    public static void clientReady(String matchId, ClientHandler handler) {
        MatchState s = matches.get(matchId);
        if (s == null) return;

        // Chỉ nhận READY sau khi đã chốt chế độ
        if (!s.modeConfigured) {
            System.out.println("[DEBUG] Ignore READY (mode not configured yet) for " + matchId);
            return;
        }

        readyClients.computeIfAbsent(matchId, k -> ConcurrentHashMap.newKeySet()).add(handler);
        Set<ClientHandler> set = readyClients.get(matchId);

        if (set != null && set.size() >= 2) {
            System.out.println("[DEBUG] Both clients ready for " + matchId);
            synchronized (s) {
                if (!s.started) {
                    s.started = true;
                    startRound(matchId, 1);
                }
            }
        }
    }

    private static int getLengthForRound(int round) {
        int inc = (Math.max(1, round) - 1) / LEN_STEP_EVERY;
        return Math.min(MAX_LEN, BASE_LEN + inc);
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(idx));
        }
        return sb.toString();
    }

    private static void startRound(String matchId, int roundNumber) {
        MatchState state = matches.get(matchId);
        if (state == null) return;

        int len = getLengthForRound(roundNumber);
        String displayed = generateRandomString(len);
        int showMs   = clamp(len * SHOW_PER_CHAR,   SHOW_MIN,   SHOW_MAX);
        int answerMs = clamp(len * ANSWER_PER_CHAR, ANSWER_MIN, ANSWER_MAX);

        synchronized (state) {
            if (state.ended) return;
            state.currentRound = roundNumber;
            state.displayedText = displayed;
            state.showMs = showMs;
            state.answerMs = answerMs;
            state.aAnswer = null;
            state.bAnswer = null;
            state.aTime = null;
            state.bTime = null;
        }

        System.out.println("[DEBUG] Starting round " + roundNumber + " for match " + matchId
                + " len=" + len + " showMs=" + showMs + " answerMs=" + answerMs);

        // v2 protocol
        state.a.sendMessage("ROUND_START:" + matchId + ":" + roundNumber + ":" + displayed + ":" + showMs + ":" + answerMs);
        state.b.sendMessage("ROUND_START:" + matchId + ":" + roundNumber + ":" + displayed + ":" + showMs + ":" + answerMs);

        int totalWait = showMs + answerMs + 200;
        scheduler.schedule(() -> evaluateRound(matchId), totalWait, TimeUnit.MILLISECONDS);
    }

    public static void submitAnswer(String matchId, ClientHandler sender, String answer, int timeMillis) {
        MatchState state = matches.get(matchId);
        if (state == null) return;
        synchronized (state) {
            if (state.ended) return;
            if (sender == state.a) {
                state.aAnswer = answer;
                state.aTime = timeMillis;
            } else if (sender == state.b) {
                state.bAnswer = answer;
                state.bTime = timeMillis;
            }
        }
    }

    private static void evaluateRound(String matchId) {
        MatchState state = matches.get(matchId);
        if (state == null) return;

        int aPoints = 0, bPoints = 0;
        boolean aCorrect, bCorrect;
        boolean suddenDeath;
        int winnerUserIdEarly = 0;   // winner ngay trong round (sinh tử, 1 đúng/1 sai)
        int roundNo;
        String shown;

        synchronized (state) {
            if (state.ended) return;

            shown = state.displayedText;
            aCorrect = shown != null && shown.equals(state.aAnswer);
            bCorrect = shown != null && shown.equals(state.bAnswer);

            suddenDeath = (state.finalRounds == Integer.MAX_VALUE);
            if (suddenDeath) {
                // 1 người đúng, 1 người sai -> kết thúc ngay
                if (aCorrect ^ bCorrect) {
                    winnerUserIdEarly = aCorrect ? state.a.getUserId() : state.b.getUserId();
                }
                // cả 2 đúng/sai -> tiếp tục tính điểm như bình thường
            }

            if (aCorrect && bCorrect) {
                if (state.aTime != null && state.bTime != null) {
                    if (state.aTime < state.bTime) { aPoints = 2; bPoints = 1; }
                    else if (state.bTime < state.aTime) { bPoints = 2; aPoints = 1; }
                    else { aPoints = 1; bPoints = 1; }
                } else {
                    aPoints = 1; bPoints = 1;
                }
            } else if (aCorrect) {
                aPoints = 2;
            } else if (bCorrect) {
                bPoints = 2;
            }

            roundNo = state.currentRound;

            // Lưu TTL theo GIÂY cho đúng tên cột (ttlSeconds)
            int ttlSeconds = (state.showMs + state.answerMs) / 1000;
            matchDAO.insertMatchRound(
                Integer.parseInt(matchId),
                roundNo,
                shown,
                (shown != null ? shown.length() : 0),
                ttlSeconds,
                state.aAnswer, state.aTime,
                state.bAnswer, state.bTime,
                aPoints, bPoints
            );

            state.aTotal += aPoints;
            state.bTotal += bPoints;
        }

        // Thông báo kết quả vòng
        String resA = "ROUND_RESULT:" + matchId + ":" + roundNo + ":" + aPoints + ":" + bPoints + ":" + (aCorrect ? "1" : "0");
        String resB = "ROUND_RESULT:" + matchId + ":" + roundNo + ":" + bPoints + ":" + aPoints + ":" + (bCorrect ? "1" : "0");
        state.a.sendMessage(resA);
        state.b.sendMessage(resB);

        // Sinh tử: có winner ngay trong round -> kết thúc luôn
        if (suddenDeath && winnerUserIdEarly != 0) {
            endMatchAndUpdateDB(matchId, state, winnerUserIdEarly);
            return;
        }

        // Kết thúc theo số round
        boolean shouldEndNow = false;
        synchronized (state) {
            if (state.finalRounds != Integer.MAX_VALUE && state.currentRound >= state.finalRounds) {
                shouldEndNow = true;
            }
        }
        if (shouldEndNow) {
            Integer winnerId = null;
            int aUserId = state.a.getUserId();
            int bUserId = state.b.getUserId();
            if (state.aTotal > state.bTotal) winnerId = aUserId;
            else if (state.bTotal > state.aTotal) winnerId = bUserId;

            endMatchAndUpdateDB(matchId, state, winnerId);
            return;
        }

        // Chưa kết thúc -> sang round tiếp
        if (!state.ended) {
            int nextRound = state.currentRound + 1;
            scheduler.schedule(() -> {
                MatchState s = matches.get(matchId);
                if (s == null || s.ended) return;
                startRound(matchId, nextRound);
            }, 1200, TimeUnit.MILLISECONDS);
        }
    }

    /** Gộp toàn bộ kết sổ: matches, users, leaderboard, gửi MATCH_RESULT */
    private static void endMatchAndUpdateDB(String matchId, MatchState state, Integer winnerId) {
        int matchIdInt = Integer.parseInt(matchId);

        matchDAO.finalizeMatch(matchIdInt, winnerId);

        UserDAO userDAO = new UserDAO();
        LeaderboardDAO lb = new LeaderboardDAO();

        int aUserId = state.a.getUserId();
        int bUserId = state.b.getUserId();

        // Cộng tổng điểm sang bảng users (tương thích hệ thống cũ)
        userDAO.incrementScoreAndWins(aUserId, state.aTotal, (winnerId != null && winnerId == aUserId) ? 1 : 0);
        userDAO.incrementScoreAndWins(bUserId, state.bTotal, (winnerId != null && winnerId == bUserId) ? 1 : 0);

        // Leaderboard competitive (1v1): +win cho người thắng, cả 2 đều +points của trận
        lb.recordCompetitiveResult(aUserId, (winnerId != null && winnerId == aUserId) ? 1 : 0, state.aTotal);
        lb.recordCompetitiveResult(bUserId, (winnerId != null && winnerId == bUserId) ? 1 : 0, state.bTotal);

        // Gửi kết quả cuối
        state.a.sendMessage("MATCH_RESULT:" + matchId + ":" + state.aTotal + "," + state.bTotal + "," + (winnerId != null ? winnerId : 0));
        state.b.sendMessage("MATCH_RESULT:" + matchId + ":" + state.bTotal + "," + state.aTotal + "," + (winnerId != null ? winnerId : 0));

        matches.remove(matchId);
        readyClients.remove(matchId);

        // trả trạng thái về ONLINE
        UserDAO dao = new UserDAO();
        dao.updateStatus(state.a.getUserId(), "ONLINE");
        dao.updateStatus(state.b.getUserId(), "ONLINE");

        try { ClientHandler.broadcastSoloRanking(); } catch (Exception ignore) {}
    }

    /** rounds=0 sinh tử; >0 là số round; khi 2 bên xong -> chỉ gửi MATCH_CONFIG, không auto-start */
    public static void receiveMode(String matchId, ClientHandler sender, int rounds) {
        MatchState s = matches.get(matchId);
        if (s == null) return;

        boolean justConfigured = false;

        synchronized (s) {
            if (sender == s.a) s.aProposedRounds = rounds;
            else if (sender == s.b) s.bProposedRounds = rounds;

            if (!s.modeConfigured && s.aProposedRounds != null && s.bProposedRounds != null) {
                int a = s.aProposedRounds, b = s.bProposedRounds;

                if (a == 0 && b == 0) s.finalRounds = Integer.MAX_VALUE; // sinh tử
                else if (a == 0)      s.finalRounds = Math.max(1, b);
                else if (b == 0)      s.finalRounds = Math.max(1, a);
                else {
                    int avg = Math.max(1, Math.round((a + b) / 2.0f));
                    s.finalRounds = avg;
                }

                s.modeConfigured = true;
                justConfigured = true;
            }
        }

        if (justConfigured) {
            int announce = (s.finalRounds == Integer.MAX_VALUE) ? 0 : s.finalRounds;
            s.a.sendMessage("MATCH_CONFIG:" + matchId + ":" + announce);
            s.b.sendMessage("MATCH_CONFIG:" + matchId + ":" + announce);
            // Start sẽ diễn ra khi BOTH READY trong clientReady()
        }
    }

    public static void surrender(String matchId, ClientHandler sender) {
        MatchState state = matches.get(matchId);
        if (state == null) return;

        Integer winnerId;
        String surrenderName;

        synchronized (state) {
            if (state.ended) return;
            state.ended = true;

            int aUserId = state.a.getUserId();
            int bUserId = state.b.getUserId();
            boolean aSurrender = (sender == state.a);
            surrenderName = sender.getUsername();
            winnerId = aSurrender ? bUserId : aUserId;
        }

        // gửi thông báo có verdict riêng cho mỗi bên
        String msgA = "MATCH_ENDED_SURRENDER:" + matchId + ":" + winnerId + ":" + surrenderName + ":" +
                ((winnerId == state.a.getUserId()) ? "WIN" : "LOSE");
        String msgB = "MATCH_ENDED_SURRENDER:" + matchId + ":" + winnerId + ":" + surrenderName + ":" +
                ((winnerId == state.b.getUserId()) ? "WIN" : "LOSE");
        state.a.sendMessage(msgA);
        state.b.sendMessage(msgB);

        // kết sổ (users + leaderboard + MATCH_RESULT) và dọn state
        endMatchAndUpdateDB(matchId, state, winnerId);
    }

    // ====================== INNER STATE ======================
    private static class MatchState {
        final String matchId;
        final ClientHandler a;
        final ClientHandler b;

        int currentRound;
        String displayedText;

        int showMs;
        int answerMs;

        String aAnswer, bAnswer;
        Integer aTime, bTime;

        int aTotal = 0, bTotal = 0;

        volatile boolean ended = false;
        volatile boolean started = false;

        Integer aProposedRounds = null;
        Integer bProposedRounds = null;
        int finalRounds = Integer.MAX_VALUE; // 0/sinh tử ⇒ dùng MAX_VALUE
        boolean modeConfigured = false;

        MatchState(String matchId, ClientHandler a, ClientHandler b) {
            this.matchId = matchId;
            this.a = a;
            this.b = b;
        }
    }
}
