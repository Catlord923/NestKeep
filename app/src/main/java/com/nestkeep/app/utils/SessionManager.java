package com.nestkeep.app.utils;

import android.content.SharedPreferences;
import android.content.Context;
import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SessionManager {
    private static final String PREF_NAME = "NestKeepSession";
    private static final String KEY_USER_ID_ENCODED = "userIdEncoded";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_BIOMETRIC_ENABLED = "biometricEnabled";
    private static final String KEY_LAST_ACTIVE = "lastActive";

    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    private static final String SALT = "nestkeep_uid_salt_2024";

    private final SharedPreferences pref;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void createLoginSession(int userId, String username) {
        pref.edit()
                .putString(KEY_USER_ID_ENCODED, encodeUserId(userId))
                .putString(KEY_USERNAME, username)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putLong(KEY_LAST_ACTIVE, System.currentTimeMillis())
                .apply();
    }

    /**
     * Call from onPause in any authenticated activity to keep the session alive
     * while the user is actively using the app.
     */
    public void updateLastActive() {
        pref.edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply();
    }

    /**
     * Returns true if the user has been away for longer than SESSION_TIMEOUT_MS.
     * A return value of true means the session should be treated as expired and
     * the user should re-authenticate.
     */
    public boolean isSessionExpired() {
        if (!isLoggedIn()) return false;
        long lastActive = pref.getLong(KEY_LAST_ACTIVE, 0L);
        if (lastActive == 0L) return false; // no timestamp yet - fresh login
        return System.currentTimeMillis() - lastActive > SESSION_TIMEOUT_MS;
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        String encoded = pref.getString(KEY_USER_ID_ENCODED, null);
        if (encoded == null) return -1;
        return decodeUserId(encoded);
    }

    public String getUsername() {
        return pref.getString(KEY_USERNAME, "User");
    }

    // Clears the session but preserves the biometric preference so the user
    // doesn't have to re-enable it after logging back in.
    public void logoutUser() {
        boolean biometricEnabled = isBiometricEnabled();
        pref.edit()
                .clear()
                .putBoolean(KEY_BIOMETRIC_ENABLED, biometricEnabled)
                .apply();
    }

    public boolean isBiometricEnabled() {
        return pref.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    // Stores the userId as Base64 with a SHA-256 checksum to prevent casual plaintext
    // reads of the SharedPreferences file.
    private static String encodeUserId(int userId) {
        try {
            String raw = SALT + userId;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            String idPart = Base64.encodeToString(
                    String.valueOf(userId).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            String checksum = Base64.encodeToString(hash, Base64.NO_WRAP).substring(0, 8);
            return idPart + ":" + checksum;
        } catch (NoSuchAlgorithmException e) {
            return Base64.encodeToString(
                    String.valueOf(userId).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        }
    }

    private static int decodeUserId(String encoded) {
        try {
            String idPart = encoded.split(":")[0];
            String decoded = new String(Base64.decode(idPart, Base64.NO_WRAP),
                    StandardCharsets.UTF_8);
            return Integer.parseInt(decoded);
        } catch (Exception e) {
            return -1;
        }
    }
}
