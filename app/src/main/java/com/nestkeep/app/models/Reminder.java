package com.nestkeep.app.models;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

public class Reminder {
    private int reminderId;
    private LocalDateTime reminderDateTime;
    private int choreId;
    private String choreTitle; // pre-fetched via JOIN to avoid per-row DB calls

    public Reminder(int reminderId, LocalDateTime reminderDateTime, int choreId) {
        this.reminderId = reminderId;
        this.reminderDateTime = reminderDateTime;
        this.choreId = choreId;
        this.choreTitle = "";
    }

    public Reminder(int reminderId, LocalDateTime reminderDateTime, int choreId, String choreTitle) {
        this.reminderId = reminderId;
        this.reminderDateTime = reminderDateTime;
        this.choreId = choreId;
        this.choreTitle = choreTitle;
    }

    public int getReminderId() { return reminderId; }
    public void setReminderId(int reminderId) { this.reminderId = reminderId; }

    public LocalDateTime getReminderDateTime() { return reminderDateTime; }
    public void setReminderDateTime(LocalDateTime reminderDateTime) { this.reminderDateTime = reminderDateTime; }

    public int getChoreId() { return choreId; }
    public void setChoreId(int choreId) { this.choreId = choreId; }

    public String getChoreTitle() { return choreTitle; }
    public void setChoreTitle(String choreTitle) { this.choreTitle = choreTitle; }

    public LocalDate getReminderDate() {
        return reminderDateTime != null ? reminderDateTime.toLocalDate() : null;
    }

    public LocalTime getReminderTime() {
        return reminderDateTime != null ? reminderDateTime.toLocalTime() : null;
    }
}
