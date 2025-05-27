package com.aiassistant.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aiassistant.database.AppDatabase;
import models.Task;
import com.aiassistant.services.TaskSchedulerService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for managing task scheduling data and actions.
 */
public class TaskSchedulerViewModel extends AndroidViewModel {
    
    private final TaskSchedulerService schedulerService;
    private final ExecutorService executor;
    private final AppDatabase database;
    
    private final MutableLiveData<List<Task>> allTasks;
    
    public TaskSchedulerViewModel(@NonNull Application application) {
        super(application);
        
        schedulerService = TaskSchedulerService.getInstance(application);
        executor = Executors.newCachedThreadPool();
        database = AppDatabase.getInstance(application);
        
        allTasks = new MutableLiveData<>();
        
        // Initial load of tasks
        loadAllTasks();
    }
    
    /**
     * Load all tasks from the database
     */
    private void loadAllTasks() {
        executor.execute(() -> {
            List<Task> tasks = database.taskDao().getAllTasks();
            allTasks.postValue(tasks);
        });
    }
    
    /**
     * Insert a new task
     */
    public void insertTask(Task task) {
        executor.execute(() -> {
            // Let service handle scheduling
            schedulerService.scheduleTask(task);
            
            // Reload tasks
            loadAllTasks();
        });
    }
    
    /**
     * Update an existing task
     */
    public void updateTask(Task task) {
        executor.execute(() -> {
            // Let service handle rescheduling
            schedulerService.rescheduleTask(task);
            
            // Reload tasks
            loadAllTasks();
        });
    }
    
    /**
     * Delete a task
     */
    public void deleteTask(Task task) {
        executor.execute(() -> {
            // Let service handle cancellation and deletion
            schedulerService.deleteTask(task);
            
            // Reload tasks
            loadAllTasks();
        });
    }
    
    /**
     * Execute a task immediately
     */
    public void executeTaskNow(Task task) {
        executor.execute(() -> {
            schedulerService.executeTaskNow(task);
            
            // Reload tasks after a delay to reflect execution status
            new android.os.Handler().postDelayed(this::loadAllTasks, 1000);
        });
    }
    
    /**
     * Get task by ID
     */
    public Task getTaskById(int taskId) {
        return database.taskDao().getTaskById(taskId);
    }
    
    /**
     * Get all tasks
     */
    public LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }
    
    /**
     * Reload tasks from database
     */
    public void reloadTasks() {
        loadAllTasks();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
