package com.example.todoapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoapp.data.entity.MilestoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {

    @Query("SELECT * FROM milestones WHERE goalId = :goalId ORDER BY date ASC")
    fun getMilestonesByGoalId(goalId: Long): Flow<List<MilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(milestone: MilestoneEntity): Long

    @Update
    suspend fun update(milestone: MilestoneEntity)

    @Delete
    suspend fun delete(milestone: MilestoneEntity)

    @Query("SELECT * FROM milestones WHERE date >= :dayStart AND date < :dayEnd ORDER BY date ASC")
    suspend fun getMilestonesOnDay(dayStart: Long, dayEnd: Long): List<MilestoneEntity>

    @Query("SELECT * FROM milestones")
    fun getAllMilestones(): Flow<List<MilestoneEntity>>
}
