package com.aiassistant.scheduler;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.content.Intent;
import android.content.IntentFilter;

import utils.ActionExecutor;
import com.aiassistant.scheduler.model.Action;
import com.aiassistant.scheduler.model.ActionSequence;
import com.aiassistant.scheduler.model.Condition;
import com.aiassistant.scheduler.model.ScheduledTask;
import com.aiassistant.scheduler.model.TaskPriority;
import com.aiassistant.scheduler.model.TaskStatus;
import com.aiassistant.scheduler.model.Trigger;
import com.aiassistant.scheduler.model.TriggerType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Advanced task scheduler for managing AI and automation tasks
 */
public class TaskSchedulerManager {
    private static final String TAG = "TaskSchedulerManager";
    
    // Scheduler settings
    private static final long SCHEDULER_TICK_MS = 1000; // Tick every second
    private static final int MAX_CONCURRENT_TASKS = 5; // Maximum tasks that can run at once
    private static final int MAX_TASKS = 100; // Maximum number of tasks in the scheduler
    private static final String TASKS_FILENAME = "scheduled_tasks.dat";
    private static final String PREFS_NAME = "scheduler_prefs";
    
    // Context and task storage
    private final Context context;
    private final Map<String, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private final List<String> runningTaskIds = new CopyOnWriteArrayList<>();
    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    
    // Scheduler thread and state
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    // Action executor
    private final ActionExecutor actionExecutor;
    
    // Task listeners
    private final List<TaskListener> taskListeners = new CopyOnWriteArrayList<>();
    
    /**
     * Task event listener interface
     */
    public interface TaskListener {
        void onTaskStarted(ScheduledTask task);
        void onTaskCompleted(ScheduledTask task, boolean success);
        void onTaskStatusChanged(ScheduledTask task, TaskStatus oldStatus, TaskStatus newStatus);
    }
    
    /**
     * Create a new task scheduler manager
     */
    public TaskSchedulerManager(Context context) {
        this.context = context;
        this.actionExecutor = new ActionExecutor(context);
        
        // Load saved tasks
        loadTasks();
    }
    
    /**
     * Start the scheduler
     */
    public boolean start() {
        if (isRunning.compareAndSet(false, true)) {
            // Start scheduler thread
            scheduler.scheduleAtFixedRate(
                this::processTasks,
                0,
                SCHEDULER_TICK_MS,
                TimeUnit.MILLISECONDS
            );
            
            Log.d(TAG, "Task scheduler started");
            return true;
        }
        
        return false;
    }
    
    /**
     * Stop the scheduler
     */
    public boolean stop() {
        if (isRunning.compareAndSet(true, false)) {
            // Cancel all running tasks
            for (String taskId : runningTaskIds) {
                cancelTask(taskId);
            }
            
            // Cancel all scheduled futures
            for (ScheduledFuture<?> future : scheduledFutures.values()) {
                future.cancel(false);
            }
            
            scheduledFutures.clear();
            
            Log.d(TAG, "Task scheduler stopped");
            return true;
        }
        
        return false;
    }
    
