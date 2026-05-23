package com.nestkeep.app.database;

import android.content.Context;
import android.database.Cursor;

import com.nestkeep.app.models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class DatabaseHelperTest {

    private DatabaseHelper dbHelper;

    // Helpers for readability
    private static final String USERNAME   = "testuser";
    private static final String FULL_NAME  = "Test User";
    private static final String EMAIL      = "test@example.com";
    private static final String PASSWORD   = "password123";

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        // Use null name for in-memory database - wiped after each test
        dbHelper = new DatabaseHelper(context);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    
    // User tests
    

    @Test
    public void insertUser_validData_returnsPositiveId() {
        long id = dbHelper.insertUser(USERNAME, FULL_NAME, EMAIL, PASSWORD);
        assertTrue(id > 0);
    }

    @Test
    public void insertUser_duplicateUsername_returnsMinusOne() {
        dbHelper.insertUser(USERNAME, FULL_NAME, EMAIL, PASSWORD);
        long id = dbHelper.insertUser(USERNAME, "Other Name", "other@example.com", PASSWORD);
        assertEquals(-1, id);
    }

    @Test
    public void insertUser_duplicateEmail_returnsMinusOne() {
        dbHelper.insertUser(USERNAME, FULL_NAME, EMAIL, PASSWORD);
        long id = dbHelper.insertUser("otheruser", "Other Name", EMAIL, PASSWORD);
        assertEquals(-1, id);
    }

    @Test
    public void insertUserAndReturn_validData_returnsUserWithCorrectFields() {
        User user = dbHelper.insertUserAndReturn(USERNAME, FULL_NAME, EMAIL, PASSWORD);
        assertNotNull(user);
        assertEquals(USERNAME, user.getUsername());
        assertEquals(FULL_NAME, user.getFullName());
        assertEquals(EMAIL, user.getEmail());
        assertTrue(user.getUserId() > 0);
    }

    @Test
    public void insertUserAndReturn_duplicateEmail_returnsNull() {
        dbHelper.insertUserAndReturn(USERNAME, FULL_NAME, EMAIL, PASSWORD);
        User user = dbHelper.insertUserAndReturn("otheruser", "Other", EMAIL, PASSWORD);
        assertNull(user);
    }

    @Test
    public void authenticateUser_correctCredentials_returnsUser() {
        dbHelper.insertUser(USERNAME, FULL_NAME, EMAIL, PASSWORD);
        User user = dbHelper.authenticateUser(EMAIL, PASSWORD);
        assertNotNull(user);
        assertEquals(USERNAME, user.getUsername());
        assertEquals(EMAIL, user.getEmail());
    }

    @Test
    public void authenticateUser_wrongPassword_returnsNull() {
        dbHelper.insertUser(USERNAME, FULL_NAME, EMAIL, PASSWORD);
        User user = dbHelper.authenticateUser(EMAIL, "wrongpassword");
        assertNull(user);
    }

    @Test
    public void authenticateUser_wrongEmail_returnsNull() {
        dbHelper.insertUser(USERNAME, FULL_NAME, EMAIL, PASSWORD);
        User user = dbHelper.authenticateUser("wrong@example.com", PASSWORD);
        assertNull(user);
    }

    @Test
    public void authenticateUser_nonExistentUser_returnsNull() {
        User user = dbHelper.authenticateUser(EMAIL, PASSWORD);
        assertNull(user);
    }

    @Test
    public void authenticateUser_passwordHashNotStoredInPlaintext() {
        dbHelper.insertUser(USERNAME, FULL_NAME, EMAIL, PASSWORD);
        User user = dbHelper.authenticateUser(EMAIL, PASSWORD);
        assertNotNull(user);
        // The stored password should be a BCrypt hash, not the original
        assertNotEquals(PASSWORD, user.getPassword());
        assertTrue("Stored password should be a BCrypt hash",
                user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"));
    }

    
    // Chore tests
    

    private long insertTestUser() {
        return dbHelper.insertUser(USERNAME, FULL_NAME, EMAIL, PASSWORD);
    }

    private long insertTestChore(int userId) {
        return dbHelper.insertChore("Clean kitchen", "Scrub the hob",
                LocalDate.now().plusDays(1).toString(),
                LocalTime.of(10, 0).toString(),
                "PENDING", userId);
    }

    @Test
    public void insertChore_validData_returnsPositiveId() {
        long userId = insertTestUser();
        long choreId = insertTestChore((int) userId);
        assertTrue(choreId > 0);
    }

    @Test
    public void getChoresByUserId_noChores_returnsEmptyCursor() {
        long userId = insertTestUser();
        Cursor cursor = dbHelper.getChoresByUserId((int) userId);
        assertNotNull(cursor);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void getChoresByUserId_withChores_returnsCorrectCount() {
        long userId = insertTestUser();
        insertTestChore((int) userId);
        insertTestChore((int) userId);
        Cursor cursor = dbHelper.getChoresByUserId((int) userId);
        assertEquals(2, cursor.getCount());
        cursor.close();
    }

    @Test
    public void getChoresByUserId_onlyReturnsChoresForThatUser() {
        long userId1 = dbHelper.insertUser("user1", "User One", "user1@test.com", PASSWORD);
        long userId2 = dbHelper.insertUser("user2", "User Two", "user2@test.com", PASSWORD);
        insertTestChore((int) userId1);
        insertTestChore((int) userId2);
        insertTestChore((int) userId2);

        Cursor cursor = dbHelper.getChoresByUserId((int) userId1);
        assertEquals(1, cursor.getCount());
        cursor.close();
    }

    @Test
    public void getChoreById_existingChore_returnsCursor() {
        long userId = insertTestUser();
        long choreId = insertTestChore((int) userId);
        Cursor cursor = dbHelper.getChoreById((int) choreId);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals("Clean kitchen",
                cursor.getString(cursor.getColumnIndexOrThrow("title")));
        cursor.close();
    }

    @Test
    public void getChoreById_nonExistentChore_returnsEmptyCursor() {
        Cursor cursor = dbHelper.getChoreById(9999);
        assertNotNull(cursor);
        assertFalse(cursor.moveToFirst());
        cursor.close();
    }

    @Test
    public void updateChore_existingChore_returnsOneRowAffected() {
        long userId = insertTestUser();
        long choreId = insertTestChore((int) userId);
        int rows = dbHelper.updateChore((int) choreId, "Updated title", "New desc",
                LocalDate.now().plusDays(2).toString(),
                LocalTime.of(12, 0).toString(), "COMPLETED");
        assertEquals(1, rows);
    }

    @Test
    public void updateChore_nonExistentChore_returnsZeroRows() {
        int rows = dbHelper.updateChore(9999, "Title", "Desc",
                LocalDate.now().toString(), LocalTime.now().toString(), "PENDING");
        assertEquals(0, rows);
    }

    @Test
    public void deleteChore_existingChore_removesIt() {
        long userId = insertTestUser();
        long choreId = insertTestChore((int) userId);
        dbHelper.deleteChore((int) choreId);
        Cursor cursor = dbHelper.getChoreById((int) choreId);
        assertFalse(cursor.moveToFirst());
        cursor.close();
    }

    @Test
    public void deleteCompletedChores_onlyDeletesCompletedOnes() {
        long userId = insertTestUser();
        dbHelper.insertChore("Pending chore", "", LocalDate.now().plusDays(1).toString(),
                LocalTime.of(10, 0).toString(), "PENDING", (int) userId);
        dbHelper.insertChore("Completed chore", "", LocalDate.now().plusDays(1).toString(),
                LocalTime.of(11, 0).toString(), "COMPLETED", (int) userId);

        dbHelper.deleteCompletedChores((int) userId);

        Cursor cursor = dbHelper.getChoresByUserId((int) userId);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("PENDING", cursor.getString(cursor.getColumnIndexOrThrow("status")));
        cursor.close();
    }

    @Test
    public void markAllChoresCompleted_marksAllPendingAsCompleted() {
        long userId = insertTestUser();
        dbHelper.insertChore("Chore 1", "", LocalDate.now().plusDays(1).toString(),
                LocalTime.of(10, 0).toString(), "PENDING", (int) userId);
        dbHelper.insertChore("Chore 2", "", LocalDate.now().plusDays(1).toString(),
                LocalTime.of(11, 0).toString(), "PENDING", (int) userId);

        int updated = dbHelper.markAllChoresCompleted((int) userId);
        assertEquals(2, updated);

        Cursor cursor = dbHelper.getChoresByUserId((int) userId);
        while (cursor.moveToNext()) {
            assertEquals("COMPLETED",
                    cursor.getString(cursor.getColumnIndexOrThrow("status")));
        }
        cursor.close();
    }

    
    // Reminder tests
    

    @Test
    public void insertReminder_validData_returnsPositiveId() {
        long userId = insertTestUser();
        long choreId = insertTestChore((int) userId);
        long reminderId = dbHelper.insertReminder(
                LocalDateTime.now().plusHours(1).toString(), (int) choreId);
        assertTrue(reminderId > 0);
    }

    @Test
    public void getRemindersByChoreId_noReminders_returnsEmptyCursor() {
        long userId = insertTestUser();
        long choreId = insertTestChore((int) userId);
        Cursor cursor = dbHelper.getRemindersByChoreId((int) choreId);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void getRemindersByChoreId_withReminders_returnsCorrectCount() {
        long userId = insertTestUser();
        long choreId = insertTestChore((int) userId);
        dbHelper.insertReminder(LocalDateTime.now().plusHours(1).toString(), (int) choreId);
        dbHelper.insertReminder(LocalDateTime.now().plusHours(2).toString(), (int) choreId);
        Cursor cursor = dbHelper.getRemindersByChoreId((int) choreId);
        assertEquals(2, cursor.getCount());
        cursor.close();
    }

    @Test
    public void deleteReminder_existingReminder_removesIt() {
        long userId = insertTestUser();
        long choreId = insertTestChore((int) userId);
        long reminderId = dbHelper.insertReminder(
                LocalDateTime.now().plusHours(1).toString(), (int) choreId);
        dbHelper.deleteReminder((int) reminderId);
        Cursor cursor = dbHelper.getRemindersByChoreId((int) choreId);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void deleteChore_cascadesToReminders() {
        long userId = insertTestUser();
        long choreId = insertTestChore((int) userId);
        dbHelper.insertReminder(LocalDateTime.now().plusHours(1).toString(), (int) choreId);
        dbHelper.deleteChore((int) choreId);
        Cursor cursor = dbHelper.getRemindersByChoreId((int) choreId);
        assertEquals("Reminders should be cascade-deleted with the chore",
                0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void getRemindersByUserId_includesChoreTitle() {
        long userId = insertTestUser();
        long choreId = insertTestChore((int) userId);
        dbHelper.insertReminder(LocalDateTime.now().plusHours(1).toString(), (int) choreId);
        Cursor cursor = dbHelper.getRemindersByUserId((int) userId);
        assertTrue(cursor.moveToFirst());
        String choreTitle = cursor.getString(cursor.getColumnIndexOrThrow("choreTitle"));
        assertEquals("Clean kitchen", choreTitle);
        cursor.close();
    }

    @Test
    public void getRemindersByUserId_onlyReturnsRemindersForThatUser() {
        long userId1 = dbHelper.insertUser("user1", "User One", "user1@test.com", PASSWORD);
        long userId2 = dbHelper.insertUser("user2", "User Two", "user2@test.com", PASSWORD);
        long choreId1 = insertTestChore((int) userId1);
        long choreId2 = insertTestChore((int) userId2);
        dbHelper.insertReminder(LocalDateTime.now().plusHours(1).toString(), (int) choreId1);
        dbHelper.insertReminder(LocalDateTime.now().plusHours(2).toString(), (int) choreId2);

        Cursor cursor = dbHelper.getRemindersByUserId((int) userId1);
        assertEquals(1, cursor.getCount());
        cursor.close();
    }
}
