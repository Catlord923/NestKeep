package com.nestkeep.app.activities;

import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.CheckBox;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.biometric.BiometricPrompt;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nestkeep.app.database.DatabaseHelper;
import com.nestkeep.app.utils.ValidationUtils;
import com.nestkeep.app.utils.SessionManager;
import com.nestkeep.app.models.User;
import com.nestkeep.app.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignInActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private ProgressBar progressBar;
    private CheckBox cbEnableBiometric;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private boolean hasKnownUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        tilEmail    = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);
        cbEnableBiometric = findViewById(R.id.cbEnableBiometric);

        boolean sessionExpired = getIntent().getBooleanExtra("session_expired", false);

        // Biometrics are only meaningful when there is a known user to re-authenticate.
        // A voluntary logout clears the session, so hasKnownUser will be false and the
        // biometric button will not be shown, forcing credential entry instead.
        hasKnownUser = sessionManager.isLoggedIn() || sessionExpired;

        setupBiometrics();

        // Fix back button: pressing back on SignIn exits the app rather than
        // navigating to a previous (possibly authenticated) screen
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        // Show checkbox checked if biometric was previously enabled
        cbEnableBiometric.setChecked(sessionManager.isBiometricEnabled());

        boolean biometricEnabled = sessionManager.isBiometricEnabled();
        // Auto-prompt whenever there is a known user and biometrics is enabled,
        // whether that's a normal reopen or a session expiry. The button is shown
        // only after the user explicitly dismisses the prompt via "Use Password".
        boolean shouldAutoPrompt = biometricEnabled && hasKnownUser;

        if (sessionExpired) {
            Toast.makeText(this, R.string.session_expired, Toast.LENGTH_SHORT).show();
        }

        if (shouldAutoPrompt) {
            // Hide the button since the prompt fires automatically - showing both would
            // cause a double prompt. It will reappear via onAuthenticationError if the
            // user dismisses with "Use Password".
            findViewById(R.id.btnBiometric).setVisibility(View.GONE);
            biometricPrompt.authenticate(promptInfo);
        } else {
            findViewById(R.id.btnBiometric).setVisibility(View.GONE);
        }

        Button btnSignIn = findViewById(R.id.btnSignIn);
        btnSignIn.setOnClickListener(v -> loginUser());

        TextView tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });
    }

    // Sets up the BiometricPrompt and its PromptInfo. Must be called before any
    // biometric authentication is attempted, including auto-triggers in onCreate.
    private void setupBiometrics() {
        biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        sessionManager.updateLastActive();
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.biometric_welcome_back,
                                        sessionManager.getUsername()),
                                Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignInActivity.this, DashboardActivity.class));
                        finish();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // User tapped "Use Password" - re-show the button so they can
                        // switch back to biometrics if they change their mind.
                        // Only re-show if there is a known user to authenticate as.
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON && hasKnownUser) {
                            findViewById(R.id.btnBiometric).setVisibility(View.VISIBLE);
                        }
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.biometric_use_password))
                .build();
    }

    private void loginUser() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean valid = true;
        if (!ValidationUtils.validateEmail(tilEmail, email,
                getString(R.string.error_invalid_email))) valid = false;
        if (!ValidationUtils.validateNotEmpty(tilPassword, password,
                getString(R.string.error_field_required))) valid = false;
        if (!valid) return;

        // BCrypt is intentionally slow - run it off the main thread to avoid UI freeze
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btnSignIn).setEnabled(false);

        executor.execute(() -> {
            User user = dbHelper.authenticateUser(email, password);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                findViewById(R.id.btnSignIn).setEnabled(true);

                if (user != null) {
                    sessionManager.createLoginSession(user.getUserId(), user.getUsername());
                    // Save biometric preference from checkbox
                    sessionManager.setBiometricEnabled(cbEnableBiometric.isChecked());
                    startActivity(new Intent(this, DashboardActivity.class));
                    finish();
                } else {
                    tilEmail.setError(getString(R.string.error_invalid_credentials));
                    tilPassword.setError(" "); // space shows the error underline without text
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
