package com.mycompany.btl_n6.Server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class LeaderboardDAO {

    // Use userId (int) as the key in leaderboard tables. Tables now reference users.userId.
    public boolean recordCompetitiveResult(int userId, int winsToAdd, int pointsToAdd) {
        String sql = "INSERT INTO leaderboard_competitive (userId, wins, points) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE wins = wins + VALUES(wins), points = points + VALUES(points)";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, winsToAdd);
            ps.setInt(3, pointsToAdd);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean recordSoloResult(int userId, int roundsWonToAdd, int pointsToAdd) {
        // Conditional upsert: only replace stored record if the new session's points are higher,
        // or if points equal and the new session has fewer roundsWon (better performance).
        String sql = "INSERT INTO leaderboard_solo (userId, roundsWon, points) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "points = IF(VALUES(points) > points, VALUES(points), points), "
                + "roundsWon = IF(VALUES(points) > points, VALUES(roundsWon), "
                + "IF(VALUES(points) = points AND VALUES(roundsWon) < roundsWon, VALUES(roundsWon), roundsWon))";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, roundsWonToAdd);
            ps.setInt(3, pointsToAdd);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("LeaderboardDAO.recordSoloResult failed: " + e.getMessage());
        }
        return false;
    }

    public List<String> getTopCompetitive(int limit) {
        List<String> out = new ArrayList<>();
        String sql = "SELECT u.username, lb.wins, lb.points FROM leaderboard_competitive lb JOIN users u ON u.userId = lb.userId ORDER BY lb.wins DESC, lb.points DESC LIMIT ?";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString("username") + " | W:" + rs.getInt("wins") + " P:" + rs.getInt("points"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public List<String> getTopSolo(int limit) {
        List<String> out = new ArrayList<>();
    // Order by points descending first, then roundsWon ascending (fewer rounds is better when points tie)
    String sql = "SELECT u.username, lb.roundsWon, lb.points FROM leaderboard_solo lb JOIN users u ON u.userId = lb.userId ORDER BY lb.points DESC, lb.roundsWon ASC LIMIT ?";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString("username") + " | R:" + rs.getInt("roundsWon") + " P:" + rs.getInt("points"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }
}
