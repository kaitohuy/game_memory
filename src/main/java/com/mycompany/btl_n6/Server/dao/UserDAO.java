/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.btl_n6.Server.dao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import com.mycompany.btl_n6.Server.model.User;
/**
 *
 * @author Hi
 */
public class UserDAO {
    
    // Kiểm tra đăng nhập
    public User checkLogin(String username, String password) {
        User user = null;
        String sql = "SELECT * FROM users WHERE username = ? AND passwordHash = ?";
        try (Connection conn = ConnectionSQL.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hash(password)); // password ở DB nên hash (SHA-256)
            
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user = new User(
                        rs.getInt("userId"),
                        rs.getString("username"),
                        rs.getString("passwordHash"),
                        rs.getInt("totalScore"),
                        rs.getInt("totalWins"),
                        rs.getString("status")
                    );
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return user; // trả về null nếu không tồn tại
    }

    // Đăng ký user mới (lưu password đã hash)
    public boolean registerUser(String username, String password) {
        // First check if username already exists to provide clearer behavior
        if (usernameExists(username)) {
            return false;
        }

        String sql = "INSERT INTO users (username, passwordHash, totalScore, totalWins, status) VALUES (?, ?, 0, 0, 'OFFLINE')";
        try (Connection conn = ConnectionSQL.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hash(password));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // log error message for debugging
            System.err.println("UserDAO.registerUser failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Check whether a username already exists in users table.
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.usernameExists failed: " + e.getMessage());
            // On DB error, return false to let register attempt proceed and fail safely downstream
            return false;
        }
    }

    private String hash(String input) {
        if (input == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            // convert to hex
            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            String res = formatter.toString();
            formatter.close();
            return res;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
     public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = ConnectionSQL.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                User user = new User(
                        rs.getInt("userId"),
                        rs.getString("username"),
                        rs.getString("passwordHash"),
                        rs.getInt("totalScore"),
                        rs.getInt("totalWins"),
                        rs.getString("status")
                );
                users.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }
     
     public boolean updateStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE userId = ?";
        try (Connection conn = ConnectionSQL.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
     
    public boolean updateScoreAndWins(int userId, int score, int wins) {
        String sql = "UPDATE users SET totalScore = ?, totalWins = ? WHERE userId = ?";
        try (Connection conn = ConnectionSQL.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, score);
            stmt.setInt(2, wins);
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE userId = ?";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("userId"),
                            rs.getString("username"),
                            rs.getString("passwordHash"),
                            rs.getInt("totalScore"),
                            rs.getInt("totalWins"),
                            rs.getString("status")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Increment totals atomically
    public boolean incrementScoreAndWins(int userId, int addScore, int addWins) {
        String sql = "UPDATE users SET totalScore = totalScore + ?, totalWins = totalWins + ? WHERE userId = ?";
        try (Connection conn = ConnectionSQL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, addScore);
            ps.setInt(2, addWins);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

     
    public List<User> getRanking() {
        List<User> ranking = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY totalScore DESC, totalWins DESC";

        try (Connection conn = ConnectionSQL.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                User user = new User(
                        rs.getInt("userId"),
                        rs.getString("username"),
                        rs.getString("passwordHash"),
                        rs.getInt("totalScore"),
                        rs.getInt("totalWins"),
                        rs.getString("status")
                );
                ranking.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ranking;
    }
  
}
