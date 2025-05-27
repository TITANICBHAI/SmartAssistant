package com.aiassistant.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aiassistant.R;
import models.TaskInfo;
import models.TaskStatus;
import models.TaskPriority;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying tasks in a RecyclerView.
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<TaskInfo> tasks;
    private Context context;
    private TaskAdapterListener listener;
    private SimpleDateFormat dateFormat;

    /**
     * Interface for handling task-related actions.
     */
    public interface TaskAdapterListener {
        void onTaskExecuteClick(TaskInfo task);
        void onTaskEditClick(TaskInfo task);
        void onTaskDeleteClick(TaskInfo task);
    }

    /**
     * Constructor for the adapter.
     *
     * @param context  The context
     * @param listener The listener for task actions
     */
    public TaskAdapter(Context context, TaskAdapterListener listener) {
        this.context = context;
        this.listener = listener;
        this.tasks = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskInfo task = tasks.get(position);
        
        // Set task name and description
        holder.taskName.setText(task.getName());
        
        if (!TextUtils.isEmpty(task.getDescription())) {
            holder.taskDescription.setText(task.getDescription());
            holder.taskDescription.setVisibility(View.VISIBLE);
        } else {
            holder.taskDescription.setVisibility(View.GONE);
        }
        
        // Set schedule information
        String scheduleText = formatScheduleInfo(task);
        holder.taskSchedule.setText(scheduleText);
        
        // Set task status
        holder.taskStatus.setText(task.getStatus().getDisplayName());
        holder.taskStatus.setBackgroundColor(
                ContextCompat.getColor(context, task.getStatus().getStatusColorResId()));
        
        // Set priority indicator color
        holder.priorityIndicator.setBackgroundColor(
                ContextCompat.getColor(context, task.getPriority().getPriorityColorResId()));

        // Set click listeners for action buttons
        holder.executeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskExecuteClick(task);
            }
        });
        
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskEditClick(task);
            }
        });
        
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskDeleteClick(task);
            }
        });
        
        // Disable execute button if the task is already running or completed
        if (task.getStatus() == TaskStatus.RUNNING || task.getStatus() == TaskStatus.COMPLETED) {
            holder.executeButton.setAlpha(0.5f);
            holder.executeButton.setEnabled(false);
        } else {
            holder.executeButton.setAlpha(1.0f);
            holder.executeButton.setEnabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    /**
     * Updates the task list and refreshes the adapter.
     *
     * @param newTasks The new list of tasks
     */
    public void updateTasks(List<TaskInfo> newTasks) {
        this.tasks.clear();
        if (newTasks != null) {
            this.tasks.addAll(newTasks);
        }
        notifyDataSetChanged();
    }

    /**
     * Adds a single task to the list and refreshes the adapter.
     *
     * @param task The task to add
     */
    public void addTask(TaskInfo task) {
        if (task != null) {
            this.tasks.add(task);
            notifyItemInserted(this.tasks.size() - 1);
        }
    }

    /**
     * Updates a task in the list if it exists.
     *
     * @param task The updated task
     */
    public void updateTask(TaskInfo task) {
        if (task != null) {
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).getId().equals(task.getId())) {
                    tasks.set(i, task);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    /**
     * Removes a task from the list if it exists.
     *
     * @param taskId The ID of the task to remove
     */
    public void removeTask(String taskId) {
        if (taskId != null) {
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).getId().equals(taskId)) {
                    tasks.remove(i);
                    notifyItemRemoved(i);
                    break;
                }
            }
        }
    }

    /**
     * Formats schedule information for display.
     *
     * @param task The task
     * @return Formatted schedule information
     */
    private String formatScheduleInfo(TaskInfo task) {
        StringBuilder builder = new StringBuilder();
        builder.append(context.getString(R.string.scheduled_for)).append(" ");
        
        switch (task.getScheduleType()) {
            case ONCE:
                if (task.getScheduledDate() != null) {
                    builder.append(dateFormat.format(task.getScheduledDate()));
                }
                break;
                
            case DAILY:
                builder.append("Daily");
                if (task.getScheduledDate() != null) {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
                    builder.append(" at ").append(timeFormat.format(task.getScheduledDate()));
                }
                break;
                
            case WEEKLY:
                builder.append("Weekly");
                // TODO: Add day of week info
                break;
                
            case MONTHLY:
                builder.append("Monthly");
                // TODO: Add day of month info
                break;
                
            case INTERVAL:
                builder.append("Every ").append(task.getIntervalMinutes()).append(" minutes");
                break;
                
            default:
                if (task.getScheduledDate() != null) {
                    builder.append(dateFormat.format(task.getScheduledDate()));
                }
                break;
        }
        
        return builder.toString();
    }

    /**
     * ViewHolder for task items.
     */
    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName;
        TextView taskDescription;
        TextView taskSchedule;
        TextView taskStatus;
        View priorityIndicator;
        ImageView executeButton;
        ImageView editButton;
        ImageView deleteButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.tv_task_name);
            taskDescription = itemView.findViewById(R.id.tv_task_description);
            taskSchedule = itemView.findViewById(R.id.tv_task_schedule);
            taskStatus = itemView.findViewById(R.id.tv_task_status);
            priorityIndicator = itemView.findViewById(R.id.view_priority_indicator);
            executeButton = itemView.findViewById(R.id.iv_execute_task);
            editButton = itemView.findViewById(R.id.iv_edit_task);
            deleteButton = itemView.findViewById(R.id.iv_delete_task);
        }
    }
}