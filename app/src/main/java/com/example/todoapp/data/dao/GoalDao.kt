package com.example.todoapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoapp.data.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query(
        """
        SELECT * FROM goals ORDER BY
            isAchieved ASC,
            CASE WHEN isAchieved = 0 THEN
                CASE WHEN ddl IS NULL THEN 1 ELSE 0 END
            END,
            ddl ASC,
            achievedAt DESC
        """
    )
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalById(id: Long): Flow<GoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE isAchieved = 1 AND achievedAt >= :dayStart AND achievedAt < :dayEnd ORDER BY achievedAt DESC")
    suspend fun getAchievedOnDay(dayStart: Long, dayEnd: Long): List<GoalEntity>
}
