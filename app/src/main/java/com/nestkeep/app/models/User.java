package com.nestkeep.app.models;

public class User {
    private int userId;
    private String username;
    private String fullName;
    private String email;
    private String password;

    public User(int userId, String username, String fullName, String email, String password) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    public int getUserId() { return userId; }

    public String getUsername() { return username; }

    public String getFullName() { return fullName; }

    public String getEmail() { return email; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }
}