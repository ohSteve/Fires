package com.example.todoapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checkin_records")
data class CheckInRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val date: String,
    val status: String = "pending",
    val isBackfill: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
