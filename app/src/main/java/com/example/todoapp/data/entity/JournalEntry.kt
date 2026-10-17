package com.example.todoapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journals")
data class JournalEntry(
    @PrimaryKey val date: String,
    val notes: String = "",
    val entertainment: String = "",
    val instantGratification: Boolean = false,
    val usageMinutes: Long? = null,
    val usageTopApps: String = "",
    val usageUpdateTime: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
