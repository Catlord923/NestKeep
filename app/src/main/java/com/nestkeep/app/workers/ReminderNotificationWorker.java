package com.nestkeep.app.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.work.WorkerParameters;
import androidx.annotation.NonNull;
import androidx.work.Worker;

import com.nestkeep.app.activities.DashboardActivity;

public class ReminderNotificationWorker extends Worker {

    public static final String KEY_CHORE_TITLE  = "chore_title";
    public static final String KEY_CHORE_ID     = "chore_id";
    public static final String KEY_REMINDER_ID  = "reminder_id";
    public static final String CHANNEL_ID       = "nestkeep_reminders";

    public ReminderNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String choreTitle = getInputData().getString(KEY_CHORE_TITLE);
        int choreId    = getInputData().getInt(KEY_CHORE_ID, -1);
        int reminderId = getInputData().getInt(KEY_REMINDER_ID, choreId); // fallback keeps old behaviour

        createNotificationChannel();

        // Tapping the notification opens the dashboard
        Intent intent = new Intent(getApplicationContext(), DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), reminderId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Reminder: " + choreTitle)
                .setContentText("This chore is coming up soon.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(reminderId, builder.build());
        }

        return Result.success();
    }

    // Creating a channel that already exists is a no-op, so this is safe to call
    // on every notification without checking whether the channel exists first.
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "NestKeep Reminders",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notifications for upcoming chore reminders");
        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
