package com.example.todoapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoapp.data.entity.TodoItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoItemDao {

    @Query(
        """
        SELECT * FROM todo_items ORDER BY
            isCompleted ASC,
            CASE WHEN isCompleted = 0 THEN
                CASE WHEN ddl IS NULL THEN 1 ELSE 0 END
            END,
            ddl ASC,
            completedAt DESC
        """
    )
    fun getAllTodos(): Flow<List<TodoItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoItemEntity): Long

    @Update
    suspend fun update(todo: TodoItemEntity)

    @Delete
    suspend fun delete(todo: TodoItemEntity)

    @Query("SELECT * FROM todo_items WHERE isCompleted = 1 AND completedAt >= :dayStart AND completedAt < :dayEnd ORDER BY completedAt DESC")
    suspend fun getCompletedOnDay(dayStart: Long, dayEnd: Long): List<TodoItemEntity>
}
