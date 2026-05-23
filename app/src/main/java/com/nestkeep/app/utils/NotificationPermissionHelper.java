package com.nestkeep.app.utils;

import android.content.pm.PackageManager;
import android.app.Activity;
import android.Manifest;
import android.os.Build;

import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

public class NotificationPermissionHelper {

    public static final int REQUEST_CODE = 1001;

    private NotificationPermissionHelper() {}

    /**
     * Returns true if POST_NOTIFICATIONS is already granted (or not needed below API 33).
     */
    public static boolean hasPermission(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(activity,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests POST_NOTIFICATIONS at runtime if not already granted.
     * No-op below API 33. The result comes back to onRequestPermissionsResult.
     */
    public static void requestIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (!hasPermission(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE
            );
        }
    }
}
