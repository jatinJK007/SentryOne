package com.example.sentryone.Database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SOSHistoryDao {
    @Insert
    suspend fun insert(sosHistory: SOSHistoryClass)

    @Query("SELECT * FROM SOS_Trigger_History ORDER BY trigger_time DESC")
    fun getHistory(): LiveData<List<SOSHistoryClass>>
}