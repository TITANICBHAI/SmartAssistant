package com.aiassistant.models;

import java.util.Calendar;

/**
 * Task Schedule
 * Defines when a task should be executed
 */
public class TaskSchedule {
    // Schedule type
    private ScheduleType scheduleType;
    
    // Interval in milliseconds (for recurring tasks)
    private long intervalMs;
    
    // Start time (for delayed or scheduled tasks)
    private long startTimeMs;
    
    // End time (for tasks with an expiration)
    private long endTimeMs;
    
    // Maximum executions (0 for unlimited)
    private int maxExecutions;
    
    // Days of week (for weekly tasks)
    private boolean[] daysOfWeek;
    
    // Whether the task should execute as soon as possible
    private boolean executeImmediately;
    
    // Whether the task should execute when the device is idle
    private boolean executeWhenIdle;
    
    // Whether the task should execute on app launch
    private boolean executeOnAppLaunch;
    
    // Whether the task should execute on app close
    private boolean executeOnAppClose;
    
    /**
     * Create a new one-time schedule
     */
    public static TaskSchedule oneTime() {
        TaskSchedule schedule = new TaskSchedule();
        schedule.scheduleType = ScheduleType.ONE_TIME;
        schedule.executeImmediately = true;
        return schedule;
    }
    
    /**
     * Create a new delayed schedule
     */
    public static TaskSchedule delayed(long delayMs) {
        TaskSchedule schedule = new TaskSchedule();
        schedule.scheduleType = ScheduleType.ONE_TIME;
        schedule.startTimeMs = System.currentTimeMillis() + delayMs;
        return schedule;
    }
    
    /**
     * Create a new recurring schedule
     */
    public static TaskSchedule recurring(long intervalMs) {
        TaskSchedule schedule = new TaskSchedule();
        schedule.scheduleType = ScheduleType.RECURRING;
        schedule.intervalMs = intervalMs;
        schedule.maxExecutions = 0; // Unlimited
        return schedule;
    }
    
    /**
     * Create a new daily schedule
     */
    public static TaskSchedule daily(int hourOfDay, int minute) {
        TaskSchedule schedule = new TaskSchedule();
        schedule.scheduleType = ScheduleType.DAILY;
        
        // Calculate first execution time
        Calendar calendar = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        // If time is in the past, schedule for tomorrow
        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        schedule.startTimeMs = calendar.getTimeInMillis();
        schedule.intervalMs = 24 * 60 * 60 * 1000; // 24 hours
        schedule.maxExecutions = 0; // Unlimited
        
        return schedule;
    }
    
    /**
     * Create a new weekly schedule
     */
    public static TaskSchedule weekly(int hourOfDay, int minute, boolean[] daysOfWeek) {
        TaskSchedule schedule = new TaskSchedule();
        schedule.scheduleType = ScheduleType.WEEKLY;
        schedule.daysOfWeek = daysOfWeek;
        
        // Calculate first execution time
        Calendar calendar = Calendar.getInstance();
        
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        // Find the next day that matches
        for (int i = 0; i < 7; i++) {
            int dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - 1) % 7; // 0-6 (Sunday-Saturday)
            
            if (daysOfWeek[dayOfWeek]) {
                break;
            }
            
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        schedule.startTimeMs = calendar.getTimeInMillis();
        schedule.maxExecutions = 0; // Unlimited
        
        return schedule;
    }
    
    /**
     * Create a new app event schedule
     */
    public static TaskSchedule appEvent(boolean onLaunch, boolean onClose) {
        TaskSchedule schedule = new TaskSchedule();
        schedule.scheduleType = ScheduleType.APP_EVENT;
        schedule.executeOnAppLaunch = onLaunch;
        schedule.executeOnAppClose = onClose;
        schedule.maxExecutions = 0; // Unlimited
        return schedule;
    }
    
    /**
     * Create a new idle schedule
     */
    public static TaskSchedule whenIdle() {
        TaskSchedule schedule = new TaskSchedule();
        schedule.scheduleType = ScheduleType.IDLE;
        schedule.executeWhenIdle = true;
        schedule.maxExecutions = 0; // Unlimited
        return schedule;
    }
    
    /**
     * Private constructor
     */
    private TaskSchedule() {
        // Default values
        this.startTimeMs = 0;
        this.endTimeMs = 0;
        this.maxExecutions = 1;
        this.daysOfWeek = new boolean[7];
        this.executeImmediately = false;
        this.executeWhenIdle = false;
        this.executeOnAppLaunch = false;
        this.executeOnAppClose = false;
    }
    
