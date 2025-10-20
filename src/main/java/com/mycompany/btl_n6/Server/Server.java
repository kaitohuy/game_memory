/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.btl_n6.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.mycompany.btl_n6.Server.handle.ClientHandler;
/**
 *
 * @author Hi
 */
public class Server {
    private int port;
    private static ArrayList<Socket> clientSockets = new ArrayList<>();
    // map socket -> handler for lifecycle management (watchdog)
    private static final Map<Socket, ClientHandler> handlerMap = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
    // timeout configuration (ms)
    private static final long CLIENT_TIMEOUT_MS = 90_000; // 90 seconds
    private static final long WATCHDOG_INTERVAL_MS = 30_000; // check every 30s

    public Server(int port) {
        this.port = port;
    }
    
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            // start watchdog to clean up stale connections
            watchdog.scheduleAtFixedRate(() -> {
                try {
                    long now = System.currentTimeMillis();
                    for (Map.Entry<Socket, ClientHandler> e : handlerMap.entrySet()) {
                        ClientHandler h = e.getValue();
                        if (h == null) continue;
                        long last = h.getLastPingMs();
                        if (now - last > CLIENT_TIMEOUT_MS) {
                            System.out.println("Disconnecting stale client: " + e.getKey().getRemoteSocketAddress());
                            try {
                                h.disconnectForTimeout();
                            } catch (Exception ex) {
                                // ignore
                            }
                        }
                    }
                } catch (Exception ex) {
                    // ignore watchdog errors
                }
            }, WATCHDOG_INTERVAL_MS, WATCHDOG_INTERVAL_MS, TimeUnit.MILLISECONDS);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Chấp nhận kết nối từ client
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                  
                // Thêm clientSocket vào danh sách clientSockets
                synchronized (clientSockets) {
                    clientSockets.add(clientSocket);
                }
                
                // Tạo ClientHandler để xử lý client trên một thread riêng
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientSockets);
                handlerMap.put(clientSocket, clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // called by ClientHandler constructor to register itself
    public static void registerHandler(Socket socket, ClientHandler handler) {
        if (socket != null && handler != null) handlerMap.put(socket, handler);
    }

    public static void unregisterHandler(Socket socket) {
        if (socket != null) handlerMap.remove(socket);
    }
}