    /**
     * Process scheduled tasks
     */
    private void processTasks() {
        if (!isRunning.get()) {
            return;
        }
        
        try {
            // Get current time
            long now = System.currentTimeMillis();
            
            // Check if we can run more tasks
            if (runningTaskIds.size() >= MAX_CONCURRENT_TASKS) {
                return;
            }
            
            // Get all pending or scheduled tasks
            List<ScheduledTask> pendingTasks = new ArrayList<>();
            
            for (ScheduledTask task : tasks.values()) {
                if (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.SCHEDULED) {
                    pendingTasks.add(task);
                }
            }
            
            // Sort by priority (highest first) and then by scheduled time (earliest first)
            Collections.sort(pendingTasks, new Comparator<ScheduledTask>() {
                @Override
                public int compare(ScheduledTask t1, ScheduledTask t2) {
                    // First compare by priority (reverse order - highest priority first)
                    int priorityCompare = Integer.compare(
                        t2.getPriority().getValue(),
                        t1.getPriority().getValue()
                    );
                    
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }
                    
                    // Then compare by scheduled time (earliest first)
                    return Long.compare(
                        t1.getScheduledTime().getTime(),
                        t2.getScheduledTime().getTime()
                    );
                }
            });
            
            // Check each task
            for (ScheduledTask task : pendingTasks) {
                // Skip if we've reached the maximum concurrent tasks
                if (runningTaskIds.size() >= MAX_CONCURRENT_TASKS) {
                    break;
                }
                
                // Check if it's time to run the task
                if (task.getScheduledTime().getTime() <= now) {
                    // Check if trigger condition is met
                    if (task.getTrigger() != null && !evaluateTrigger(task.getTrigger())) {
                        continue;
                    }
                    
                    // Check if pre-execution condition is met
                    if (task.getPreExecutionCondition() != null && 
                        !evaluateCondition(task.getPreExecutionCondition())) {
                        continue;
                    }
                    
                    // Execute the task
                    executeTask(task);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing tasks: " + e.getMessage(), e);
        }
    }
    
    /**
     * Execute a task
     */
    private void executeTask(ScheduledTask task) {
        if (task == null || runningTaskIds.contains(task.getTaskId())) {
            return;
        }
        
        try {
            // Add to running tasks
            runningTaskIds.add(task.getTaskId());
            
            // Update status
            TaskStatus oldStatus = task.getStatus();
            task.setStatus(TaskStatus.RUNNING);
            task.setLastExecutionTime(new Date());
            task.setExecutionCount(task.getExecutionCount() + 1);
            
            // Notify listeners
            notifyTaskStatusChanged(task, oldStatus, TaskStatus.RUNNING);
            notifyTaskStarted(task);
            
            // Get action sequence
            ActionSequence sequence = task.getSequence();
            
            if (sequence != null) {
                // Execute in a separate thread
                Runnable taskRunnable = () -> {
                    boolean success = true;
                    
                    // Execute each action in the sequence
                    for (Action action : sequence.getActions()) {
                        try {
                            // Check if we should stop
                            if (!isRunning.get() || !runningTaskIds.contains(task.getTaskId())) {
                                success = false;
                                break;
                            }
                            
                            // Check action condition
                            if (action.getCondition() != null && !evaluateCondition(action.getCondition())) {
                                continue;
                            }
                            
                            // Execute action
                            boolean actionSuccess = actionExecutor.executeAction(
                                action.getActionType(),
                                action.getParameters()
                            );
                            
                            // If action failed and we should stop on error, exit
                            if (!actionSuccess && sequence.isStopOnError()) {
                                success = false;
                                break;
                            }
                            
                            // If action has a delay after execution, wait
                            if (action.getDelayAfterMs() > 0) {
                                try {
                                    Thread.sleep(action.getDelayAfterMs());
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error executing action: " + e.getMessage(), e);
                            if (sequence.isStopOnError()) {
                                success = false;
                                break;
                            }
                        }
                    }
                    
                    // Task is complete
                    completeTask(task, success);
                };
                
                // Execute the task
                if (sequence.isRunOnUiThread()) {
                    mainHandler.post(taskRunnable);
                } else {
                    scheduler.execute(taskRunnable);
                }
            } else {
                // No sequence, just mark as complete
                completeTask(task, true);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting task execution: " + e.getMessage(), e);
            completeTask(task, false);
        }
    }
    
    /**
     * Complete a task execution
     */
    private void completeTask(ScheduledTask task, boolean success) {
        try {
            // Remove from running tasks
            runningTaskIds.remove(task.getTaskId());
            
            // Update task
            TaskStatus oldStatus = task.getStatus();
            if (success) {
                task.setStatus(TaskStatus.COMPLETED);
                task.setSuccessCount(task.getSuccessCount() + 1);
            } else {
                task.setStatus(TaskStatus.FAILED);
                task.setFailureCount(task.getFailureCount() + 1);
            }
            
            task.setLastCompletionTime(new Date());
            
            // For recurring tasks, schedule next execution
            if (task.isRecurring()) {
                rescheduleTask(task);
            }
            
            // Notify listeners
            notifyTaskStatusChanged(task, oldStatus, task.getStatus());
            notifyTaskCompleted(task, success);
            
            // Save task state
            saveTasks();
            
        } catch (Exception e) {
            Log.e(TAG, "Error completing task: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reschedule a recurring task
     */
    private void rescheduleTask(ScheduledTask task) {
        try {
            // Calculate next execution time based on recurrence pattern
            Date nextExecution = calculateNextExecutionTime(task);
            
            if (nextExecution != null) {
                // Update task
                task.setScheduledTime(nextExecution);
                task.setStatus(TaskStatus.SCHEDULED);
                
                Log.d(TAG, "Rescheduled task " + task.getTaskId() + " for " + nextExecution);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error rescheduling task: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate next execution time based on recurrence pattern
     */
    private Date calculateNextExecutionTime(ScheduledTask task) {
        if (!task.isRecurring() || task.getRecurrencePattern() == null) {
            return null;
        }
        
        try {
            long now = System.currentTimeMillis();
            long nextTime = now;
            
            switch (task.getRecurrencePattern()) {
                case MINUTES:
                    nextTime = now + (task.getRecurrenceInterval() * 60 * 1000);
                    break;
                    
                case HOURLY:
                    nextTime = now + (task.getRecurrenceInterval() * 60 * 60 * 1000);
                    break;
                    
                case DAILY:
                    nextTime = now + (task.getRecurrenceInterval() * 24 * 60 * 60 * 1000);
                    break;
                    
                case WEEKLY:
                    nextTime = now + (task.getRecurrenceInterval() * 7 * 24 * 60 * 60 * 1000);
                    break;
                    
                case MONTHLY:
                    // Simplified - just add 30 days
                    nextTime = now + (task.getRecurrenceInterval() * 30 * 24 * 60 * 60 * 1000);
                    break;
                    
                case CUSTOM:
                    // Custom recurrence using interval in seconds
                    nextTime = now + (task.getRecurrenceInterval() * 1000);
                    break;
            }
            
            return new Date(nextTime);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating next execution time: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Add a new task to the scheduler
     */
    public String scheduleTask(ScheduledTask task) {
        if (task == null) {
            return null;
        }
        
        try {
            // Check if we've reached the maximum number of tasks
            if (tasks.size() >= MAX_TASKS) {
                // Remove oldest non-running task
                removeOldestTask();
            }
            
            // Generate task ID if not already set
            if (task.getTaskId() == null || task.getTaskId().isEmpty()) {
                task.setTaskId(generateTaskId());
            }
            
            // Set created time if not already set
            if (task.getCreatedTime() == null) {
                task.setCreatedTime(new Date());
            }
            
            // Set scheduled time if not already set
            if (task.getScheduledTime() == null) {
                task.setScheduledTime(new Date());
            }
            
            // Set status to scheduled
            task.setStatus(TaskStatus.SCHEDULED);
            
            // Add to tasks map
            tasks.put(task.getTaskId(), task);
            
            // Save tasks
            saveTasks();
            
            Log.d(TAG, "Scheduled task: " + task.getTaskId() + " - " + task.getName());
            
            return task.getTaskId();
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling task: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Remove oldest non-running task
     */
    private void removeOldestTask() {
        try {
            ScheduledTask oldestTask = null;
            Date oldestTime = new Date();
            
            // Find oldest non-running task
            for (ScheduledTask task : tasks.values()) {
                if (!runningTaskIds.contains(task.getTaskId()) && 
                    task.getCreatedTime().before(oldestTime)) {
                    oldestTask = task;
                    oldestTime = task.getCreatedTime();
                }
            }
            
            // Remove it
            if (oldestTask != null) {
                tasks.remove(oldestTask.getTaskId());
                Log.d(TAG, "Removed oldest task: " + oldestTask.getTaskId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing oldest task: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate a unique task ID
     */
    private String generateTaskId() {
        return "task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    /**
     * Cancel a running or scheduled task
     */
    public boolean cancelTask(String taskId) {
        try {
            // Get the task
            ScheduledTask task = tasks.get(taskId);
            
            if (task == null) {
                return false;
            }
            
            // Cancel the task
            TaskStatus oldStatus = task.getStatus();
            task.setStatus(TaskStatus.CANCELLED);
            
            // Remove from running tasks
            runningTaskIds.remove(taskId);
            
            // Cancel any scheduled future
            ScheduledFuture<?> future = scheduledFutures.get(taskId);
            if (future != null) {
                future.cancel(false);
                scheduledFutures.remove(taskId);
            }
            
            // Notify listeners
            notifyTaskStatusChanged(task, oldStatus, TaskStatus.CANCELLED);
            
            // Save tasks
            saveTasks();
            
            Log.d(TAG, "Cancelled task: " + taskId);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling task: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Delete a task from the scheduler
     */
    public boolean deleteTask(String taskId) {
        try {
            // Cancel the task first
            cancelTask(taskId);
            
            // Remove from tasks map
            ScheduledTask task = tasks.remove(taskId);
            
            if (task != null) {
                // Save tasks
                saveTasks();
                
                Log.d(TAG, "Deleted task: " + taskId);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting task: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get a task by ID
     */
    public ScheduledTask getTask(String taskId) {
        return tasks.get(taskId);
    }
    
    /**
     * Get all tasks
     */
    public List<ScheduledTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
    
    /**
     * Get tasks by status
     */
    public List<ScheduledTask> getTasksByStatus(TaskStatus status) {
        List<ScheduledTask> result = new ArrayList<>();
        
        for (ScheduledTask task : tasks.values()) {
            if (task.getStatus() == status) {
                result.add(task);
            }
        }
        
        return result;
    }
    
    /**
     * Get tasks by priority
     */
    public List<ScheduledTask> getTasksByPriority(TaskPriority priority) {
        List<ScheduledTask> result = new ArrayList<>();
        
        for (ScheduledTask task : tasks.values()) {
            if (task.getPriority() == priority) {
                result.add(task);
            }
        }
        
        return result;
    }
    
    /**
     * Save tasks to storage
     */
    private void saveTasks() {
        try {
            // Create tasks directory if it doesn't exist
            File directory = new File(context.getFilesDir(), "tasks");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Create file
            File file = new File(directory, TASKS_FILENAME);
            
            // Create a copy of tasks (to avoid ConcurrentModificationException)
            Map<String, ScheduledTask> tasksCopy = new HashMap<>(tasks);
            
            // Write tasks to file
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                out.writeObject(tasksCopy);
            }
            
            // Save metadata to preferences
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("tasks_count", tasks.size());
            editor.putLong("last_save_time", System.currentTimeMillis());
            editor.apply();
            
            Log.d(TAG, "Saved " + tasks.size() + " tasks to storage");
        } catch (IOException e) {
            Log.e(TAG, "Error saving tasks: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load tasks from storage
     */
    @SuppressWarnings("unchecked")
    private void loadTasks() {
        try {
            // Get tasks file
            File directory = new File(context.getFilesDir(), "tasks");
            File file = new File(directory, TASKS_FILENAME);
            
            if (file.exists()) {
                // Read tasks from file
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                    Map<String, ScheduledTask> loadedTasks = (Map<String, ScheduledTask>) in.readObject();
                    
                    if (loadedTasks != null) {
                        // Clear current tasks
                        tasks.clear();
                        
                        // Add loaded tasks
                        tasks.putAll(loadedTasks);
                        
                        // Reset running tasks (since we're just starting)
                        runningTaskIds.clear();
                        
                        // Update task statuses
                        for (ScheduledTask task : tasks.values()) {
                            if (task.getStatus() == TaskStatus.RUNNING) {
                                task.setStatus(TaskStatus.PENDING);
                            }
                        }
                        
                        Log.d(TAG, "Loaded " + tasks.size() + " tasks from storage");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading tasks: " + e.getMessage(), e);
        }
    }
    
    /**
     * Evaluate a trigger against real system state.
     *
     * Supported trigger types:
     *  IMMEDIATE     — always fires.
     *  SCHEDULED     — fires when the task's scheduledTime has passed (already
     *                  checked by the caller, so return true here).
     *  APP_LAUNCH    — fires when the foreground package matches "package_name".
     *  APP_EXIT      — fires when the foreground package does NOT match "package_name".
     *  BATTERY       — fires when battery level is above/below "level".
     *  CONNECTIVITY  — fires when network type matches "type" (wifi/cellular/none).
     *  SCREEN_STATE  — fires when screen is on/off, matching "state" (on/off).
     *  TIME_RANGE    — fires when current hour:minute is within [start_hour:start_minute,
     *                  end_hour:end_minute].
     *  DATA_CONDITION — delegates to the trigger's embedded Condition object.
     *  Custom / others — evaluate the trigger's embedded Condition or return true.
     */
    private boolean evaluateTrigger(Trigger trigger) {
        if (trigger == null || trigger.getTriggerType() == null) return true;

        try {
            TriggerType type = trigger.getTriggerType();

            switch (type) {

                case IMMEDIATE:
                case SCHEDULED:
                    return true;

                case APP_LAUNCH: {
                    String requiredPkg = (String) trigger.getParameter("package_name");
                    if (requiredPkg == null || requiredPkg.isEmpty()) return true;
                    com.aiassistant.services.AIAccessibilityService svc =
                            com.aiassistant.services.AIAccessibilityService.getInstance();
                    if (svc == null) return false;
                    return requiredPkg.equals(svc.getCurrentPackageName());
                }

                case APP_EXIT: {
                    String blockedPkg = (String) trigger.getParameter("package_name");
                    if (blockedPkg == null || blockedPkg.isEmpty()) return true;
                    com.aiassistant.services.AIAccessibilityService svc =
                            com.aiassistant.services.AIAccessibilityService.getInstance();
                    if (svc == null) return true;
                    return !blockedPkg.equals(svc.getCurrentPackageName());
                }

                case BATTERY: {
                    int    requiredLevel = getIntParam(trigger, "level", 20);
                    boolean checkBelow  = getBoolParam(trigger, "below", true);
                    int    currentLevel = getBatteryLevel();
                    return checkBelow ? currentLevel <= requiredLevel
                                     : currentLevel >= requiredLevel;
                }

                case CONNECTIVITY: {
                    String requiredType = (String) trigger.getParameter("type");
                    if (requiredType == null) return true;
                    com.aiassistant.monitoring.NetworkStateMonitor.NetworkState ns =
                            getNetworkState();
                    if (ns == null) return false;
                    switch (requiredType.toLowerCase()) {
                        case "wifi":     return ns.type ==
                                com.aiassistant.monitoring.NetworkStateMonitor.ConnectionType.WIFI;
                        case "cellular": return ns.type ==
                                com.aiassistant.monitoring.NetworkStateMonitor.ConnectionType.CELLULAR;
                        case "connected": return ns.isConnected;
                        case "none":
                        case "offline":  return !ns.isConnected;
                        default:         return ns.isConnected;
                    }
                }

                case SCREEN_STATE: {
                    String reqState = (String) trigger.getParameter("state");
                    android.os.PowerManager pm = (android.os.PowerManager)
                            context.getSystemService(Context.POWER_SERVICE);
                    boolean screenOn = pm != null && pm.isInteractive();
                    if ("off".equalsIgnoreCase(reqState)) return !screenOn;
                    return screenOn; // default: "on"
                }

                case TIME_RANGE: {
                    int startH = getIntParam(trigger, "start_hour",   0);
                    int startM = getIntParam(trigger, "start_minute",  0);
                    int endH   = getIntParam(trigger, "end_hour",     23);
                    int endM   = getIntParam(trigger, "end_minute",   59);
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    int nowH = cal.get(java.util.Calendar.HOUR_OF_DAY);
                    int nowM = cal.get(java.util.Calendar.MINUTE);
                    int nowMins   = nowH * 60 + nowM;
                    int startMins = startH * 60 + startM;
                    int endMins   = endH * 60 + endM;
                    if (startMins <= endMins) {
                        return nowMins >= startMins && nowMins <= endMins;
                    } else {
                        // Crosses midnight
                        return nowMins >= startMins || nowMins <= endMins;
                    }
                }

                case DATA_CONDITION: {
                    Condition embedded = trigger.getCondition();
                    if (embedded == null) return true;
                    return evaluateCondition(embedded);
                }

                default:
                    // Unknown trigger — check embedded condition or pass
                    Condition embedded = trigger.getCondition();
                    return embedded == null || evaluateCondition(embedded);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error evaluating trigger: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Evaluate a condition by building a real context map from system state
     * and delegating to {@link Condition#evaluate(Map)}.
     */
    private boolean evaluateCondition(Condition condition) {
        if (condition == null) return true;
        try {
            Map<String, Object> ctx = buildConditionContext();
            return condition.evaluate(ctx);
        } catch (Exception e) {
            Log.e(TAG, "Error evaluating condition: " + e.getMessage(), e);
            return false;
        }
    }

    /** Assembles a context map with real system values for condition evaluation. */
    private Map<String, Object> buildConditionContext() {
        Map<String, Object> ctx = new HashMap<>();

        // Battery
        ctx.put("battery_level", getBatteryLevel());

        // Network
        com.aiassistant.monitoring.NetworkStateMonitor.NetworkState ns = getNetworkState();
        ctx.put("network_connected", ns != null && ns.isConnected);
        ctx.put("network_type",      ns != null ? ns.type.name().toLowerCase() : "none");
        ctx.put("network_metered",   ns != null && ns.isMetered);

        // Screen
        android.os.PowerManager pm = (android.os.PowerManager)
                context.getSystemService(Context.POWER_SERVICE);
        ctx.put("screen_on", pm != null && pm.isInteractive());

        // Foreground app
        com.aiassistant.services.AIAccessibilityService svc =
                com.aiassistant.services.AIAccessibilityService.getInstance();
        ctx.put("foreground_package", svc != null ? svc.getCurrentPackageName() : "");
        ctx.put("last_interaction_ms",
                svc != null ? svc.getLastInteractionTime() : 0L);

        // Time
        java.util.Calendar cal = java.util.Calendar.getInstance();
        ctx.put("hour",    cal.get(java.util.Calendar.HOUR_OF_DAY));
        ctx.put("minute",  cal.get(java.util.Calendar.MINUTE));
        ctx.put("weekday", cal.get(java.util.Calendar.DAY_OF_WEEK));

        // Running task count
        ctx.put("running_task_count", runningTaskIds.size());

        return ctx;
    }

    // -----------------------------------------------------------------------
    // System state helpers
    // -----------------------------------------------------------------------

    private int getBatteryLevel() {
        android.content.Intent bIntent = context.registerReceiver(
                null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (bIntent == null) return 100;
        int level = bIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
        int scale = bIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
        return scale > 0 ? (int) (100f * level / scale) : level;
    }

    private com.aiassistant.monitoring.NetworkStateMonitor.NetworkState getNetworkState() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return null;
            // Quick synchronous check using a temp monitor
            com.aiassistant.monitoring.NetworkStateMonitor mon =
                    new com.aiassistant.monitoring.NetworkStateMonitor(context);
            return mon.getCurrentState();
        } catch (Exception e) {
            return null;
        }
    }

    private int getIntParam(Trigger t, String key, int def) {
        Object v = t.getParameter(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) { try { return Integer.parseInt((String) v); } catch (Exception ignored) {} }
        return def;
    }

    private boolean getBoolParam(Trigger t, String key, boolean def) {
        Object v = t.getParameter(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String)  return Boolean.parseBoolean((String) v);
        return def;
    }
    
    /**
     * Add a task listener
     */
    public void addTaskListener(TaskListener listener) {
        if (listener != null && !taskListeners.contains(listener)) {
            taskListeners.add(listener);
        }
    }
    
    /**
     * Remove a task listener
     */
    public void removeTaskListener(TaskListener listener) {
        taskListeners.remove(listener);
    }
    
    /**
     * Notify listeners that a task has started
     */
    private void notifyTaskStarted(ScheduledTask task) {
        for (TaskListener listener : taskListeners) {
            try {
                listener.onTaskStarted(task);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying task listener: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Notify listeners that a task has completed
     */
    private void notifyTaskCompleted(ScheduledTask task, boolean success) {
        for (TaskListener listener : taskListeners) {
            try {
                listener.onTaskCompleted(task, success);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying task listener: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Notify listeners that a task's status has changed
     */
    private void notifyTaskStatusChanged(ScheduledTask task, TaskStatus oldStatus, TaskStatus newStatus) {
        for (TaskListener listener : taskListeners) {
            try {
                listener.onTaskStatusChanged(task, oldStatus, newStatus);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying task listener: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        // Stop the scheduler
        stop();
        
        // Cancel all scheduled futures
        for (ScheduledFuture<?> future : scheduledFutures.values()) {
            future.cancel(true);
        }
        
        // Shutdown executor
        scheduler.shutdownNow();
        
        // Save tasks
        saveTasks();
        
        Log.d(TAG, "Task scheduler cleaned up");
    }
}