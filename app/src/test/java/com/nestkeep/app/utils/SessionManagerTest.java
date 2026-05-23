package com.nestkeep.app.utils;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class SessionManagerTest {

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        sessionManager = new SessionManager(context);
        sessionManager.logoutUser(); // start clean for each test
    }

    // createLoginSession

    @Test
    public void createLoginSession_setsLoggedInTrue() {
        sessionManager.createLoginSession(1, "testuser");
        assertTrue(sessionManager.isLoggedIn());
    }

    @Test
    public void createLoginSession_storesUsername() {
        sessionManager.createLoginSession(1, "testuser");
        assertEquals("testuser", sessionManager.getUsername());
    }

    @Test
    public void createLoginSession_storesUserId() {
        sessionManager.createLoginSession(42, "testuser");
        assertEquals(42, sessionManager.getUserId());
    }

    @Test
    public void createLoginSession_storesLastActiveTimestamp() {
        long before = System.currentTimeMillis();
        sessionManager.createLoginSession(1, "testuser");
        // Session should not be expired immediately after login
        assertFalse(sessionManager.isSessionExpired());
    }

    // isLoggedIn

    @Test
    public void isLoggedIn_beforeLogin_returnsFalse() {
        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    public void isLoggedIn_afterLogin_returnsTrue() {
        sessionManager.createLoginSession(1, "testuser");
        assertTrue(sessionManager.isLoggedIn());
    }

    @Test
    public void isLoggedIn_afterLogout_returnsFalse() {
        sessionManager.createLoginSession(1, "testuser");
        sessionManager.logoutUser();
        assertFalse(sessionManager.isLoggedIn());
    }

    // getUserId

    @Test
    public void getUserId_beforeLogin_returnsMinusOne() {
        assertEquals(-1, sessionManager.getUserId());
    }

    @Test
    public void getUserId_afterLogin_returnsCorrectId() {
        sessionManager.createLoginSession(99, "testuser");
        assertEquals(99, sessionManager.getUserId());
    }

    @Test
    public void getUserId_withLargeId_returnsCorrectId() {
        sessionManager.createLoginSession(Integer.MAX_VALUE, "testuser");
        assertEquals(Integer.MAX_VALUE, sessionManager.getUserId());
    }

    @Test
    public void getUserId_withIdOne_returnsCorrectId() {
        sessionManager.createLoginSession(1, "testuser");
        assertEquals(1, sessionManager.getUserId());
    }

    // getUsername

    @Test
    public void getUsername_beforeLogin_returnsDefault() {
        assertEquals("User", sessionManager.getUsername());
    }

    @Test
    public void getUsername_afterLogin_returnsStoredUsername() {
        sessionManager.createLoginSession(1, "john_doe");
        assertEquals("john_doe", sessionManager.getUsername());
    }

    // logoutUser

    @Test
    public void logoutUser_clearsLoginState() {
        sessionManager.createLoginSession(1, "testuser");
        sessionManager.logoutUser();
        assertFalse(sessionManager.isLoggedIn());
        assertEquals(-1, sessionManager.getUserId());
        assertEquals("User", sessionManager.getUsername());
    }

    @Test
    public void logoutUser_preservesBiometricPreference() {
        sessionManager.createLoginSession(1, "testuser");
        sessionManager.setBiometricEnabled(true);
        sessionManager.logoutUser();
        assertTrue("Biometric preference should survive logout",
                sessionManager.isBiometricEnabled());
    }

    @Test
    public void logoutUser_preservesBiometricDisabledPreference() {
        sessionManager.createLoginSession(1, "testuser");
        sessionManager.setBiometricEnabled(false);
        sessionManager.logoutUser();
        assertFalse(sessionManager.isBiometricEnabled());
    }

    // isSessionExpired

    @Test
    public void isSessionExpired_freshLogin_returnsFalse() {
        sessionManager.createLoginSession(1, "testuser");
        assertFalse(sessionManager.isSessionExpired());
    }

    @Test
    public void isSessionExpired_notLoggedIn_returnsFalse() {
        // Not logged in should never report expired
        assertFalse(sessionManager.isSessionExpired());
    }

    @Test
    public void isSessionExpired_afterUpdateLastActive_returnsFalse() {
        sessionManager.createLoginSession(1, "testuser");
        sessionManager.updateLastActive();
        assertFalse(sessionManager.isSessionExpired());
    }

    // updateLastActive

    @Test
    public void updateLastActive_doesNotThrow() {
        sessionManager.createLoginSession(1, "testuser");
        sessionManager.updateLastActive(); // should not throw
    }

    // biometric flag

    @Test
    public void isBiometricEnabled_defaultsFalse() {
        assertFalse(sessionManager.isBiometricEnabled());
    }

    @Test
    public void setBiometricEnabled_true_persistsTrue() {
        sessionManager.setBiometricEnabled(true);
        assertTrue(sessionManager.isBiometricEnabled());
    }

    @Test
    public void setBiometricEnabled_false_persistsFalse() {
        sessionManager.setBiometricEnabled(true);
        sessionManager.setBiometricEnabled(false);
        assertFalse(sessionManager.isBiometricEnabled());
    }

    // userId encoding round-trip

    @Test
    public void userId_encodingRoundTrip_variousIds() {
        int[] testIds = {1, 10, 100, 1000, 9999, 123456};
        for (int id : testIds) {
            sessionManager.createLoginSession(id, "user");
            assertEquals("userId round-trip failed for id=" + id, id, sessionManager.getUserId());
            sessionManager.logoutUser();
        }
    }
}
