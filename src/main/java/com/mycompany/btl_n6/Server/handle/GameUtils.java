/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.btl_n6.Server.handle;

/**
 *
 * @author Hi
 */
public class GameUtils {
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    public static int computeTtlForLength(int length) {
        int extra = Math.max(0, length - 4);
        int ttl = 2 + (extra + 1) / 2;
        return Math.min(Math.max(ttl, 2), 30);
    }
}