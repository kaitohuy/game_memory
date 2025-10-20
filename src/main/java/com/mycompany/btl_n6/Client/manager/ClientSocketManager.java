package com.mycompany.btl_n6.Client.manager;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import com.mycompany.btl_n6.Client.ClientSocket;

public class ClientSocketManager {
    private ClientSocket clientSocket;
    private Thread receiveThread;
    private Thread heartbeatThread;
    private volatile boolean running = true;
    private final CopyOnWriteArrayList<MessageListener> listeners = new CopyOnWriteArrayList<>();

    public ClientSocketManager(ClientSocket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void startReceivingMessages() {
        receiveThread = new Thread(() -> {
            try {
                while (running) {
                    String message = clientSocket.receiveMessage();
                    System.out.println("Mess: " + message);
                    if (message != null) {
                        notifyListeners(message);
                    } else {
                        running = false;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeClientSocket();
            }
        }, "Client-Receiver");
        receiveThread.setDaemon(true);
        receiveThread.start();

        heartbeatThread = new Thread(() -> {
            try {
                while (running) {
                    try {
                        clientSocket.sendMessage("PING");
                    } catch (IOException ex) {
                        
                    }
                    Thread.sleep(30_000);
                }
            } catch (InterruptedException ie) {
                // exit
            }
        }, "Client-Heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    public void stopReceivingMessages() {
        running = false;
        if (receiveThread != null) receiveThread.interrupt();
        if (heartbeatThread != null) heartbeatThread.interrupt();
        closeClientSocket();
        listeners.clear();
    }

    public void addMessageListener(MessageListener listener) {
        if (listener != null) listeners.addIfAbsent(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String message) {
        for (MessageListener listener : listeners) {
            try {
                listener.onMessageReceived(message);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void closeClientSocket() {
        try {
            if (clientSocket != null && !clientSocket.getSocket().isClosed()) {
                clientSocket.getSocket().close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
