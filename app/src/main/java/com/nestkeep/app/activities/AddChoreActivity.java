package com.nestkeep.app.activities;

import android.app.TimePickerDialog;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nestkeep.app.controllers.ChoreController;
import com.nestkeep.app.utils.ValidationUtils;
import com.nestkeep.app.utils.SessionManager;
import com.nestkeep.app.utils.DateUtils;
import com.nestkeep.app.models.Reminder;
import com.nestkeep.app.models.Chore;
import com.nestkeep.app.models.Status;
import com.nestkeep.app.R;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public class AddChoreActivity extends BaseActivity {

    private TextInputLayout tilTitle, tilDueDate, tilDueTime;
    private TextInputEditText etTitle, etDescription, etDueDate, etDueTime;
    private Button btnAddReminder, btnSaveChore;
    private LinearLayout llRemindersContainer;
    private TextView tvNoReminders;
    private ProgressBar progressBar;

    private ChoreController choreController;
    private SessionManager sessionManager;

    private LocalDate selectedDueDate;
    private LocalTime selectedDueTime;
    private final List<LocalDateTime> reminderDates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_chore);

        choreController = new ChoreController(this);
        sessionManager  = new SessionManager(this);

        initViews();
        setupListeners();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmDiscard();
            }
        });
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> confirmDiscard());

        tilTitle    = findViewById(R.id.tilTitle);
        tilDueDate  = findViewById(R.id.tilDueDate);
        tilDueTime  = findViewById(R.id.tilDueTime);
        etTitle     = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etDueDate   = findViewById(R.id.etDueDate);
        etDueTime   = findViewById(R.id.etDueTime);

        btnAddReminder      = findViewById(R.id.btnAddReminder);
        btnSaveChore        = findViewById(R.id.btnSaveChore);
        llRemindersContainer = findViewById(R.id.llRemindersContainer);
        tvNoReminders       = findViewById(R.id.tvNoReminders);
        progressBar         = findViewById(R.id.progressBar);
    }

    // Shows a discard confirmation dialog if any fields have been filled in.
    // If nothing has been entered yet, finishes immediately without prompting.
    private void confirmDiscard() {
        boolean hasChanges = !etTitle.getText().toString().trim().isEmpty()
                || !etDescription.getText().toString().trim().isEmpty()
                || selectedDueDate != null
                || selectedDueTime != null
                || !reminderDates.isEmpty();

        if (!hasChanges) {
            finish();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_unsaved_changes_title)
                .setMessage(R.string.dialog_unsaved_changes_message)
                .setPositiveButton(R.string.action_leave, (d, w) -> finish())
                .setNegativeButton(R.string.action_stay, null)
                .show();
    }

    private void setupListeners() {
        View.OnClickListener dateClick = v -> showDatePicker();
        etDueDate.setOnClickListener(dateClick);
        tilDueDate.setEndIconOnClickListener(dateClick);

        View.OnClickListener timeClick = v -> showDueTimePicker();
        etDueTime.setOnClickListener(timeClick);
        tilDueTime.setEndIconOnClickListener(timeClick);

        btnAddReminder.setOnClickListener(v -> {
            if (selectedDueDate == null || selectedDueTime == null) {
                Toast.makeText(this, R.string.error_set_due_date_first, Toast.LENGTH_SHORT).show();
                return;
            }
            showReminderDatePicker();
        });

        btnSaveChore.setOnClickListener(v -> saveChore());
    }

    private void showDatePicker() {
        // Constrain to today and forward - no past due dates
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.label_due_date)
                .setCalendarConstraints(constraints)
                .setSelection(selectedDueDate != null
                        ? selectedDueDate.atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                        : MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            selectedDueDate = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            etDueDate.setText(DateUtils.formatDate(selectedDueDate));
            tilDueDate.setError(null);
        });

        picker.show(getSupportFragmentManager(), "due_date_picker");
    }

    private void showDueTimePicker() {
        LocalTime initial = selectedDueTime != null ? selectedDueTime : LocalTime.now();
        TimePickerDialog dialog = new TimePickerDialog(this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                (view, hourOfDay, minute) -> {
                    selectedDueTime = LocalTime.of(hourOfDay, minute);
                    etDueTime.setText(DateUtils.formatTime(selectedDueTime));
                    tilDueTime.setError(null);

                    if (selectedDueDate != null &&
                            selectedDueDate.equals(LocalDate.now()) &&
                            selectedDueTime.isBefore(LocalTime.now())) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.dialog_past_time_title)
                                .setMessage(R.string.dialog_past_time_message)
                                .setPositiveButton(R.string.action_keep_it, null)
                                .setNegativeButton(R.string.action_change_time,
                                        (d, w) -> showDueTimePicker())
                                .show();
                    }
                },
                initial.getHour(), initial.getMinute(), true);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void showReminderDatePicker() {
        // Reminders can only be set up to the due date
        LocalDateTime due = LocalDateTime.of(selectedDueDate, selectedDueTime);
        long dueMillis = due.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .setEnd(dueMillis)
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.label_reminder_date)
                .setCalendarConstraints(constraints)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            LocalDate reminderDate = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            showReminderTimePicker(reminderDate);
        });

        picker.show(getSupportFragmentManager(), "reminder_date_picker");
    }

    private void showReminderTimePicker(LocalDate reminderDate) {
        LocalTime initial = LocalTime.now();
        TimePickerDialog dialog = new TimePickerDialog(this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                (view, hourOfDay, minute) -> {
                    LocalDateTime picked = LocalDateTime.of(reminderDate,
                            LocalTime.of(hourOfDay, minute));
                    LocalDateTime due = LocalDateTime.of(selectedDueDate, selectedDueTime);

                    if (picked.isAfter(due)) {
                        Toast.makeText(this, R.string.error_reminder_after_due,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (picked.isBefore(LocalDateTime.now())) {
                        Toast.makeText(this, R.string.error_reminder_in_past,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addReminderView(picked);
                },
                initial.getHour(), initial.getMinute(), true);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    // Inflates a reminder row into the container and wires up its delete button.
    // The chore title and view-chore button are hidden here since the chore hasn't been saved yet.
    private void addReminderView(LocalDateTime reminderDateTime) {
        reminderDates.add(reminderDateTime);
        tvNoReminders.setVisibility(View.GONE);

        View reminderView = getLayoutInflater().inflate(
                R.layout.item_reminder, llRemindersContainer, false);
        TextView tvReminderDateTime  = reminderView.findViewById(R.id.tvReminderDateTime);
        TextView tvReminderChoreTitle = reminderView.findViewById(R.id.tvReminderChoreTitle);
        ImageView btnDeleteReminder  = reminderView.findViewById(R.id.btnDeleteReminder);
        View btnViewChore            = reminderView.findViewById(R.id.btnViewChore);

        tvReminderChoreTitle.setVisibility(View.GONE);
        btnViewChore.setVisibility(View.GONE);
        tvReminderDateTime.setText(
                DateUtils.formatDate(reminderDateTime.toLocalDate()) + " " +
                DateUtils.formatTime(reminderDateTime.toLocalTime()));

        btnDeleteReminder.setOnClickListener(v -> {
            llRemindersContainer.removeView(reminderView);
            reminderDates.remove(reminderDateTime);
            if (reminderDates.isEmpty()) tvNoReminders.setVisibility(View.VISIBLE);
        });

        llRemindersContainer.addView(reminderView);
    }

    // Validates all required fields, creates the chore, then schedules any reminders.
    // setLoading stays true on success since finish() is called immediately after.
    private void saveChore() {
        String title       = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        boolean valid = true;
        if (!ValidationUtils.validateNotEmpty(tilTitle, title,
                getString(R.string.error_field_required))) valid = false;
        if (selectedDueDate == null) {
            tilDueDate.setError(getString(R.string.error_field_required));
            valid = false;
        }
        if (selectedDueTime == null) {
            tilDueTime.setError(getString(R.string.error_field_required));
            valid = false;
        }
        if (!valid) return;

        setLoading(true);

        Chore chore = new Chore(0, title, description, selectedDueDate, selectedDueTime,
                Status.PENDING, sessionManager.getUserId());
        long choreId = choreController.createChore(chore);

        if (choreId != -1) {
            for (LocalDateTime dt : reminderDates) {
                choreController.createReminder(new Reminder(0, dt, (int) choreId), choreId, title);
            }
            Toast.makeText(this, R.string.toast_chore_added, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            setLoading(false);
            Toast.makeText(this, R.string.error_saving_chore, Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSaveChore.setEnabled(!loading);
    }
}
