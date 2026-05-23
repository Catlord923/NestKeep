package com.nestkeep.app.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class HashUtils {

    private HashUtils() {}

    /**
     * Hashes a plain-text password with BCrypt (cost factor 12).
     * BCrypt automatically generates and embeds a unique salt per call,
     * so two identical passwords will produce different hashes.
     * Store the full returned string - it contains the salt + hash together.
     */
    public static String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     * Use this instead of hashing and comparing directly.
     */
    public static boolean verifyPassword(String password, String storedHash) {
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), storedHash);
        return result.verified;
    }
}
