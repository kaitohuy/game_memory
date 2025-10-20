package com.mycompany.btl_n6.Server.handle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mycompany.btl_n6.Server.Server;
import com.mycompany.btl_n6.Server.dao.MatchDAO;
import com.mycompany.btl_n6.Server.dao.SoloDAO;
import com.mycompany.btl_n6.Server.dao.UserDAO;
import com.mycompany.btl_n6.Server.model.User;

public class ClientHandler implements Runnable {

    private volatile String currentMatchId;
    private final Socket clientSocket;
    private static ArrayList<Socket> clientSockets;
    private User currentUser;
    private DataInputStream in;
    private DataOutputStream out;
    private volatile long lastPingMs = System.currentTimeMillis();
    private static final Map<String, ClientHandler> userHandlers = new ConcurrentHashMap<>();

    public ClientHandler(Socket socket, ArrayList<Socket> clientSockets) {
        this.clientSocket = socket;
        ClientHandler.clientSockets = clientSockets;
        Server.registerHandler(socket, this);
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            while (true) {
                String request = in.readUTF();
                if (request == null || request.isEmpty()) {
                    continue;
                }

                String[] command = request.split(":", 2);
                String action = command[0];
                System.out.println("Received: " + request + " from " + (currentUser != null ? currentUser.getUsername() : clientSocket.getRemoteSocketAddress()));

                switch (action) {
                    case "LOGIN" ->
                        handleLogin(command);
                    case "REGISTER" ->
                        handleRegister(command);
                    case "GET_USERS" ->
                        handleGetUsers();
                    case "GET_RANKING" ->
                        handleGetRanking();
                    case "LOGOUT" -> {
                        handleLogout();
                        return;
                    }
                    case "START_SOLO" ->
                        startSolo();
                    case "INVITE" ->
                        handleInvite(command);
                    case "INVITE_RESPONSE" ->
                        handleInviteResponse(command);
                    case "SOLO_SUBMIT" ->
                        SoloManager.handleSubmit(command, this);
                    case "SOLO_CONTINUE" ->
                        SoloManager.handleContinue(command, this);
                    case "MATCH_SUBMIT" ->
                        handleMatchSubmit(command);
                    case "MATCH_READY" ->
                        handleMatchReady(command);
                    case "STATUS" ->
                        updateStatus(command);
                    case "PING" -> {
                        touchLastPing();
                        sendMessage("PONG");
                    }
                    case "MATCH_SURRENDER" -> {
                        handleMatchSurrender(command);
                    }
                    case "MATCH_MODE" -> {
                        handleMatchMode(command);
                    }
                    default ->
                        System.out.println("Unknown action: " + action);
                }
            }
        } catch (IOException e) {
            System.err.println("Client ngắt kết nối: " + clientSocket.getRemoteSocketAddress());
        } finally {
            cleanup();
        }
    }

    private void handleLogin(String[] command) throws IOException {
        if (command.length < 2 || command[1].split(",").length < 2) {
            sendMessage("LOGIN:FAILURE:Định dạng không hợp lệ");
            return;
        }
        String[] data = command[1].split(",");
        UserDAO userDAO = new UserDAO();
        User user = userDAO.checkLogin(data[0], data[1]);
        if (user != null) {
            currentUser = user;
            userDAO.updateStatus(user.getUserId(), "ONLINE");
            currentUser.setStatus("ONLINE");
            userHandlers.put(user.getUsername(), this);
            sendMessage("LOGIN:SUCCESS:" + user.getUserId() + "," + user.getUsername() + ","
                    + user.getTotalScore() + "," + user.getTotalWins() + "," + user.getStatus());
            broadcastOnlineUsers();
        } else {
            sendMessage("LOGIN:FAILURE:Sai tên đăng nhập hoặc mật khẩu");
        }
    }

    private void handleRegister(String[] command) throws IOException {
        if (command.length < 2 || command[1].split(",").length < 2) {
            sendMessage("REGISTER:FAILURE:Định dạng không hợp lệ");
            return;
        }
        String[] data = command[1].split(",");
        UserDAO userDAO = new UserDAO();
        sendMessage(userDAO.registerUser(data[0].trim(), data[1].trim())
                ? "REGISTER:SUCCESS" : "REGISTER:FAILURE:Tên người dùng đã tồn tại hoặc lỗi cơ sở dữ liệu");
    }

    private void handleInvite(String[] command) throws IOException {
        if (command.length < 2 || currentUser == null) {
            return;
        }
        String targetName = command[1].trim();

        // chặn tự mời chính mình
        if (currentUser.getUsername().equalsIgnoreCase(targetName)) {
            sendMessage("INVITE:FAILURE:Bạn không thể mời chính mình.");
            return;
        }

        ClientHandler targetHandler = userHandlers.get(targetName);
        if (targetHandler == null || targetHandler.out == null) {
            sendMessage("INVITE:FAILURE:Người dùng không khả dụng.");
            return;
        }

        // chỉ mời khi BOTH ONLINE
        String myStatus = currentUser.getStatus() == null ? "OFFLINE" : currentUser.getStatus();
        String targetStatus = targetHandler.currentUser != null && targetHandler.currentUser.getStatus() != null
                ? targetHandler.currentUser.getStatus() : "OFFLINE";

        if (!"ONLINE".equalsIgnoreCase(myStatus)) {
            sendMessage("INVITE:FAILURE:Bạn hiện không ở trạng thái ONLINE.");
            return;
        }
        if (!"ONLINE".equalsIgnoreCase(targetStatus)) {
            sendMessage("INVITE:FAILURE:Đối thủ không ở trạng thái ONLINE.");
            return;
        }

        // ok -> chuyển lời mời
        targetHandler.sendMessage("INVITE_FROM:" + currentUser.getUsername());
    }

    // --- INVITE_RESPONSE ---
    private void handleInviteResponse(String[] command) throws IOException {
        if (command.length < 2 || command[1].split(":").length < 2) {
            return;
        }
        String[] parts = command[1].split(":");
        ClientHandler inviterHandler = userHandlers.get(parts[0]);
        if (inviterHandler == null) {
            return;
        }

        if ("OK".equalsIgnoreCase(parts[1])) {
            // kiểm tra lại 2 bên vẫn ONLINE trước khi tạo trận
            if (!"ONLINE".equalsIgnoreCase(safeStatus(inviterHandler.currentUser))
                    || !"ONLINE".equalsIgnoreCase(safeStatus(currentUser))) {
                inviterHandler.sendMessage("INVITE:FAILURE:Một trong hai người chơi không còn ONLINE.");
                sendMessage("INVITE:FAILURE:Một trong hai người chơi không còn ONLINE.");
                return;
            }

            MatchDAO daoMatch = new MatchDAO();
            int matchIdInt = daoMatch.createMatch(inviterHandler.currentUser.getUserId(), currentUser.getUserId());
            if (matchIdInt <= 0) {
                inviterHandler.sendMessage("INVITE:FAILURE:Lỗi cơ sở dữ liệu.");
                return;
            }
            String matchId = String.valueOf(matchIdInt);
            inviterHandler.currentMatchId = matchId;
            this.currentMatchId = matchId;

            UserDAO dao = new UserDAO();
            dao.updateStatus(inviterHandler.currentUser.getUserId(), "BUSY");
            dao.updateStatus(currentUser.getUserId(), "BUSY");
            // cập nhật bộ nhớ để kiểm tra trạng thái lần sau
            inviterHandler.currentUser.setStatus("BUSY");
            currentUser.setStatus("BUSY");

            MatchManager.createMatch(matchId, inviterHandler, this);
            String matchMsg = "MATCH_START:" + matchId + ":" + parts[0] + "," + currentUser.getUsername();
            inviterHandler.sendMessage(matchMsg);
            sendMessage(matchMsg);
            inviterHandler.sendMessage("MATCH_MODE_REQUEST:" + matchId);
            sendMessage("MATCH_MODE_REQUEST:" + matchId);
        } else {
            inviterHandler.sendMessage("INVITE_REJECTED:" + currentUser.getUsername());
        }
    }

    private String safeStatus(User u) {
        return (u == null || u.getStatus() == null) ? "OFFLINE" : u.getStatus();
    }

    private void handleGetUsers() throws IOException {
        UserDAO userDAO = new UserDAO();
        List<User> users = userDAO.getAllUsers();
        StringBuilder sb = new StringBuilder("ONLINE_USERS:");
        for (User u : users) {
            sb.append(u.getUsername()).append(",").append(u.getTotalScore()).append(",").append(u.getStatus()).append(";");
        }
        if (!users.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sendMessage(sb.toString());
    }

    private void handleGetRanking() throws IOException {
        // Send competitive ranking (by wins then points)
        try {
            com.mycompany.btl_n6.Server.dao.LeaderboardDAO lb = new com.mycompany.btl_n6.Server.dao.LeaderboardDAO();
            java.util.List<String> comp = lb.getTopCompetitive(50);
            StringBuilder sbc = new StringBuilder("RANKING_COMPETITIVE:");
            for (String line : comp) {
                // each line formatted as "name | W:x P:y" -> convert to payload name,points,wins
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    String rest = parts[1].trim();
                    // parse W and P values loosely
                    int wins = 0, points = 0;
                    try {
                        String[] kv = rest.split("\\s+");
                        for (String k : kv) {
                            if (k.startsWith("W:")) {
                                wins = Integer.parseInt(k.substring(2));
                            }
                            if (k.startsWith("P:")) {
                                points = Integer.parseInt(k.substring(2));
                            }
                        }
                    } catch (Exception ex) {
                    }
                    sbc.append(name).append(",").append(points).append(",").append(wins).append(";");
                }
            }
            if (sbc.length() > "RANKING_COMPETITIVE:".length()) {
                sbc.deleteCharAt(sbc.length() - 1);
            }
            sendMessage(sbc.toString());
        } catch (Exception e) {
            // fallback: send userDAO ranking
            UserDAO userDAO = new UserDAO();
            List<User> ranking = userDAO.getRanking();
            StringBuilder sb = new StringBuilder("RANKING:");
            for (User u : ranking) {
                sb.append(u.getUsername()).append(",").append(u.getTotalScore()).append(",").append(u.getTotalWins()).append(";");
            }
            if (!ranking.isEmpty()) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sendMessage(sb.toString());
        }

        // Send solo ranking
        try {
            com.mycompany.btl_n6.Server.dao.LeaderboardDAO lb = new com.mycompany.btl_n6.Server.dao.LeaderboardDAO();
            java.util.List<String> solo = lb.getTopSolo(50);
            StringBuilder sbs = new StringBuilder("RANKING_SOLO:");
            for (String line : solo) {
                // expected "name | R:x P:y"
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    String rest = parts[1].trim();
                    int rounds = 0, points = 0;
                    try {
                        String[] kv = rest.split("\s+");
                        for (String k : kv) {
                            if (k.startsWith("R:")) {
                                rounds = Integer.parseInt(k.substring(2));
                            }
                            if (k.startsWith("P:")) {
                                points = Integer.parseInt(k.substring(2));
                            }
                        }
                    } catch (Exception ex) {
                    }
                    sbs.append(name).append(",").append(points).append(",").append(rounds).append(";");
                }
            }
            if (sbs.length() > "RANKING_SOLO:".length()) {
                sbs.deleteCharAt(sbs.length() - 1);
            }
            sendMessage(sbs.toString());
        } catch (Exception ex) {
            // ignore solo ranking failure
        }
    }

    private void handleLogout() throws IOException {
        if (currentUser != null) {
            UserDAO dao = new UserDAO();
            dao.updateStatus(currentUser.getUserId(), "OFFLINE");
            userHandlers.remove(currentUser.getUsername());
            broadcastOnlineUsers();
        }
        sendMessage("LOGOUT:SUCCESS");
    }

    private void startSolo() {
        String sessionId = currentUser != null ? generateSessionId() : "guest_" + System.currentTimeMillis();
        if (currentUser != null) {
            currentUser.setStatus("BUSY");
            new UserDAO().updateStatus(currentUser.getUserId(), "BUSY");
            broadcastOnlineUsers();
        }
        SoloManager.startSolo(sessionId, this);
    }

    private String generateSessionId() {
        try {
            SoloDAO sdao = new SoloDAO();
            int sid = sdao.createSession(currentUser.getUserId());
            return sid > 0 ? String.valueOf(sid) : currentUser.getUsername() + "_" + System.currentTimeMillis();
        } catch (Exception e) {
            return currentUser.getUsername() + "_" + System.currentTimeMillis();
        }
    }

    private void handleMatchSubmit(String[] command) {
        if (command.length < 2 || command[1].split(":", 4).length < 4) {
            return;
        }
        String[] parts = command[1].split(":", 4);
        String matchId = parts[0];
        int roundNumber = parseIntSafe(parts[1]);
        String answer = parts[2];
        int timeMillis = parseIntSafe(parts[3]);
        MatchManager.submitAnswer(matchId, this, answer, timeMillis);
    }

    private void handleMatchReady(String[] command) {
        if (command.length < 2) {
            return;
        }
        System.out.println("[DEBUG] Received MATCH_READY for match " + command[1].trim() + " from " + (currentUser != null ? currentUser.getUsername() : "unknown"));
        MatchManager.clientReady(command[1].trim(), this);
    }

    private void updateStatus(String[] command) {
        if (command.length < 2 || currentUser == null) {
            return;
        }
        String newStatus = command[1].split(",")[0].trim();
        new UserDAO().updateStatus(currentUser.getUserId(), newStatus);
        currentUser.setStatus(newStatus);                // <<< THÊM DÒNG NÀY
        broadcastOnlineUsers();
    }

    private void cleanup() {
        try {
            // 1) Nếu đang trong trận -> coi như đầu hàng TRƯỚC
            if (currentMatchId != null) {
                MatchManager.surrender(currentMatchId, this);
                currentMatchId = null;
            }

            // 2) Đóng stream/socket như cũ
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (clientSocket != null) {
                clientSockets.remove(clientSocket);
                clientSocket.close();
                Server.unregisterHandler(clientSocket);

                // 3) Cuối cùng cập nhật trạng thái user này -> OFFLINE
                if (currentUser != null) {
                    new UserDAO().updateStatus(currentUser.getUserId(), "OFFLINE");
                    userHandlers.remove(currentUser.getUsername());
                    broadcastOnlineUsers();
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng tài nguyên: " + e.getMessage());
        }
    }

    private void broadcastOnlineUsers() {
        UserDAO userDAO = new UserDAO();
        List<User> users = userDAO.getAllUsers();
        StringBuilder sb = new StringBuilder("ONLINE_USERS:");
        for (User u : users) {
            sb.append(u.getUsername()).append(",").append(u.getTotalScore()).append(",").append(u.getStatus()).append(";");
        }
        if (!users.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        String msg = sb.toString();
        userHandlers.values().forEach(h -> h.sendMessage(msg));
    }

    /**
     * Broadcast the current solo leaderboard to all connected clients.
     */
    public static void broadcastSoloRanking() {
        try {
            com.mycompany.btl_n6.Server.dao.LeaderboardDAO lb = new com.mycompany.btl_n6.Server.dao.LeaderboardDAO();
            java.util.List<String> solo = lb.getTopSolo(50);
            StringBuilder sbs = new StringBuilder("RANKING_SOLO:");
            for (String line : solo) {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    String rest = parts[1].trim();
                    int rounds = 0, points = 0;
                    try {
                        String[] kv = rest.split("\\s+");
                        for (String k : kv) {
                            if (k.startsWith("R:")) {
                                rounds = Integer.parseInt(k.substring(2));
                            }
                            if (k.startsWith("P:")) {
                                points = Integer.parseInt(k.substring(2));
                            }
                        }
                    } catch (Exception ex) {
                    }
                    sbs.append(name).append(",").append(points).append(",").append(rounds).append(";");
                }
            }
            if (sbs.length() > "RANKING_SOLO:".length()) {
                sbs.deleteCharAt(sbs.length() - 1);
            }
            String msg = sbs.toString();
            userHandlers.values().forEach(h -> h.sendMessage(msg));
        } catch (Exception ex) {
            System.err.println("Failed to broadcast solo ranking: " + ex.getMessage());
        }
    }

    public synchronized void sendMessage(String msg) {
        try {
            if (out != null) {
                System.out.println("[DEBUG] Sending message to " + (currentUser != null ? currentUser.getUsername() : clientSocket.getRemoteSocketAddress()) + ": " + msg);
                out.writeUTF(msg);
                out.flush();
            } else {
                System.err.println("[ERROR] Output stream is null for " + (currentUser != null ? currentUser.getUsername() : clientSocket.getRemoteSocketAddress()));
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to send message '" + msg + "' to " + (currentUser != null ? currentUser.getUsername() : clientSocket.getRemoteSocketAddress()) + ": " + e.getMessage());
        }
    }

    public void disconnectForTimeout() {
        sendMessage("DISCONNECT:TIMEOUT");
        cleanup();
    }

    public void touchLastPing() {
        lastPingMs = System.currentTimeMillis();
    }

    public long getLastPingMs() {
        return lastPingMs;
    }

    public int getUserId() {
        return currentUser != null ? currentUser.getUserId() : -1;
    }

    public String getUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void handleMatchSurrender(String[] command) {
        String matchId = (command.length >= 2 && !command[1].isBlank())
                ? command[1].trim()
                : currentMatchId;
        if (matchId == null || matchId.isEmpty()) {
            return;
        }

        MatchManager.surrender(matchId, this);
        currentMatchId = null;
    }

    private void handleMatchMode(String[] command) {
        if (command.length < 2) {
            return;
        }
        String[] parts = command[1].split(":");
        if (parts.length < 2) {
            return;
        }

        String matchId = parts[0].trim();
        int rounds = parseIntSafe(parts[1]);
        if (matchId.isEmpty()) {
            return;
        }

        this.currentMatchId = matchId;
        MatchManager.receiveMode(matchId, this, rounds);
    }

}
