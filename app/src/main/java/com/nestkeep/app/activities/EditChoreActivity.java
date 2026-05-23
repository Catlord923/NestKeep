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
import com.nestkeep.app.models.Reminder;
import com.nestkeep.app.utils.DateUtils;
import com.nestkeep.app.models.Status;
import com.nestkeep.app.models.Chore;
import com.nestkeep.app.R;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public class EditChoreActivity extends BaseActivity {

    private TextInputLayout tilTitle, tilDueDate, tilDueTime;
    private TextInputEditText etTitle, etDescription, etDueDate, etDueTime;
    private TextView tvCurrentStatus, tvNoReminders;
    private Button btnMarkComplete, btnAddReminder, btnSaveChore;
    private LinearLayout llRemindersContainer;
    private ProgressBar progressBar;

    private ChoreController choreController;
    private Chore currentChore;
    private int choreId;

    private LocalDate selectedDueDate;
    private LocalTime selectedDueTime;
    private final List<LocalDateTime> newReminderDates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_chore);

        choreId = getIntent().getIntExtra("choreId", -1);
        if (choreId == -1) { finish(); return; }

        choreController = new ChoreController(this);
        initViews();
        loadChoreData();
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
        tvCurrentStatus     = findViewById(R.id.tvCurrentStatus);
        btnMarkComplete     = findViewById(R.id.btnMarkComplete);
        btnAddReminder      = findViewById(R.id.btnAddReminder);
        btnSaveChore        = findViewById(R.id.btnSaveChore);
        llRemindersContainer = findViewById(R.id.llRemindersContainer);
        tvNoReminders       = findViewById(R.id.tvNoReminders);
        progressBar         = findViewById(R.id.progressBar);
    }

    // Compares current field values against the originally loaded chore.
    // Only prompts if something has actually changed, avoiding false positives.
    private void confirmDiscard() {
        if (currentChore == null) { finish(); return; }

        String currentTitle = etTitle.getText().toString().trim();
        String currentDesc  = etDescription.getText().toString().trim();
        String originalDesc = currentChore.getDescription() != null
                ? currentChore.getDescription() : "";

        boolean hasChanges =
                !currentTitle.equals(currentChore.getTitle()) ||
                !currentDesc.equals(originalDesc) ||
                (selectedDueDate != null && !selectedDueDate.equals(currentChore.getDueDate())) ||
                (selectedDueTime != null && !selectedDueTime.equals(currentChore.getDueTime())) ||
                !newReminderDates.isEmpty();

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

    private void loadChoreData() {
        currentChore = choreController.getChoreById(choreId);
        if (currentChore == null) { finish(); return; }

        etTitle.setText(currentChore.getTitle());
        etDescription.setText(currentChore.getDescription());
        selectedDueDate = currentChore.getDueDate();
        selectedDueTime = currentChore.getDueTime();
        etDueDate.setText(DateUtils.formatDate(selectedDueDate));
        etDueTime.setText(DateUtils.formatTime(selectedDueTime));
        updateStatusUI(currentChore.getStatus());

        for (Reminder r : choreController.getRemindersForChore(choreId)) {
            addReminderView(r.getReminderDateTime(), r.getReminderId());
        }
    }

    private void updateStatusUI(Status status) {
        tvCurrentStatus.setText(status.name());
        if (status == Status.COMPLETED) {
            tvCurrentStatus.setBackgroundResource(android.R.color.holo_green_dark);
            btnMarkComplete.setText(R.string.action_revert_to_pending);
        } else {
            tvCurrentStatus.setBackgroundResource(android.R.color.holo_orange_dark);
            btnMarkComplete.setText(R.string.action_mark_complete);
        }
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

        btnMarkComplete.setOnClickListener(v -> {
            if (currentChore.getStatus() == Status.COMPLETED) {
                // Reverting to pending - check if the form's due date/time is in the past,
                // not currentChore's stored values which may have been changed in the form
                LocalDateTime due = LocalDateTime.of(selectedDueDate, selectedDueTime);
                if (due.isBefore(LocalDateTime.now())) {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.dialog_revert_overdue_title)
                            .setMessage(R.string.dialog_revert_overdue_message)
                            .setPositiveButton(R.string.action_revert_anyway, (d, w) -> {
                                currentChore.setStatus(Status.PENDING);
                                updateStatusUI(Status.PENDING);
                            })
                            .setNegativeButton(R.string.action_cancel, null)
                            .show();
                    return;
                }
            }
            Status newStatus = currentChore.getStatus() == Status.COMPLETED
                    ? Status.PENDING : Status.COMPLETED;
            currentChore.setStatus(newStatus);
            updateStatusUI(newStatus);
        });

        btnSaveChore.setOnClickListener(v -> saveChanges());
    }

    private void showDatePicker() {
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
                    addReminderView(picked, -1);
                },
                initial.getHour(), initial.getMinute(), true);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    // existingReminderId is -1 for newly added reminders (not yet in the DB).
    // Existing reminders are deleted via the controller; new ones are removed from the local list.
    private void addReminderView(LocalDateTime reminderDateTime, int existingReminderId) {
        if (existingReminderId == -1) newReminderDates.add(reminderDateTime);
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
            if (existingReminderId != -1) {
                choreController.deleteReminder(existingReminderId);
            } else {
                newReminderDates.remove(reminderDateTime);
            }
            if (llRemindersContainer.getChildCount() == 0)
                tvNoReminders.setVisibility(View.VISIBLE);
        });

        llRemindersContainer.addView(reminderView);
    }

    // Updates the chore in the DB, then schedules any newly added reminders.
    // Only newReminderDates are processed here - existing reminders were already
    // handled individually via their delete buttons.
    private void saveChanges() {
        String title       = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        boolean valid = true;
        if (!ValidationUtils.validateNotEmpty(tilTitle, title,
                getString(R.string.error_field_required))) valid = false;
        if (selectedDueDate == null) {
            tilDueDate.setError(getString(R.string.error_field_required)); valid = false;
        }
        if (selectedDueTime == null) {
            tilDueTime.setError(getString(R.string.error_field_required)); valid = false;
        }
        if (!valid) return;

        setLoading(true);
        currentChore.setTitle(title);
        currentChore.setDescription(description);
        currentChore.setDueDate(selectedDueDate);
        currentChore.setDueTime(selectedDueTime);

        if (choreController.updateChoreFull(currentChore)) {
            for (LocalDateTime dt : newReminderDates) {
                choreController.createReminder(
                        new Reminder(0, dt, choreId), choreId, title);
            }
            Toast.makeText(this, R.string.toast_changes_saved, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            setLoading(false);
            Toast.makeText(this, R.string.error_saving_changes, Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSaveChore.setEnabled(!loading);
    }
}
