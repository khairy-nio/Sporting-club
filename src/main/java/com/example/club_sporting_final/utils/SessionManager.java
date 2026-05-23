package com.example.club_sporting_final.utils;

/**
 * Singleton session manager to hold the currently logged-in user's info.
 * Used for Role-Based Access Control across the application.
 */
public class SessionManager {

    private static SessionManager instance;

    private String username;
    private String role;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public void logout() {
        this.username = null;
        this.role = null;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    public boolean isLoggedIn() {
        return username != null && role != null;
    }
}
