package com.nestkeep.app.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

// Robolectric needed because isValidEmail uses android.util.Patterns
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ValidationUtilsTest {

    // isValidEmail

    @Test
    public void isValidEmail_validEmail_returnsTrue() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
    }

    @Test
    public void isValidEmail_validEmailWithSubdomain_returnsTrue() {
        assertTrue(ValidationUtils.isValidEmail("user@mail.example.com"));
    }

    @Test
    public void isValidEmail_validEmailWithPlus_returnsTrue() {
        assertTrue(ValidationUtils.isValidEmail("user+tag@example.com"));
    }

    @Test
    public void isValidEmail_noAtSign_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail("userexample.com"));
    }

    @Test
    public void isValidEmail_noDomain_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail("user@"));
    }

    @Test
    public void isValidEmail_noTLD_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail("user@example"));
    }

    @Test
    public void isValidEmail_emptyString_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail(""));
    }

    @Test
    public void isValidEmail_null_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail(null));
    }

    @Test
    public void isValidEmail_whitespaceOnly_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail("   "));
    }

    // isValidPassword

    @Test
    public void isValidPassword_eightChars_returnsTrue() {
        assertTrue(ValidationUtils.isValidPassword("12345678"));
    }

    @Test
    public void isValidPassword_moreThanEightChars_returnsTrue() {
        assertTrue(ValidationUtils.isValidPassword("password123!"));
    }

    @Test
    public void isValidPassword_sevenChars_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("1234567"));
    }

    @Test
    public void isValidPassword_emptyString_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword(""));
    }

    @Test
    public void isValidPassword_null_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword(null));
    }

    @Test
    public void isValidPassword_exactlyEightSpecialChars_returnsTrue() {
        assertTrue(ValidationUtils.isValidPassword("!@#$%^&*"));
    }

    // isValidUsername

    @Test
    public void isValidUsername_validThreeChars_returnsTrue() {
        assertTrue(ValidationUtils.isValidUsername("abc"));
    }

    @Test
    public void isValidUsername_validTwentyChars_returnsTrue() {
        assertTrue(ValidationUtils.isValidUsername("abcdefghij1234567890"));
    }

    @Test
    public void isValidUsername_withUnderscore_returnsTrue() {
        assertTrue(ValidationUtils.isValidUsername("user_name"));
    }

    @Test
    public void isValidUsername_withNumbers_returnsTrue() {
        assertTrue(ValidationUtils.isValidUsername("user123"));
    }

    @Test
    public void isValidUsername_tooShort_returnsFalse() {
        assertFalse(ValidationUtils.isValidUsername("ab"));
    }

    @Test
    public void isValidUsername_tooLong_returnsFalse() {
        assertFalse(ValidationUtils.isValidUsername("a".repeat(21)));
    }

    @Test
    public void isValidUsername_withSpace_returnsFalse() {
        assertFalse(ValidationUtils.isValidUsername("user name"));
    }

    @Test
    public void isValidUsername_withHyphen_returnsFalse() {
        assertFalse(ValidationUtils.isValidUsername("user-name"));
    }

    @Test
    public void isValidUsername_withSpecialChars_returnsFalse() {
        assertFalse(ValidationUtils.isValidUsername("user@name"));
    }

    @Test
    public void isValidUsername_null_returnsFalse() {
        assertFalse(ValidationUtils.isValidUsername(null));
    }

    @Test
    public void isValidUsername_emptyString_returnsFalse() {
        assertFalse(ValidationUtils.isValidUsername(""));
    }

    @Test
    public void isValidUsername_mixedCase_returnsTrue() {
        assertTrue(ValidationUtils.isValidUsername("UserName123"));
    }
}
