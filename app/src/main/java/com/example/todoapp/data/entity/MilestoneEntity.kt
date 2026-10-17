package com.example.todoapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "milestones",
    foreignKeys = [ForeignKey(
        entity = GoalEntity::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("goalId")]
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val date: Long,
    val completedTaskName: String = "",
    val completedTaskDetail: String = "",
    val timeSpent: String = "",
    val difficulties: String = "",
    val futurePlans: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
