package com.mycompany.btl_n6.Server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;


public class MatchDAO {
    public int createMatch(int playerA, int playerB) {
        String sql = "INSERT INTO matches (playerA, playerB) VALUES (?, ?)";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, playerA);
            ps.setInt(2, playerB);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean insertMatchRound(int matchId, int roundNumber, String displayedText, int length, int ttl,
                                    String aAnswer, Integer aTime, String bAnswer, Integer bTime,
                                    int aPoints, int bPoints) {
        String sql = "INSERT INTO match_rounds (matchId, roundNumber, displayedText, length, ttlSeconds, playerA_answer, playerA_timeMillis, playerB_answer, playerB_timeMillis, playerA_points, playerB_points) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, roundNumber);
            ps.setString(3, displayedText);
            ps.setInt(4, length);
            ps.setInt(5, ttl);
            ps.setString(6, aAnswer);
            if (aTime == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, aTime);
            ps.setString(8, bAnswer);
            if (bTime == null) ps.setNull(9, Types.INTEGER); else ps.setInt(9, bTime);
            ps.setInt(10, aPoints);
            ps.setInt(11, bPoints);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean finalizeMatch(int matchId, Integer winnerId) {
        String sql = "UPDATE matches SET endTime = CURRENT_TIMESTAMP, winner = ?, isActive = FALSE WHERE matchId = ?";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (winnerId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, winnerId);
            ps.setInt(2, matchId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
