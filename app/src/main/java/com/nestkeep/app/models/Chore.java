package com.nestkeep.app.models;

import java.time.LocalDate;
import java.time.LocalTime;

public class Chore {
    private int choreId;
    private String title;
    private String description;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private Status status;
    private int userId;

    public Chore(int choreId, String title, String description, LocalDate dueDate, LocalTime dueTime, Status status, int userId) {
        this.choreId = choreId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.status = status;
        this.userId = userId;
    }

    public int getChoreId() { return choreId; }
    public void setChoreId(int choreId) { this.choreId = choreId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalTime getDueTime() { return dueTime; }
    public void setDueTime(LocalTime dueTime) { this.dueTime = dueTime; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}