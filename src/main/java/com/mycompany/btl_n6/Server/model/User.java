/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.btl_n6.Server.model;

import java.io.Serializable;

public class User implements Serializable {
    private int userid;
    private String username;
    private String password;
    private int totalScore;
    private int totalWins;
    private String status;

    public User() {
    }

    public User(int userid, String username, String password, int totalScore, int totalWins, String status) {
        this.userid = userid;
        this.username = username;
        this.password = password;
        this.totalScore = totalScore;
        this.totalWins = totalWins;
        this.status = status;
    }

    // Getter - Setter
    public int getUserId() {
        return userid;
    }

    public void setUserId(int userid) {
        this.userid = userid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}