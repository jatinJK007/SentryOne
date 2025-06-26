package com.example.sentryone.Database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SOS_Trigger_History")
data class SOSHistoryClass(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "trigger_time") val triggerTime: String?,
//    @ColumnInfo(name = "trigger_type") val triggerType: String?,
    @ColumnInfo(name = "location_latitude") val locationLatitude: String?,
    @ColumnInfo(name = "location_longitude") val locationLongitude: String?,
//    @ColumnInfo(name = "contacts_notified") val contactsNotified: String?
)
