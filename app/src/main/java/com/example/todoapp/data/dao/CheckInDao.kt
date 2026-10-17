package com.example.todoapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoapp.data.entity.CheckInItemEntity
import com.example.todoapp.data.entity.CheckInRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {

    // Items
    @Query("SELECT * FROM checkin_items ORDER BY createdAt ASC")
    fun getAllItems(): Flow<List<CheckInItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CheckInItemEntity): Long

    @Update
    suspend fun updateItem(item: CheckInItemEntity)

    @Delete
    suspend fun deleteItem(item: CheckInItemEntity)

    // Records
    @Query("SELECT * FROM checkin_records WHERE date = :date")
    suspend fun getRecordsForDate(date: String): List<CheckInRecordEntity>

    @Query("SELECT * FROM checkin_records WHERE date < :date AND status = 'pending'")
    suspend fun getPendingRecordsBefore(date: String): List<CheckInRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: CheckInRecordEntity): Long

    @Update
    suspend fun updateRecord(record: CheckInRecordEntity)

    @Query("DELETE FROM checkin_records WHERE itemId = :itemId AND date >= :fromDate")
    suspend fun deleteRecordsFromDate(itemId: Long, fromDate: String)

    @Query("DELETE FROM checkin_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("SELECT COUNT(*) FROM checkin_records WHERE itemId = :itemId AND status = 'completed'")
    suspend fun getCompletedCount(itemId: Long): Int

    @Query("SELECT COUNT(*) FROM checkin_records WHERE itemId = :itemId AND date >= :startDate AND date <= :endDate")
    suspend fun getTotalDaysInRange(itemId: Long, startDate: String, endDate: String): Int
}
