package com.nestkeep.app.activities;

import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.media.MediaPlayer;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nestkeep.app.utils.NotificationPermissionHelper;
import com.nestkeep.app.controllers.ChoreController;
import com.google.android.material.tabs.TabLayout;
import com.nestkeep.app.adapters.ChoreAdapter;
import com.nestkeep.app.utils.SessionManager;
import com.nestkeep.app.models.Status;
import com.nestkeep.app.models.Chore;
import com.nestkeep.app.R;

import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends BaseActivity {

    private SessionManager sessionManager;
    private ChoreController choreController;
    private ChoreAdapter adapter;

    private RecyclerView rvChores;
    private TextView tvEmptyState;
    private ProgressBar progressBar;
    private SearchView searchView;
    private TabLayout tabLayoutFilter;

    private Status currentFilter = null;
    private String currentQuery = "";

    // Sort options
    private static final int SORT_DUE_DATE = 0;
    private static final int SORT_TITLE = 1;
    private static final int SORT_STATUS = 2;
    private int currentSort = SORT_DUE_DATE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        NotificationPermissionHelper.requestIfNeeded(this);
        choreController = new ChoreController(this);

        initViews();
        setupToolbar();
        setupSearch();
        setupRecyclerView();
        setupTabs();
        setupFab();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If the user has been away for more than 30 minutes, expire the session
        // and send them back to sign-in for re-authentication
        if (sessionManager.isSessionExpired()) {
            sessionManager.updateLastActive(); // reset so sign-in doesn't loop
            Intent intent = new Intent(this, SignInActivity.class);
            intent.putExtra("session_expired", true);
            startActivity(intent);
            finish();
            return;
        }

        loadChores();
    }

    private void initViews() {
        rvChores        = findViewById(R.id.rvChores);
        tvEmptyState    = findViewById(R.id.tvEmptyState);
        tabLayoutFilter = findViewById(R.id.tabLayoutFilter);
        progressBar     = findViewById(R.id.progressBar);
        searchView      = findViewById(R.id.searchView);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.dashboard_welcome, sessionManager.getUsername()));

        ImageButton btnMenu = findViewById(R.id.btnReminders);
        btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, btnMenu);
            popup.getMenu().add(getString(R.string.menu_view_reminders));
            popup.getMenu().add(getString(R.string.menu_sort));
            popup.getMenu().add(getString(R.string.menu_mark_all_complete));
            popup.getMenu().add(getString(R.string.menu_delete_completed));
            popup.getMenu().add(getString(R.string.menu_log_out));
            popup.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if (title.equals(getString(R.string.menu_view_reminders))) {
                    startActivity(new Intent(this, RemindersActivity.class));
                } else if (title.equals(getString(R.string.menu_sort))) {
                    showSortDialog();
                } else if (title.equals(getString(R.string.menu_mark_all_complete))) {
                    confirmMarkAllComplete();
                } else if (title.equals(getString(R.string.menu_delete_completed))) {
                    confirmDeleteCompleted();
                } else if (title.equals(getString(R.string.menu_log_out))) {
                    sessionManager.logoutUser();
                    startActivity(new Intent(this, SignInActivity.class));
                    finish();
                }
                return true;
            });
            popup.show();
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query.trim().toLowerCase();
                loadChores();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText.trim().toLowerCase();
                loadChores();
                return true;
            }
        });
    }

    private void showSortDialog() {
        String[] options = {
                getString(R.string.sort_due_date),
                getString(R.string.sort_title),
                getString(R.string.sort_status)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_sort_title)
                .setSingleChoiceItems(options, currentSort, (dialog, which) -> {
                    currentSort = which;
                    dialog.dismiss();
                    loadChores();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmMarkAllComplete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_mark_all_title)
                .setMessage(R.string.dialog_mark_all_message)
                .setPositiveButton(R.string.action_confirm, (d, w) -> {
                    choreController.markAllChoresCompleted(sessionManager.getUserId());
                    loadChores();
                    Toast.makeText(this, R.string.toast_all_marked_complete,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmDeleteCompleted() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_completed_title)
                .setMessage(R.string.dialog_delete_completed_message)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    choreController.deleteCompletedChores(sessionManager.getUserId());
                    loadChores();
                    Toast.makeText(this, R.string.toast_completed_deleted,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void setupRecyclerView() {
        rvChores.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChoreAdapter(new ArrayList<>(), new ChoreAdapter.OnChoreClickListener() {
            @Override
            public void onEditClick(Chore chore) {
                Intent intent = new Intent(DashboardActivity.this, EditChoreActivity.class);
                intent.putExtra("choreId", chore.getChoreId());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(Chore chore) {
                new MaterialAlertDialogBuilder(DashboardActivity.this)
                        .setTitle(R.string.dialog_delete_chore_title)
                        .setMessage(R.string.dialog_delete_chore_message_with_reminders)
                        .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                            choreController.deleteChore(chore.getChoreId());
                            loadChores();
                            Toast.makeText(DashboardActivity.this,
                                    R.string.toast_chore_deleted, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            }
        });
        rvChores.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayoutFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentFilter = null; break;
                    case 1: currentFilter = Status.PENDING; break;
                    case 2: currentFilter = Status.COMPLETED; break;
                }
                loadChores();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAddChore);
        fab.setOnClickListener(v -> {
            playWooshSound();
            startActivity(new Intent(this, AddChoreActivity.class));
        });
    }

    // Fetches chores for the current user, then applies search filtering and
    // sorting in memory before handing the result to the adapter.
    private void loadChores() {
        progressBar.setVisibility(View.VISIBLE);
        rvChores.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);

        List<Chore> chores = choreController.getChoresForUser(
                sessionManager.getUserId(), currentFilter);

        // Filter by search query
        if (!currentQuery.isEmpty()) {
            chores = chores.stream()
                    .filter(c -> c.getTitle().toLowerCase().contains(currentQuery) ||
                            (c.getDescription() != null &&
                             c.getDescription().toLowerCase().contains(currentQuery)))
                    .collect(Collectors.toList());
        }

        // Sort
        switch (currentSort) {
            case SORT_TITLE:
                chores.sort(Comparator.comparing(c -> c.getTitle().toLowerCase()));
                break;
            case SORT_STATUS:
                chores.sort(Comparator.comparing(c -> c.getStatus().name()));
                break;
            case SORT_DUE_DATE:
            default:
                chores.sort(Comparator.comparing(
                        c -> c.getDueDate().atTime(c.getDueTime())));
                break;
        }

        progressBar.setVisibility(View.GONE);
        if (chores.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvChores.setVisibility(View.VISIBLE);
            adapter.updateData(chores);
        }
    }

    // Plays a sound when the FAB is tapped. Null check guards against a missing raw resource.
    private void playWooshSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.woosh);
        if (mediaPlayer == null) return;
        mediaPlayer.setOnCompletionListener(mp -> { mp.reset(); mp.release(); });
        mediaPlayer.start();
    }
}
