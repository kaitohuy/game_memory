/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.btl_n6.Server.model;

import java.net.Socket;
import java.util.ArrayList;
/**
 *
 * @author Hi
 */
public class GameRoom {
    private String roomId;
    private final ArrayList<Socket> clientSockets;
    private int maxPlayers;
    private int targetScore;
    private String status;
    private final ArrayList<Player> listPlayer;
    private final ArrayList<Phase> listPhase;
    public GameRoom(int maxPlayers,String roomId, int targetScore) {
        this.targetScore = targetScore;
        this.clientSockets = new ArrayList<>();
        this.listPlayer = new ArrayList<>();
        this.listPhase = new ArrayList<>();
        this.roomId = roomId;
        this.maxPlayers = maxPlayers;
        this.status = "WAITING";
    }

    public ArrayList<Phase> getListPhase() {
        return listPhase;
    }

    public String getRoomId() {
        return roomId;
    }

    public ArrayList<Socket> getClientSockets() {
        return clientSockets;
    }

    public void addClient(Socket clientSocket) {
        synchronized (clientSockets) {
            clientSockets.add(clientSocket);
        }
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public boolean isFull() {
        return clientSockets.size() >= maxPlayers;
    }

    public ArrayList<Player> getListPlayer() {
        return listPlayer;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setTargetScore(int targetScore) {
        this.targetScore = targetScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
//    Xóa client khỏi phòng
    public void removeClient(Socket clientSocket) {
        synchronized (clientSockets) {
            clientSockets.remove(clientSocket);
        }
    }
}

