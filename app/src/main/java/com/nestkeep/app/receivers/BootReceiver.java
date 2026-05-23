package com.nestkeep.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nestkeep.app.controllers.ChoreController;
import com.nestkeep.app.utils.SessionManager;
import com.nestkeep.app.models.Reminder;
import com.nestkeep.app.models.Status;
import com.nestkeep.app.models.Chore;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fired when the device reboots. WorkManager jobs don't survive a reboot,
 * so we re-schedule any future reminders that would have been lost.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SessionManager sessionManager = new SessionManager(context);
        if (!sessionManager.isLoggedIn()) return; // no user, nothing to reschedule

        // Note: SessionManager holds a single session, so only the currently logged-in
        // user's reminders are rescheduled on reboot. This is intentional for a
        // single user per device app; a multi-user design would need to persist all
        // user IDs separately and iterate over them here.

        ChoreController choreController = new ChoreController(context);
        int userId = sessionManager.getUserId();

        // Re-schedule all future reminders for pending chores
        List<Chore> chores = choreController.getChoresForUser(userId, Status.PENDING);
        for (Chore chore : chores) {
            List<Reminder> reminders = choreController.getRemindersForChore(chore.getChoreId());
            for (Reminder reminder : reminders) {
                if (reminder.getReminderDateTime().isAfter(LocalDateTime.now())) {
                    choreController.scheduleReminderNotification(
                            reminder.getReminderDateTime(),
                            chore.getTitle(),
                            chore.getChoreId(),
                            reminder.getReminderId()  // use reminderId to avoid notification collision
                    );
                }
            }
        }
    }
}
