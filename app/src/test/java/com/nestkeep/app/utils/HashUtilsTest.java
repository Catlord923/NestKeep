package com.nestkeep.app.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class HashUtilsTest {

    // hashPassword

    @Test
    public void hashPassword_returnsNonNullString() {
        String hash = HashUtils.hashPassword("password123");
        assertNotNull(hash);
    }

    @Test
    public void hashPassword_returnsBcryptFormat() {
        // BCrypt hashes always start with $2a$ or $2b$
        String hash = HashUtils.hashPassword("password123");
        assertTrue("Hash should start with BCrypt prefix",
                hash.startsWith("$2a$") || hash.startsWith("$2b$"));
    }

    @Test
    public void hashPassword_samePasswordProducesDifferentHashes() {
        // BCrypt uses a random salt per call - two hashes of the same password must differ
        String hash1 = HashUtils.hashPassword("password123");
        String hash2 = HashUtils.hashPassword("password123");
        assertNotEquals("Same password should produce different hashes due to salting",
                hash1, hash2);
    }

    @Test
    public void hashPassword_differentPasswordsProduceDifferentHashes() {
        String hash1 = HashUtils.hashPassword("password123");
        String hash2 = HashUtils.hashPassword("password456");
        assertNotEquals(hash1, hash2);
    }

    @Test
    public void hashPassword_shortPassword() {
        // Should not throw for short inputs
        String hash = HashUtils.hashPassword("ab");
        assertNotNull(hash);
    }

    @Test
    public void hashPassword_longPassword() {
        String longPassword = "a".repeat(72); // BCrypt max effective length
        String hash = HashUtils.hashPassword(longPassword);
        assertNotNull(hash);
    }

    @Test
    public void hashPassword_specialCharacters() {
        String hash = HashUtils.hashPassword("P@$$w0rd!#%^&*()");
        assertNotNull(hash);
    }

    // verifyPassword

    @Test
    public void verifyPassword_correctPasswordReturnsTrue() {
        String password = "password123";
        String hash = HashUtils.hashPassword(password);
        assertTrue(HashUtils.verifyPassword(password, hash));
    }

    @Test
    public void verifyPassword_wrongPasswordReturnsFalse() {
        String hash = HashUtils.hashPassword("password123");
        assertFalse(HashUtils.verifyPassword("wrongpassword", hash));
    }

    @Test
    public void verifyPassword_emptyPasswordReturnsFalse() {
        String hash = HashUtils.hashPassword("password123");
        assertFalse(HashUtils.verifyPassword("", hash));
    }

    @Test
    public void verifyPassword_caseSensitive() {
        String hash = HashUtils.hashPassword("Password123");
        assertFalse("BCrypt should be case-sensitive",
                HashUtils.verifyPassword("password123", hash));
    }

    @Test
    public void verifyPassword_worksWithMultipleHashesOfSamePassword() {
        // Both hashes of the same password should verify correctly
        String password = "mypassword";
        String hash1 = HashUtils.hashPassword(password);
        String hash2 = HashUtils.hashPassword(password);
        assertTrue(HashUtils.verifyPassword(password, hash1));
        assertTrue(HashUtils.verifyPassword(password, hash2));
    }

    @Test
    public void verifyPassword_specialCharacters() {
        String password = "P@$$w0rd!#%^&*()";
        String hash = HashUtils.hashPassword(password);
        assertTrue(HashUtils.verifyPassword(password, hash));
    }
}
