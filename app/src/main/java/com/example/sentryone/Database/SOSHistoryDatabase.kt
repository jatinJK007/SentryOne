package com.example.sentryone.Database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SOSHistoryClass::class], version = 1, exportSchema = false)
abstract class SOSHistoryDatabase : RoomDatabase(){
    abstract fun sosDao(): SOSHistoryDao

    companion object {
        @Volatile
        private var INSTANCE : SOSHistoryDatabase? = null
        fun getDatabase(context: Context): SOSHistoryDatabase {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SOSHistoryDatabase::class.java,
                    "sos_history_db"
                )
                    .build()
                INSTANCE = instance
                instance
            }
            Log.d("TAG", "getDatabase:SOS db created ")
        }
    }
}