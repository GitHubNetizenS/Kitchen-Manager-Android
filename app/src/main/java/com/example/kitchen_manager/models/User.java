package com.example.kitchen_manager.models;

import java.io.Serializable;

public class User implements Serializable {
    private int userId;
    private String username;
    private String phone;
    private String avatarUrl;
    private  String password;
    private boolean isAdmin;
    private String title;

    // 默认构造函数
    public User() {}
    public User(int userId, String username, String phone, String password,
                String avatarUrl, boolean isAdmin, String title) {
        this.userId = userId;
        this.username = username;
        this.phone = phone;
        this.password = password;
        this.avatarUrl = avatarUrl;
        this.isAdmin = isAdmin;
        this.title = title;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
}