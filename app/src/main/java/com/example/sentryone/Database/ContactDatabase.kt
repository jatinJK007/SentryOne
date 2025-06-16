package com.example.sentryone.Database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EmergencyContact::class], version = 1)
abstract class ContactDatabase : RoomDatabase(){
    abstract fun contactDao(): EmergencyContactDao

    companion object {
        @Volatile
        private var INSTANCE : ContactDatabase? = null
        fun getDatabase(context: Context): ContactDatabase {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ContactDatabase::class.java,
                    "emergency_db"
                )
                    .build()
                INSTANCE = instance
                instance
            }
            Log.d("TAG", "getDatabase: db created ")
        }
    }
}