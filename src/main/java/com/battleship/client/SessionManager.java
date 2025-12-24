package com.battleship.client;

public class SessionManager {
    private static SessionManager instance;

    private String username;
    private String authToken;

    private SessionManager() { }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public boolean isLoggedIn() {
        return username != null && authToken != null;
    }

    public void clear() {
        username = null;
        authToken = null;
    }
}