package com.mycompany.btl_n6.Server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;


public class SoloDAO {
    public int createSession(int userId) {
        String sql = "INSERT INTO solo_sessions (userId, totalScore, roundsPlayed) VALUES (?, 0, 0)";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Persist a solo round and its computed points. Assumes schema has a points column in solo_rounds.
    public boolean insertRound(int sessionId, int roundNumber, String displayed, int length, int ttl, String answer, boolean correct, Integer timeMillis, int points) {
        String sql = "INSERT INTO solo_rounds (sessionId, roundNumber, displayedText, length, ttlSeconds, userAnswer, correct, timeTakenMillis, points) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, roundNumber);
            ps.setString(3, displayed);
            ps.setInt(4, length);
            ps.setInt(5, ttl);
            ps.setString(6, answer);
            ps.setBoolean(7, correct);
            if (timeMillis == null) ps.setNull(8, Types.INTEGER); else ps.setInt(8, timeMillis);
            ps.setInt(9, points);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean finalizeSession(int sessionId, int totalScore, int roundsPlayed) {
        String sql = "UPDATE solo_sessions SET endTime = CURRENT_TIMESTAMP, totalScore = ?, roundsPlayed = ? WHERE sessionId = ?";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, totalScore);
            ps.setInt(2, roundsPlayed);
            ps.setInt(3, sessionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Incrementally add points and rounds for a session (creates resilience to partial writes).
    public boolean addToSessionTotals(int sessionId, int addPoints, int addRounds) {
        String sql = "UPDATE solo_sessions SET totalScore = totalScore + ?, roundsPlayed = roundsPlayed + ? WHERE sessionId = ?";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, addPoints);
            ps.setInt(2, addRounds);
            ps.setInt(3, sessionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
