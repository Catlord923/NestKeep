package com.nestkeep.app.adapters;

import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.TextView;
import android.graphics.Color;
import android.view.ViewGroup;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.annotation.NonNull;

import com.nestkeep.app.utils.DateUtils;
import com.nestkeep.app.models.Status;
import com.nestkeep.app.models.Chore;
import com.nestkeep.app.R;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChoreAdapter extends RecyclerView.Adapter<ChoreAdapter.ChoreViewHolder> {

    private List<Chore> choreList = new ArrayList<>();
    private final OnChoreClickListener listener;

    public interface OnChoreClickListener {
        void onEditClick(Chore chore);
        void onDeleteClick(Chore chore);
    }

    public ChoreAdapter(List<Chore> choreList, OnChoreClickListener listener) {
        this.choreList = new ArrayList<>(choreList);
        this.listener = listener;
    }

    // Uses DiffUtil to compute the minimal set of changes between the old and new list,
    // avoiding a full redraw on every load.
    public void updateData(List<Chore> newChores) {
        ChoreDiffCallback diffCallback = new ChoreDiffCallback(choreList, newChores);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        choreList = new ArrayList<>(newChores);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ChoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chore, parent, false);
        return new ChoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChoreViewHolder holder, int position) {
        Chore chore = choreList.get(position);

        holder.tvChoreTitle.setText(chore.getTitle());
        holder.tvChoreDescription.setText(chore.getDescription());
        holder.tvDueDate.setText(DateUtils.formatDate(chore.getDueDate()));
        holder.tvDueTime.setText(DateUtils.formatTime(chore.getDueTime()));
        holder.tvStatus.setText(chore.getStatus().name());

        // Overdue: pending chore whose due date+time has passed
        boolean isOverdue = chore.getStatus() == Status.PENDING &&
                LocalDateTime.of(chore.getDueDate(), chore.getDueTime())
                        .isBefore(LocalDateTime.now());

        if (chore.getStatus() == Status.COMPLETED) {
            holder.tvStatus.setBackgroundResource(android.R.color.holo_green_dark);
            holder.viewStatusIndicator.setBackgroundResource(android.R.drawable.presence_online);
            holder.tvChoreTitle.setTextColor(Color.GRAY);
        } else if (isOverdue) {
            holder.tvStatus.setText(R.string.status_overdue);
            holder.tvStatus.setBackgroundResource(android.R.color.holo_red_dark);
            holder.viewStatusIndicator.setBackgroundResource(android.R.drawable.presence_busy);
            holder.tvChoreTitle.setTextColor(Color.RED);
        } else {
            holder.tvStatus.setBackgroundResource(android.R.color.holo_orange_dark);
            holder.viewStatusIndicator.setBackgroundResource(android.R.drawable.presence_invisible);
            holder.tvChoreTitle.setTextColor(Color.BLACK);
        }

        holder.btnEditChore.setOnClickListener(v -> listener.onEditClick(chore));
        holder.btnDeleteChore.setOnClickListener(v -> listener.onDeleteClick(chore));
    }

    @Override
    public int getItemCount() {
        return choreList.size();
    }

    static class ChoreViewHolder extends RecyclerView.ViewHolder {
        View viewStatusIndicator;
        TextView tvChoreTitle, tvChoreDescription, tvDueDate, tvDueTime, tvStatus;
        ImageButton btnEditChore, btnDeleteChore;

        public ChoreViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
            tvChoreTitle = itemView.findViewById(R.id.tvChoreTitle);
            tvChoreDescription = itemView.findViewById(R.id.tvChoreDescription);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvDueTime = itemView.findViewById(R.id.tvDueTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEditChore = itemView.findViewById(R.id.btnEditChore);
            btnDeleteChore = itemView.findViewById(R.id.btnDeleteChore);
        }
    }

    // DiffUtil

    static class ChoreDiffCallback extends DiffUtil.Callback {
        private final List<Chore> oldList;
        private final List<Chore> newList;

        ChoreDiffCallback(List<Chore> oldList, List<Chore> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getChoreId() == newList.get(newPos).getChoreId();
        }

        // Checks the fields that affect how a row looks. A status or date change
        // will trigger a rebind; description changes alone will not.
        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            Chore o = oldList.get(oldPos);
            Chore n = newList.get(newPos);
            return o.getTitle().equals(n.getTitle()) &&
                   o.getStatus() == n.getStatus() &&
                   o.getDueDate().equals(n.getDueDate()) &&
                   o.getDueTime().equals(n.getDueTime());
        }
    }
}
