package com.nestkeep.app.adapters;

import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.ViewGroup;
import android.graphics.Color;
import android.view.View;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;

import com.nestkeep.app.utils.DateUtils;
import com.nestkeep.app.models.Reminder;
import com.nestkeep.app.R;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private List<Reminder> reminderList = new ArrayList<>();
    private final OnReminderClickListener listener;

    public interface OnReminderClickListener {
        void onViewChoreClick(Reminder reminder);
        void onDeleteClick(Reminder reminder);
    }

    public ReminderAdapter(List<Reminder> reminderList, OnReminderClickListener listener) {
        this.reminderList = new ArrayList<>(reminderList);
        this.listener = listener;
    }

    // Uses DiffUtil to compute the minimal set of changes, avoiding a full redraw on every load.
    public void updateData(List<Reminder> newReminders) {
        ReminderDiffCallback diffCallback = new ReminderDiffCallback(reminderList, newReminders);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        reminderList = new ArrayList<>(newReminders);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);

        // choreTitle is pre-fetched via JOIN in ChoreController.getRemindersForUser(),
        // so no additional DB call is needed here.
        holder.tvReminderChoreTitle.setText(reminder.getChoreTitle());

        boolean isPast = reminder.getReminderDateTime().isBefore(LocalDateTime.now());

        String prefix = isPast
                ? holder.itemView.getContext().getString(R.string.label_past) + " · "
                : holder.itemView.getContext().getString(R.string.label_upcoming) + " · ";

        String dateTime = DateUtils.formatDate(reminder.getReminderDate())
                + " at "
                + DateUtils.formatTime(reminder.getReminderTime());
        holder.tvReminderDateTime.setText(prefix + dateTime);

        // Past reminders appear dimmed; upcoming appear normal
        float alpha = isPast ? 0.45f : 1.0f;
        holder.itemView.setAlpha(alpha);
        holder.tvReminderChoreTitle.setTextColor(isPast ? Color.GRAY : Color.BLACK);

        holder.btnViewChore.setOnClickListener(v -> listener.onViewChoreClick(reminder));
        holder.btnDeleteReminder.setOnClickListener(v -> listener.onDeleteClick(reminder));
    }

    @Override
    public int getItemCount() { return reminderList.size(); }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView tvReminderChoreTitle, tvReminderDateTime;
        ImageButton btnViewChore, btnDeleteReminder;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReminderChoreTitle = itemView.findViewById(R.id.tvReminderChoreTitle);
            tvReminderDateTime = itemView.findViewById(R.id.tvReminderDateTime);
            btnViewChore = itemView.findViewById(R.id.btnViewChore);
            btnDeleteReminder = itemView.findViewById(R.id.btnDeleteReminder);
        }
    }

    // DiffUtil

    static class ReminderDiffCallback extends DiffUtil.Callback {
        private final List<Reminder> oldList;
        private final List<Reminder> newList;

        ReminderDiffCallback(List<Reminder> oldList, List<Reminder> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getReminderId() == newList.get(newPos).getReminderId();
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getReminderDateTime()
                    .equals(newList.get(newPos).getReminderDateTime());
        }
    }
}
