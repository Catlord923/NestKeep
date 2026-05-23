package com.nestkeep.app.utils;

import com.google.android.material.textfield.TextInputLayout;

public class ValidationUtils {

    private ValidationUtils() {}

    /** Basic email format check - must contain @ and a dot after it. */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /** Password must be at least 8 characters. */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    /**
     * Username: 3 to 20 chars, letters/numbers/underscores only.
     */
    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        return username.matches("^[a-zA-Z0-9_]{3,20}$");
    }

    // Inline error helpers
    // These set the error on the TextInputLayout directly (Material inline errors)
    // and return false so callers can chain: if (!validateEmail(...)) return;

    public static boolean validateEmail(TextInputLayout layout, String email, String errorMsg) {
        if (!isValidEmail(email)) {
            layout.setError(errorMsg);
            return false;
        }
        layout.setError(null);
        return true;
    }

    public static boolean validatePassword(TextInputLayout layout, String password, String errorMsg) {
        if (!isValidPassword(password)) {
            layout.setError(errorMsg);
            return false;
        }
        layout.setError(null);
        return true;
    }

    public static boolean validateUsername(TextInputLayout layout, String username, String errorMsg) {
        if (!isValidUsername(username)) {
            layout.setError(errorMsg);
            return false;
        }
        layout.setError(null);
        return true;
    }

    public static boolean validateNotEmpty(TextInputLayout layout, String value, String errorMsg) {
        if (value == null || value.trim().isEmpty()) {
            layout.setError(errorMsg);
            return false;
        }
        layout.setError(null);
        return true;
    }

    public static boolean validatePasswordsMatch(TextInputLayout layout,
                                                  String password, String confirm, String errorMsg) {
        if (!password.equals(confirm)) {
            layout.setError(errorMsg);
            return false;
        }
        layout.setError(null);
        return true;
    }
}
