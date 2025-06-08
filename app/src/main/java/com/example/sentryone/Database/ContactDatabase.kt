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

            Log.d("TAG", "getDatabase: databse is about to create")
            return INSTANCE ?: synchronized(this) {
                Log.d("TAG", "getDatabase: in the process of making db ")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ContactDatabase::class.java,
                    "emergency_db"
                )
                    .build()
//                    .also { INSTANCE = it }
                INSTANCE = instance
                instance
            }
            Log.d("TAG", "getDatabase: db created ")

        }
    }
}