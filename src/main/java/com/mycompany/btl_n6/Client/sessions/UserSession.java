package com.mycompany.btl_n6.Client.sessions;

import com.mycompany.btl_n6.Server.model.User;

public class UserSession {

    private static UserSession instance;
    private User user;

    private UserSession(User user) {
        this.user = user;
    }

    public static void createSession(User user) {
        // nếu muốn cho phép ghi đè khi login lại, có thể bỏ if
        if (instance == null) {
            instance = new UserSession(user);
        }
    }

    public static UserSession getInstance() {
        return instance;
    }

    public User getUser() {
        return user;
    }

    public static void clearSession() {
        instance = null;
    }

    // ==== THÊM MỚI ====
    public static User getCurrentUser() {
        return (instance == null) ? null : instance.user;
    }

    public static String getCurrentUsername() {
        User u = getCurrentUser();
        return (u == null) ? null : u.getUsername();
    }
}
