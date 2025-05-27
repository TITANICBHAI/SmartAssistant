package com.aiassistant.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import models.LearnedData;

import java.util.List;

/**
 * Data Access Object for LearnedData entity.
 */
@Dao
public interface LearnedDataDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertLearnedData(LearnedData learnedData);
    
    @Update
    void updateLearnedData(LearnedData learnedData);
    
    @Delete
    void deleteLearnedData(LearnedData learnedData);
    
    @Query("SELECT * FROM learned_data ORDER BY learnedAt DESC")
    List<LearnedData> getAllLearnedData();
    
    @Query("SELECT * FROM learned_data WHERE packageName = :packageName ORDER BY confidence DESC")
    List<LearnedData> getLearnedDataByPackage(String packageName);
    
    @Query("SELECT * FROM learned_data WHERE confidence >= :minConfidence ORDER BY confidence DESC")
    List<LearnedData> getHighConfidenceData(float minConfidence);
    
    @Query("SELECT * FROM learned_data WHERE id = :id")
    LearnedData getLearnedDataById(int id);
    
    @Query("SELECT * FROM learned_data ORDER BY learnedAt DESC")
    LiveData<List<LearnedData>> getAllLearnedDataLiveData();
    
    @Query("DELETE FROM learned_data")
    void deleteAllLearnedData();
    
    @Query("SELECT * FROM learned_data WHERE isUserApproved = 1 ORDER BY confidence DESC")
    List<LearnedData> getUserApprovedData();
    
    @Query("SELECT * FROM learned_data WHERE patternType = :patternType ORDER BY confidence DESC")
    List<LearnedData> getLearnedDataByType(String patternType);
}
