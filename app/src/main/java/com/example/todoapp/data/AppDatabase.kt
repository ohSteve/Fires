package com.example.todoapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.todoapp.data.dao.CheckInDao
import com.example.todoapp.data.dao.GoalDao
import com.example.todoapp.data.dao.JournalDao
import com.example.todoapp.data.dao.MilestoneDao
import com.example.todoapp.data.dao.TodoItemDao
import com.example.todoapp.data.entity.CheckInItemEntity
import com.example.todoapp.data.entity.CheckInRecordEntity
import com.example.todoapp.data.entity.GoalEntity
import com.example.todoapp.data.entity.JournalEntry
import com.example.todoapp.data.entity.MilestoneEntity
import com.example.todoapp.data.entity.TodoItemEntity

@Database(
    entities = [GoalEntity::class, MilestoneEntity::class, TodoItemEntity::class, JournalEntry::class,
                CheckInItemEntity::class, CheckInRecordEntity::class],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun todoItemDao(): TodoItemDao
    abstract fun journalDao(): JournalDao
    abstract fun checkInDao(): CheckInDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, AppConstants.DATABASE_NAME
                ).build().also { INSTANCE = it }
            }
        }

        fun closeInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
