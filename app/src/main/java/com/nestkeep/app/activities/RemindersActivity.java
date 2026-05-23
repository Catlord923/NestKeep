package com.nestkeep.app.activities;

import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;
import android.view.View;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nestkeep.app.controllers.ChoreController;
import com.nestkeep.app.adapters.ReminderAdapter;
import com.nestkeep.app.utils.SessionManager;
import com.nestkeep.app.models.Reminder;
import com.nestkeep.app.R;

import java.util.ArrayList;
import java.util.List;

public class RemindersActivity extends BaseActivity {

    private RecyclerView rvReminders;
    private TextView tvEmptyState;
    private ProgressBar progressBar;

    private ReminderAdapter adapter;
    private ChoreController choreController;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        choreController = new ChoreController(this);
        sessionManager  = new SessionManager(this);

        initViews();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminders();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvReminders  = findViewById(R.id.rvReminders);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressBar  = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        rvReminders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReminderAdapter(new ArrayList<>(), new ReminderAdapter.OnReminderClickListener() {
            @Override
            public void onViewChoreClick(Reminder reminder) {
                Intent intent = new Intent(RemindersActivity.this, EditChoreActivity.class);
                intent.putExtra("choreId", reminder.getChoreId());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(Reminder reminder) {
                new MaterialAlertDialogBuilder(RemindersActivity.this)
                        .setTitle(R.string.dialog_delete_reminder_title)
                        .setMessage(R.string.dialog_delete_reminder_message)
                        .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                            choreController.deleteReminder(reminder.getReminderId());
                            loadReminders();
                            Toast.makeText(RemindersActivity.this,
                                    R.string.toast_reminder_deleted, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            }
        });
        rvReminders.setAdapter(adapter);
    }

    // Fetches all reminders for the current user via a single JOIN query.
    // Past and upcoming reminders are distinguished visually by the adapter.
    private void loadReminders() {
        progressBar.setVisibility(View.VISIBLE);
        rvReminders.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);

        List<Reminder> allReminders = choreController.getRemindersForUser(
                sessionManager.getUserId());

        progressBar.setVisibility(View.GONE);
        if (allReminders.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvReminders.setVisibility(View.VISIBLE);
            adapter.updateData(allReminders);
        }
    }
}
