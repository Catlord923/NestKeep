package com.nestkeep.app.activities;

import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Button;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nestkeep.app.database.DatabaseHelper;
import com.nestkeep.app.utils.ValidationUtils;
import com.nestkeep.app.utils.SessionManager;
import com.nestkeep.app.models.User;
import com.nestkeep.app.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilUsername, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etUsername, etEmail, etPassword, etConfirmPassword;
    private ProgressBar progressBar;
    private Button btnRegister;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Registered before super.onCreate so the callback is active immediately
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                finish();
            }
        });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        tilFullName        = findViewById(R.id.tilFullName);
        tilUsername        = findViewById(R.id.tilUsername);
        tilEmail           = findViewById(R.id.tilEmail);
        tilPassword        = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etFullName        = findViewById(R.id.etFullName);
        etUsername        = findViewById(R.id.etUsername);
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        Button btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        btnRegister.setOnClickListener(v -> registerUser());

        TextView tvGoToSignIn = findViewById(R.id.tvGoToSignIn);
        tvGoToSignIn.setOnClickListener(v -> {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        });
    }

    // Validates all fields first, collecting all errors before returning,
    // then inserts the user on a background thread due to BCrypt cost.
    // On success, auto-logs in and navigates straight to the dashboard.
    private void registerUser() {
        String fullName        = etFullName.getText().toString().trim();
        String username        = etUsername.getText().toString().trim();
        String email           = etEmail.getText().toString().trim();
        String password        = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        boolean valid = true;
        if (!ValidationUtils.validateNotEmpty(tilFullName, fullName,
                getString(R.string.error_field_required))) valid = false;
        if (!ValidationUtils.validateUsername(tilUsername, username,
                getString(R.string.error_invalid_username))) valid = false;
        if (!ValidationUtils.validateEmail(tilEmail, email,
                getString(R.string.error_invalid_email))) valid = false;
        if (!ValidationUtils.validatePassword(tilPassword, password,
                getString(R.string.error_password_too_short))) valid = false;
        if (!ValidationUtils.validatePasswordsMatch(tilConfirmPassword, password, confirmPassword,
                getString(R.string.error_passwords_no_match))) valid = false;
        if (!valid) return;

        // BCrypt hashing inside insertUser is slow - run off the main thread
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btnRegister).setEnabled(false);

        executor.execute(() -> {
            User user = dbHelper.insertUserAndReturn(username, fullName, email, password);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                findViewById(R.id.btnRegister).setEnabled(true);

                if (user != null) {
                    sessionManager.createLoginSession(user.getUserId(), user.getUsername());
                    startActivity(new Intent(this, DashboardActivity.class));
                    finish();
                } else {
                    tilUsername.setError(getString(R.string.error_username_taken));
                    tilEmail.setError(getString(R.string.error_email_taken));
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
