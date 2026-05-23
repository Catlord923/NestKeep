package com.nestkeep.app.controllers;

import android.content.Context;

import androidx.work.Configuration;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.nestkeep.app.database.DatabaseHelper;
import com.nestkeep.app.models.Chore;
import com.nestkeep.app.models.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ChoreControllerTest {

    private ChoreController choreController;
    private DatabaseHelper dbHelper;
    private int testUserId;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();

        // Initialize WorkManager for testing
        Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);

        dbHelper = new DatabaseHelper(context);
        choreController = new ChoreController(context);

        // Insert a real user so foreign key constraints are satisfied
        testUserId = (int) dbHelper.insertUser(
                "testuser", "Test User", "test@example.com", "password123");
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    // validateInput

    @Test
    public void validateInput_validChore_returnsTrue() {
        Chore chore = new Chore(0, "Clean kitchen", "Scrub the hob",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), Status.PENDING, testUserId);
        assertTrue(choreController.validateInput(chore));
    }

    @Test
    public void validateInput_nullChore_returnsFalse() {
        assertFalse(choreController.validateInput(null));
    }

    @Test
    public void validateInput_nullTitle_returnsFalse() {
        Chore chore = new Chore(0, null, "desc",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), Status.PENDING, testUserId);
        assertFalse(choreController.validateInput(chore));
    }

    @Test
    public void validateInput_emptyTitle_returnsFalse() {
        Chore chore = new Chore(0, "", "desc",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), Status.PENDING, testUserId);
        assertFalse(choreController.validateInput(chore));
    }

    @Test
    public void validateInput_whitespaceOnlyTitle_returnsFalse() {
        Chore chore = new Chore(0, "   ", "desc",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), Status.PENDING, testUserId);
        assertFalse(choreController.validateInput(chore));
    }

    @Test
    public void validateInput_nullDueDate_returnsFalse() {
        Chore chore = new Chore(0, "Clean kitchen", "desc",
                null, LocalTime.of(10, 0), Status.PENDING, testUserId);
        assertFalse(choreController.validateInput(chore));
    }

    @Test
    public void validateInput_nullDueTime_returnsFalse() {
        Chore chore = new Chore(0, "Clean kitchen", "desc",
                LocalDate.now().plusDays(1), null, Status.PENDING, testUserId);
        assertFalse(choreController.validateInput(chore));
    }

    @Test
    public void validateInput_nullBothDateAndTime_returnsFalse() {
        Chore chore = new Chore(0, "Clean kitchen", "desc",
                null, null, Status.PENDING, testUserId);
        assertFalse(choreController.validateInput(chore));
    }

    @Test
    public void validateInput_emptyDescription_returnsTrue() {
        // Description is optional
        Chore chore = new Chore(0, "Clean kitchen", "",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), Status.PENDING, testUserId);
        assertTrue(choreController.validateInput(chore));
    }

    @Test
    public void validateInput_nullDescription_returnsTrue() {
        // Description is optional
        Chore chore = new Chore(0, "Clean kitchen", null,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), Status.PENDING, testUserId);
        assertTrue(choreController.validateInput(chore));
    }

    @Test
    public void validateInput_completedStatus_returnsTrue() {
        Chore chore = new Chore(0, "Clean kitchen", "desc",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), Status.COMPLETED, testUserId);
        assertTrue(choreController.validateInput(chore));
    }

    // createChore

    @Test
    public void createChore_invalidChore_returnsMinusOne() {
        Chore invalid = new Chore(0, "", "desc",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), Status.PENDING, testUserId);
        long result = choreController.createChore(invalid);
        assertEquals(-1, result);
    }

    @Test
    public void createChore_nullChore_returnsMinusOne() {
        long result = choreController.createChore(null);
        assertEquals(-1, result);
    }

    @Test
    public void createChore_validChore_returnsPositiveId() {
        Chore chore = new Chore(0, "Vacuum living room", "Use the Dyson",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), Status.PENDING, testUserId);
        long result = choreController.createChore(chore);
        assertTrue("Valid chore should return a positive row ID", result > 0);
    }

    // updateChoreFull

    @Test
    public void updateChoreFull_invalidChore_returnsFalse() {
        Chore invalid = new Chore(0, "", "desc",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), Status.PENDING, testUserId);
        assertFalse(choreController.updateChoreFull(invalid));
    }

    @Test
    public void updateChoreFull_nullChore_returnsFalse() {
        assertFalse(choreController.updateChoreFull(null));
    }

    // scheduleReminderNotification

    @Test
    public void scheduleReminderNotification_pastDateTime_doesNotThrow() {
        // Past reminders should be silently ignored, not crash
        choreController.scheduleReminderNotification(
                java.time.LocalDateTime.now().minusHours(1),
                "Test chore", 1, 1);
    }

    @Test
    public void scheduleReminderNotification_futureDateTime_doesNotThrow() {
        choreController.scheduleReminderNotification(
                java.time.LocalDateTime.now().plusHours(1),
                "Test chore", 1, 1);
    }
}
