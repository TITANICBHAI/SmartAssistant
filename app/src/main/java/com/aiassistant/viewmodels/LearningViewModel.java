package com.aiassistant.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aiassistant.database.AppDatabase;
import models.LearnedData;
import com.aiassistant.services.LearningService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for managing learning data and actions.
 */
public class LearningViewModel extends AndroidViewModel {
    
    private final LearningService learningService;
    private final ExecutorService executor;
    private final AppDatabase database;
    
    private final MutableLiveData<List<LearnedData>> allLearnedData;
    
    public LearningViewModel(@NonNull Application application) {
        super(application);
        
        learningService = LearningService.getInstance(application);
        executor = Executors.newCachedThreadPool();
        database = AppDatabase.getInstance(application);
        
        allLearnedData = new MutableLiveData<>();
        
        // Initial load of data
        loadAllLearnedData();
    }
    
    /**
     * Load all learned data from the database
     */
    private void loadAllLearnedData() {
        executor.execute(() -> {
            List<LearnedData> data = database.learnedDataDao().getAllLearnedData();
            allLearnedData.postValue(data);
        });
    }
    
    /**
     * Learn from a video file
     */
    public void learnFromVideo(String videoPath) {
        learningService.learnFromVideo(videoPath);
        
        // Reload data after a delay to show new entries
        new android.os.Handler().postDelayed(this::loadAllLearnedData, 5000);
    }
    
    /**
     * Clear all learned data
     */
    public void clearAllLearnedData() {
        executor.execute(() -> {
            database.learnedDataDao().deleteAllLearnedData();
            loadAllLearnedData();
        });
    }
    
    /**
     * Set learning rate
     */
    public void setLearningRate(float rate) {
        learningService.setLearningRate(rate);
    }
    
    /**
     * Set whether to collect user data
     */
    public void setCollectUserData(boolean collect) {
        learningService.setCollectUserData(collect);
    }
    
    /**
     * Get learning enabled status
     */
    public LiveData<Boolean> getLearningEnabled() {
        return learningService.getLearningEnabled();
    }
    
    /**
     * Set learning enabled status
     */
    public void setLearningEnabled(boolean enabled) {
        learningService.setLearningEnabled(enabled);
    }
    
    /**
     * Get learning status
     */
    public LiveData<String> getLearningStatus() {
        return learningService.getLearningStatus();
    }
    
    /**
     * Get learning progress
     */
    public LiveData<Integer> getLearningProgress() {
        return learningService.getLearningProgress();
    }
    
    /**
     * Get all learned data
     */
    public LiveData<List<LearnedData>> getAllLearnedData() {
        return allLearnedData;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
