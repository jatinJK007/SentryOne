package com.example.sentryone.Database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EmergencyContactDao {
    @Insert
    suspend fun insert(contact: EmergencyContact)


    @Query("SELECT * FROM emergency_contacts")
    fun getAll(): LiveData<List<EmergencyContact>>

    @Delete
    suspend fun deleteItem(contact: EmergencyContact)
}