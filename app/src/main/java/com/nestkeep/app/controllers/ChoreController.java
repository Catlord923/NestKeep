package com.nestkeep.app.controllers;

import android.content.Context;
import android.database.Cursor;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Data;

import com.nestkeep.app.workers.ReminderNotificationWorker;
import com.nestkeep.app.database.DatabaseHelper;
import com.nestkeep.app.models.Reminder;
import com.nestkeep.app.models.Status;
import com.nestkeep.app.models.Chore;

import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.time.Duration;
import java.util.List;

public class ChoreController {

    private final DatabaseHelper dbHelper;
    private final Context context;

    public ChoreController(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = new DatabaseHelper(this.context);
    }

    // Returns false for null chore, null/empty title, null due date, or null due time.
    // Description is optional and not checked here.
    public boolean validateInput(Chore choreData) {
        if (choreData == null) return false;
        if (choreData.getTitle() == null || choreData.getTitle().trim().isEmpty()) return false;
        if (choreData.getDueDate() == null) return false;
        if (choreData.getDueTime() == null) return false;
        return true;
    }

    /**
     * Creates a chore after validating input.
     * Returns the new row ID, or -1 if validation fails.
     */
    public long createChore(Chore choreData) {
        if (!validateInput(choreData)) return -1;
        return dbHelper.insertChore(
                choreData.getTitle(),
                choreData.getDescription(),
                choreData.getDueDate().toString(),
                choreData.getDueTime().toString(),
                choreData.getStatus().name(),
                choreData.getUserId()
        );
    }

    /**
     * Creates a reminder and schedules a WorkManager notification for it.
     * If the reminder time is in the past, it is saved but not scheduled.
     */
    public void createReminder(Reminder reminderData, long choreId, String choreTitle) {
        long reminderId = dbHelper.insertReminder(
                reminderData.getReminderDateTime().toString(),
                (int) choreId
        );
        if (reminderId != -1) {
            scheduleReminderNotification(
                    reminderData.getReminderDateTime(), choreTitle,
                    (int) choreId, (int) reminderId);
        }
    }

    // Public so BootReceiver can reschedule after device reboot.
    // Tags the work request by choreId so all reminders for a chore
    // can be canceled together when the chore is deleted.
    public void scheduleReminderNotification(LocalDateTime reminderDateTime,
                                               String choreTitle, int choreId, int reminderId) {
        LocalDateTime now = LocalDateTime.now();
        if (!reminderDateTime.isAfter(now)) return; // don't schedule past reminders

        long delaySeconds = Duration.between(now, reminderDateTime).getSeconds();

        Data inputData = new Data.Builder()
                .putString(ReminderNotificationWorker.KEY_CHORE_TITLE, choreTitle)
                .putInt(ReminderNotificationWorker.KEY_CHORE_ID, choreId)
                .putInt(ReminderNotificationWorker.KEY_REMINDER_ID, reminderId)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(
                ReminderNotificationWorker.class)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag("reminder_" + choreId)
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);
    }

    public void deleteReminder(int reminderId) {
        dbHelper.deleteReminder(reminderId);
    }

    /** Cancels all pending WorkManager notifications for a chore. */
    public void cancelReminderNotifications(int choreId) {
        WorkManager.getInstance(context).cancelAllWorkByTag("reminder_" + choreId);
    }

    /** Fetches chores for a user. Pass null filterStatus to get all. */
    public List<Chore> getChoresForUser(int userId, Status filterStatus) {
        List<Chore> chores = new ArrayList<>();
        Cursor cursor = dbHelper.getChoresByUserId(userId);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Status currentStatus = Status.valueOf(
                        cursor.getString(cursor.getColumnIndexOrThrow("status")));
                if (filterStatus == null || currentStatus == filterStatus) {
                    chores.add(new Chore(
                            cursor.getInt(cursor.getColumnIndexOrThrow("choreId")),
                            cursor.getString(cursor.getColumnIndexOrThrow("title")),
                            cursor.getString(cursor.getColumnIndexOrThrow("description")),
                            LocalDate.parse(cursor.getString(cursor.getColumnIndexOrThrow("dueDate"))),
                            LocalTime.parse(cursor.getString(cursor.getColumnIndexOrThrow("dueTime"))),
                            currentStatus,
                            cursor.getInt(cursor.getColumnIndexOrThrow("userId"))
                    ));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return chores;
    }

    public Chore getChoreById(int choreId) {
        Cursor cursor = dbHelper.getChoreById(choreId);
        Chore chore = null;
        if (cursor != null && cursor.moveToFirst()) {
            chore = new Chore(
                    cursor.getInt(cursor.getColumnIndexOrThrow("choreId")),
                    cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    LocalDate.parse(cursor.getString(cursor.getColumnIndexOrThrow("dueDate"))),
                    LocalTime.parse(cursor.getString(cursor.getColumnIndexOrThrow("dueTime"))),
                    Status.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                    cursor.getInt(cursor.getColumnIndexOrThrow("userId"))
            );
            cursor.close();
        }
        return chore;
    }

    public List<Reminder> getRemindersForChore(int choreId) {
        List<Reminder> list = new ArrayList<>();
        Cursor cursor = dbHelper.getRemindersByChoreId(choreId);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(new Reminder(
                        cursor.getInt(cursor.getColumnIndexOrThrow("reminderId")),
                        LocalDateTime.parse(cursor.getString(
                                cursor.getColumnIndexOrThrow("reminderDateTime"))),
                        cursor.getInt(cursor.getColumnIndexOrThrow("choreId"))
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    /**
     * Single JOIN query - choreTitle is pre-populated, no per-row DB calls needed.
     */
    public List<Reminder> getRemindersForUser(int userId) {
        List<Reminder> list = new ArrayList<>();
        Cursor cursor = dbHelper.getRemindersByUserId(userId);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(new Reminder(
                        cursor.getInt(cursor.getColumnIndexOrThrow("reminderId")),
                        LocalDateTime.parse(cursor.getString(
                                cursor.getColumnIndexOrThrow("reminderDateTime"))),
                        cursor.getInt(cursor.getColumnIndexOrThrow("choreId")),
                        cursor.getString(cursor.getColumnIndexOrThrow("choreTitle"))
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public boolean updateChoreFull(Chore chore) {
        if (!validateInput(chore)) return false;
        int rows = dbHelper.updateChore(
                chore.getChoreId(),
                chore.getTitle(),
                chore.getDescription(),
                chore.getDueDate().toString(),
                chore.getDueTime().toString(),
                chore.getStatus().name()
        );
        if (rows > 0 && chore.getStatus() == Status.COMPLETED) {
            cancelReminderNotifications(chore.getChoreId());
            dbHelper.deleteRemindersByChoreId(chore.getChoreId());
        }
        return rows > 0;
    }

    public void deleteChore(int choreId) {
        cancelReminderNotifications(choreId);
        dbHelper.deleteChore(choreId);
    }

    /** Deletes all completed chores for a user, cancelling their notifications. */
    public void deleteCompletedChores(int userId) {
        // Cancel notifications for each completed chore before bulk delete
        List<Chore> completed = getChoresForUser(userId, Status.COMPLETED);
        for (Chore chore : completed) {
            cancelReminderNotifications(chore.getChoreId());
        }
        dbHelper.deleteCompletedChores(userId);
    }

    /** Marks all pending chores for a user as completed. */
    public void markAllChoresCompleted(int userId) {
        dbHelper.markAllChoresCompleted(userId);
    }
}
