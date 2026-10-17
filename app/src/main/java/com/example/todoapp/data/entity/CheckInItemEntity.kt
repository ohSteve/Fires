package com.example.todoapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checkin_items")
data class CheckInItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val requirements: String = "",
    val startDate: String = "",
    val endDate: String? = null,
    val cycleType: String = "daily",
    val cycleConfig: String = "",
    val isPaused: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
