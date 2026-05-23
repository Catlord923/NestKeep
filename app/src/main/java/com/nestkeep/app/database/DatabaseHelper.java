package com.nestkeep.app.database;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.nestkeep.app.models.User;

import com.nestkeep.app.utils.HashUtils;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "household_chore_scheduler.db";
    // Bump version to 2 - triggers onUpgrade which rebuilds tables cleanly
    private static final int DATABASE_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (" +
                "userId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "fullName TEXT NOT NULL, " +
                "email TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL)");

        db.execSQL("CREATE TABLE chores (" +
                "choreId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "description TEXT, " +
                "dueDate TEXT NOT NULL, " +
                "dueTime TEXT NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'PENDING', " +
                "userId INTEGER NOT NULL, " +
                "CONSTRAINT fk_chore_user FOREIGN KEY(userId) " +
                "REFERENCES users(userId) ON DELETE CASCADE ON UPDATE CASCADE)");

        db.execSQL("CREATE TABLE reminders (" +
                "reminderId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "reminderDateTime TEXT NOT NULL, " +
                "choreId INTEGER NOT NULL, " +
                "CONSTRAINT fk_reminder_chore FOREIGN KEY(choreId) " +
                "REFERENCES chores(choreId) ON DELETE CASCADE ON UPDATE CASCADE)");
    }

    // Tables are dropped in reverse dependency order to avoid foreign key violations.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS reminders");
        db.execSQL("DROP TABLE IF EXISTS chores");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    // User Methods

    /**
     * Inserts a new user and returns a fully populated User object on success,
     * or null if the insert failed (duplicate username/email).
     */
    public User insertUserAndReturn(String username, String fullName, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        String hashed = HashUtils.hashPassword(password);
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("fullName", fullName);
        values.put("email", email);
        values.put("password", hashed);
        long id = db.insert("users", null, values);
        if (id == -1) return null;
        return new User((int) id, username, fullName, email, hashed);
    }

    /**
     * Inserts a new user. Password is hashed with BCrypt (salted) before storage.
     * Production code should use insertUserAndReturn instead — this variant is kept
     * for tests that only need a row ID without the full User object.
     */
    @androidx.annotation.VisibleForTesting
    public long insertUser(String username, String fullName, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("fullName", fullName);
        values.put("email", email);
        values.put("password", HashUtils.hashPassword(password));
        return db.insert("users", null, values);
    }

    /**
     * Returns a User object if credentials are valid; null otherwise.
     * Cursor handling stays in the data layer - activities receive a model, not a cursor.
     * Email is looked up first, then BCrypt.verify() is called separately since
     * BCrypt hashes cannot be compared inside a SQL query.
     */
    public User authenticateUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM users WHERE email=?",
                new String[]{email}
        );
        if (cursor != null && cursor.moveToFirst()) {
            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            boolean verified = HashUtils.verifyPassword(password, storedHash);
            User user = null;
            if (verified) {
                user = new User(
                        cursor.getInt(cursor.getColumnIndexOrThrow("userId")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("fullName")),
                        cursor.getString(cursor.getColumnIndexOrThrow("email")),
                        storedHash
                );
            }
            cursor.close();
            return user; // null if password wrong
        }
        if (cursor != null) cursor.close();
        return null;
    }

    // Chore Methods

    public long insertChore(String title, String description, String dueDate,
                            String dueTime, String status, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("description", description);
        values.put("dueDate", dueDate);
        values.put("dueTime", dueTime);
        values.put("status", status);
        values.put("userId", userId);
        return db.insert("chores", null, values);
    }

    public Cursor getChoresByUserId(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM chores WHERE userId=? ORDER BY dueDate, dueTime",
                new String[]{String.valueOf(userId)}
        );
    }

    public Cursor getChoreById(int choreId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM chores WHERE choreId=?",
                new String[]{String.valueOf(choreId)}
        );
    }

    public String getChoreTitleById(int choreId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT title FROM chores WHERE choreId=?",
                new String[]{String.valueOf(choreId)}
        );
        String title = "Unknown Chore";
        if (cursor != null) {
            if (cursor.moveToFirst()) title = cursor.getString(0);
            cursor.close();
        }
        return title;
    }

    public int updateChore(int choreId, String title, String description,
                           String dueDate, String dueTime, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("description", description);
        values.put("dueDate", dueDate);
        values.put("dueTime", dueTime);
        values.put("status", status);
        return db.update("chores", values, "choreId=?", new String[]{String.valueOf(choreId)});
    }

    public int deleteChore(int choreId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("chores", "choreId=?", new String[]{String.valueOf(choreId)});
    }

    /** Deletes all completed chores for a user. Cascades to their reminders. */
    public int deleteCompletedChores(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("chores", "userId=? AND status=?",
                new String[]{String.valueOf(userId), "COMPLETED"});
    }

    /** Marks all pending chores for a user as completed. */
    public int markAllChoresCompleted(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", "COMPLETED");
        return db.update("chores", values, "userId=? AND status=?",
                new String[]{String.valueOf(userId), "PENDING"});
    }

    // Reminder Methods

    public long insertReminder(String reminderDateTime, int choreId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("reminderDateTime", reminderDateTime);
        values.put("choreId", choreId);
        return db.insert("reminders", null, values);
    }

    public Cursor getRemindersByChoreId(int choreId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM reminders WHERE choreId=?",
                new String[]{String.valueOf(choreId)}
        );
    }

    public int deleteRemindersByChoreId(int choreId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("reminders", "choreId=?", new String[]{String.valueOf(choreId)});
    }

    /**
     * Single JOIN query - fetches all reminders for a user with the chore title
     * included, so adapters need zero additional DB calls per row.
     */
    public Cursor getRemindersByUserId(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT r.reminderId, r.reminderDateTime, r.choreId, c.title AS choreTitle " +
                "FROM reminders r " +
                "INNER JOIN chores c ON r.choreId = c.choreId " +
                "WHERE c.userId=? " +
                "ORDER BY r.reminderDateTime",
                new String[]{String.valueOf(userId)}
        );
    }

    public int deleteReminder(int reminderId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("reminders", "reminderId=?", new String[]{String.valueOf(reminderId)});
    }
}