    // Getters and setters
    public ScheduleType getScheduleType() {
        return scheduleType;
    }
    
    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }
    
    public long getIntervalMs() {
        return intervalMs;
    }
    
    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }
    
    public long getStartTimeMs() {
        return startTimeMs;
    }
    
    public void setStartTimeMs(long startTimeMs) {
        this.startTimeMs = startTimeMs;
    }
    
    public long getEndTimeMs() {
        return endTimeMs;
    }
    
    public void setEndTimeMs(long endTimeMs) {
        this.endTimeMs = endTimeMs;
    }
    
    public int getMaxExecutions() {
        return maxExecutions;
    }
    
    public void setMaxExecutions(int maxExecutions) {
        this.maxExecutions = maxExecutions;
    }
    
    public boolean[] getDaysOfWeek() {
        return daysOfWeek;
    }
    
    public void setDaysOfWeek(boolean[] daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }
    
    public boolean isExecuteImmediately() {
        return executeImmediately;
    }
    
    public void setExecuteImmediately(boolean executeImmediately) {
        this.executeImmediately = executeImmediately;
    }
    
    public boolean isExecuteWhenIdle() {
        return executeWhenIdle;
    }
    
    public void setExecuteWhenIdle(boolean executeWhenIdle) {
        this.executeWhenIdle = executeWhenIdle;
    }
    
    public boolean isExecuteOnAppLaunch() {
        return executeOnAppLaunch;
    }
    
    public void setExecuteOnAppLaunch(boolean executeOnAppLaunch) {
        this.executeOnAppLaunch = executeOnAppLaunch;
    }
    
    public boolean isExecuteOnAppClose() {
        return executeOnAppClose;
    }
    
    public void setExecuteOnAppClose(boolean executeOnAppClose) {
        this.executeOnAppClose = executeOnAppClose;
    }
    
    /**
     * Check if a task with this schedule is due for execution
     */
    public boolean isDue(long lastExecutionTime) {
        long now = System.currentTimeMillis();
        
        // Check if schedule is expired
        if (endTimeMs > 0 && now > endTimeMs) {
            return false;
        }
        
        // Check schedule type
        switch (scheduleType) {
            case ONE_TIME:
                // If executeImmediately is true and task has never run, it's due
                if (executeImmediately && lastExecutionTime == 0) {
                    return true;
                }
                
                // If startTimeMs is set and we've passed it, it's due (unless already executed)
                return startTimeMs > 0 && now >= startTimeMs && lastExecutionTime < startTimeMs;
                
            case RECURRING:
                // If startTimeMs is set and we haven't reached it yet, not due
                if (startTimeMs > 0 && now < startTimeMs) {
                    return false;
                }
                
                // If never executed and past start time, it's due
                if (lastExecutionTime == 0) {
                    return startTimeMs == 0 || now >= startTimeMs;
                }
                
                // Check if enough time has passed since last execution
                return now >= (lastExecutionTime + intervalMs);
                
            case DAILY:
                // If never executed and past start time, it's due
                if (lastExecutionTime == 0) {
                    return now >= startTimeMs;
                }
                
                // Check if 24 hours have passed since last execution and we're past the scheduled time
                Calendar lastExecution = Calendar.getInstance();
                lastExecution.setTimeInMillis(lastExecutionTime);
                
                Calendar scheduledTime = Calendar.getInstance();
                scheduledTime.setTimeInMillis(startTimeMs);
                
                Calendar nextExecution = Calendar.getInstance();
                nextExecution.setTimeInMillis(lastExecutionTime);
                nextExecution.add(Calendar.DAY_OF_MONTH, 1);
                nextExecution.set(Calendar.HOUR_OF_DAY, scheduledTime.get(Calendar.HOUR_OF_DAY));
                nextExecution.set(Calendar.MINUTE, scheduledTime.get(Calendar.MINUTE));
                nextExecution.set(Calendar.SECOND, 0);
                nextExecution.set(Calendar.MILLISECOND, 0);
                
                return now >= nextExecution.getTimeInMillis();
                
            case WEEKLY:
                // If we don't have any days set, not due
                boolean anyDaySet = false;
                for (boolean day : daysOfWeek) {
                    if (day) {
                        anyDaySet = true;
                        break;
                    }
                }
                
                if (!anyDaySet) {
                    return false;
                }
                
                // Check if it's the correct day of the week and time
                Calendar nowCal = Calendar.getInstance();
                int today = (nowCal.get(Calendar.DAY_OF_WEEK) - 1) % 7; // 0-6 (Sunday-Saturday)
                
                // If it's not the right day, not due
                if (!daysOfWeek[today]) {
                    return false;
                }
                
                // If it's the right day, check if we're past the time and haven't run today
                Calendar startCal = Calendar.getInstance();
                startCal.setTimeInMillis(startTimeMs);
                
                Calendar todayScheduled = Calendar.getInstance();
                todayScheduled.set(Calendar.HOUR_OF_DAY, startCal.get(Calendar.HOUR_OF_DAY));
                todayScheduled.set(Calendar.MINUTE, startCal.get(Calendar.MINUTE));
                todayScheduled.set(Calendar.SECOND, 0);
                todayScheduled.set(Calendar.MILLISECOND, 0);
                
                // Check if we've already run today
                if (lastExecutionTime > 0) {
                    Calendar lastCal = Calendar.getInstance();
                    lastCal.setTimeInMillis(lastExecutionTime);
                    
                    if (lastCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && 
                        lastCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)) {
                        return false;
                    }
                }
                
                return now >= todayScheduled.getTimeInMillis();
                
            case APP_EVENT:
                // This is handled by the app event system, not by time
                return false;
                
            case IDLE:
                // This is handled by the idle detection system, not by time
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Get the next execution time based on the schedule
     */
    public long getNextExecutionTime(long lastExecutionTime) {
        // Calculate next execution time based on schedule type and last execution
        switch (scheduleType) {
            case ONE_TIME:
                // If already executed or no start time, no next execution
                if (lastExecutionTime > 0 || startTimeMs == 0) {
                    return 0;
                }
                return startTimeMs;
                
            case RECURRING:
                // If never executed, return start time
                if (lastExecutionTime == 0) {
                    return startTimeMs > 0 ? startTimeMs : System.currentTimeMillis();
                }
                
                // Next execution is last execution + interval
                return lastExecutionTime + intervalMs;
                
            case DAILY:
                Calendar lastExecution = Calendar.getInstance();
                Calendar scheduledTime = Calendar.getInstance();
                Calendar nextExecution = Calendar.getInstance();
                
                if (lastExecutionTime > 0) {
                    lastExecution.setTimeInMillis(lastExecutionTime);
                    nextExecution.setTimeInMillis(lastExecutionTime);
                } else {
                    // If never executed, use now as reference
                    nextExecution.setTimeInMillis(System.currentTimeMillis());
                }
                
                scheduledTime.setTimeInMillis(startTimeMs);
                
                nextExecution.set(Calendar.HOUR_OF_DAY, scheduledTime.get(Calendar.HOUR_OF_DAY));
                nextExecution.set(Calendar.MINUTE, scheduledTime.get(Calendar.MINUTE));
                nextExecution.set(Calendar.SECOND, 0);
                nextExecution.set(Calendar.MILLISECOND, 0);
                
                // If next execution is in the past, move to tomorrow
                if (nextExecution.getTimeInMillis() <= System.currentTimeMillis()) {
                    nextExecution.add(Calendar.DAY_OF_MONTH, 1);
                }
                
                return nextExecution.getTimeInMillis();
                
            case WEEKLY:
                // Find the next day that matches our schedule
                Calendar now = Calendar.getInstance();
                Calendar next = Calendar.getInstance();
                Calendar start = Calendar.getInstance();
                
                start.setTimeInMillis(startTimeMs);
                
                next.set(Calendar.HOUR_OF_DAY, start.get(Calendar.HOUR_OF_DAY));
                next.set(Calendar.MINUTE, start.get(Calendar.MINUTE));
                next.set(Calendar.SECOND, 0);
                next.set(Calendar.MILLISECOND, 0);
                
                // Check if today is scheduled and we haven't run yet
                int today = (now.get(Calendar.DAY_OF_WEEK) - 1) % 7;
                boolean ranToday = false;
                
                if (lastExecutionTime > 0) {
                    Calendar last = Calendar.getInstance();
                    last.setTimeInMillis(lastExecutionTime);
                    
                    ranToday = last.get(Calendar.YEAR) == now.get(Calendar.YEAR) && 
                               last.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
                }
                
                // If today is scheduled, we haven't run yet, and it's not too late
                if (daysOfWeek[today] && !ranToday && next.after(now)) {
                    return next.getTimeInMillis();
                }
                
                // Find the next scheduled day
                for (int i = 1; i <= 7; i++) {
                    next.add(Calendar.DAY_OF_MONTH, 1);
                    int dayOfWeek = (next.get(Calendar.DAY_OF_WEEK) - 1) % 7;
                    
                    if (daysOfWeek[dayOfWeek]) {
                        return next.getTimeInMillis();
                    }
                }
                
                // No day is scheduled, return 0
                return 0;
                
            case APP_EVENT:
            case IDLE:
                // These don't have predictable next execution times
                return 0;
                
            default:
                return 0;
        }
    }
}