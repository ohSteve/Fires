package com.example.todoapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ddl: Long? = null,
    val implementationPath: String = "",
    val isAchieved: Boolean = false,
    val achievedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
