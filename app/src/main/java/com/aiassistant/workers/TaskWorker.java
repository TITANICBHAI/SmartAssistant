package com.aiassistant.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.aiassistant.MainActivity;
import com.aiassistant.R;
import com.aiassistant.database.AppDatabase;
import models.Task;
import com.aiassistant.services.TaskSchedulerService;

/**
 * Worker for executing scheduled tasks.
 */
public class TaskWorker extends Worker {
    
    private static final String TAG = "TaskWorker";
    
    // Input data keys
    public static final String KEY_TASK_ID = "task_id";
    public static final String KEY_TASK_TITLE = "task_title";
    public static final String KEY_TASK_ACTION = "task_action";
    
    private static final String NOTIFICATION_CHANNEL_ID = "task_notification_channel";
    
    public TaskWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        int taskId = getInputData().getInt(KEY_TASK_ID, -1);
        String taskTitle = getInputData().getString(KEY_TASK_TITLE);
        String taskAction = getInputData().getString(KEY_TASK_ACTION);
        
        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID");
            return Result.failure();
        }
        
        try {
            // Get task from database
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            Task task = database.taskDao().getTaskById(taskId);
            
            if (task == null) {
                Log.e(TAG, "Task not found: " + taskId);
                return Result.failure();
            }
            
            // Check if task is still active
            if (!task.isActive()) {
                Log.d(TAG, "Task is inactive, skipping: " + taskId);
                return Result.success();
            }
            
            // Perform task action
            if ("Notification".equals(taskAction)) {
                // Show notification for the task
                showTaskNotification(task);
            } else {
                // For other action types, let the service handle it
                TaskSchedulerService schedulerService = TaskSchedulerService.getInstance(getApplicationContext());
                schedulerService.executeTaskNow(task);
                return Result.success();
            }
            
            // Update task execution time
            task.setLastExecutedAt(System.currentTimeMillis());
            database.taskDao().updateTask(task);
            
            // If it's a repeating task, schedule the next occurrence
            if (!"None".equals(task.getRepeatPattern())) {
                long nextTime = task.getNextScheduledTime();
                task.setScheduledTime(nextTime);
                
                // Use the service to schedule the next occurrence
                TaskSchedulerService schedulerService = TaskSchedulerService.getInstance(getApplicationContext());
                schedulerService.rescheduleTask(task);
            }
            
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing task: " + taskId, e);
            return Result.failure();
        }
    }
    
    /**
     * Show a notification for the task
     */
    private void showTaskNotification(Task task) {
        Context context = getApplicationContext();
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            createNotificationChannel(notificationManager);
            
            // Create intent for when notification is tapped
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, task.getId(), intent, PendingIntent.FLAG_IMMUTABLE);
            
            // Build notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(task.getTitle())
                    .setContentText(task.getDescription())
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            
            // Show notification
            notificationManager.notify(task.getId(), builder.build());
        }
    }
    
    /**
     * Create notification channel for Android O and above
     */
    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Task Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notifications for scheduled tasks");
            
            notificationManager.createNotificationChannel(channel);
        }
    }
}
