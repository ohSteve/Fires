package com.example.todoapp.ui.checkin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.TodoApplication
import com.example.todoapp.data.entity.CheckInItemEntity
import com.example.todoapp.data.entity.CheckInRecordEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CheckInViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as TodoApplication).database
    private val dao = db.checkInDao()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _currentDate = MutableStateFlow(todayString())
    val currentDate: StateFlow<String> = _currentDate

    val items: StateFlow<List<CheckInItemEntity>> = dao.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _records = MutableStateFlow<List<CheckInRecordEntity>>(emptyList())
    val records: StateFlow<List<CheckInRecordEntity>> = _records

    init {
        loadDate(_currentDate.value)
    }

    fun selectDate(dateStr: String) {
        _currentDate.value = dateStr
        loadDate(dateStr)
    }

    fun refreshCurrent(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val date = _currentDate.value
            if (date == todayString()) autoAbandonStale(date)
            ensureRecords(date)
            _records.value = dao.getRecordsForDate(date)
            onDone()
        }
    }

    fun goToToday(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val today = todayString()
            _currentDate.value = today
            autoAbandonStale(today)
            ensureRecords(today)
            _records.value = dao.getRecordsForDate(today)
            onDone()
        }
    }

    fun refreshAndAdd(onDone: () -> Unit) {
        viewModelScope.launch {
            val today = todayString()
            _currentDate.value = today
            autoAbandonStale(today)
            ensureRecords(today)
            _records.value = dao.getRecordsForDate(today)
            onDone()
        }
    }

    private fun loadDate(dateStr: String) {
        viewModelScope.launch {
            if (dateStr == todayString()) {
                autoAbandonStale(dateStr)
            }
            ensureRecords(dateStr)
            _records.value = dao.getRecordsForDate(dateStr)
        }
    }

    private suspend fun autoAbandonStale(todayStr: String) {
        val pendingBefore = dao.getPendingRecordsBefore(todayStr)
        for (r in pendingBefore) {
            dao.updateRecord(r.copy(status = "abandoned", updatedAt = System.currentTimeMillis()))
        }
    }

    private suspend fun ensureRecords(dateStr: String) {
        // 直接取数据库最新项目列表：items 是 StateFlow（异步更新），
        // 刚新增的项目在 items.value 中可能还不存在，会导致新项目不生成当日记录、界面不显示
        val allItems = dao.getAllItems().first()
        val existing = dao.getRecordsForDate(dateStr)
        val existingItemIds = existing.map { it.itemId }.toSet()

        for (item in allItems) {
            if (item.id in existingItemIds) continue
            if (item.isPaused) continue
            if (item.endDate != null && dateStr > item.endDate) continue
            if (dateStr < item.startDate) continue
            if (isItemDue(item, dateStr)) {
                dao.insertRecord(
                    CheckInRecordEntity(itemId = item.id, date = dateStr, status = "pending")
                )
            }
        }
    }

    fun isItemDue(item: CheckInItemEntity, dateStr: String): Boolean {
        return when (item.cycleType) {
            "daily" -> true
            "every_two_days" -> {
                val daysDiff = daysBetween(item.startDate, dateStr)
                daysDiff >= 0 && daysDiff % 2 == 0L
            }
            "weekly" -> {
                val cal = parseDate(dateStr)
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val adjustedDow = if (dow == Calendar.SUNDAY) 7 else dow - 1
                val configured = item.cycleConfig.split(",").mapNotNull { it.trim().toIntOrNull() }
                adjustedDow in configured
            }
            "custom" -> {
                item.cycleConfig.split(",").map { it.trim() }.contains(dateStr)
            }
            else -> false
        }
    }

    fun markCompleted(record: CheckInRecordEntity) {
        if (_currentDate.value != todayString()) return
        viewModelScope.launch {
            dao.updateRecord(record.copy(status = "completed", updatedAt = System.currentTimeMillis()))
            _records.value = dao.getRecordsForDate(_currentDate.value)
        }
    }

    fun markAbandoned(record: CheckInRecordEntity) {
        if (_currentDate.value != todayString()) return
        viewModelScope.launch {
            dao.updateRecord(record.copy(status = "abandoned", updatedAt = System.currentTimeMillis()))
            _records.value = dao.getRecordsForDate(_currentDate.value)
        }
    }

    fun toggleRecord(record: CheckInRecordEntity) {
        if (_currentDate.value != todayString()) return
        viewModelScope.launch {
            val newStatus = when (record.status) {
                "completed", "abandoned" -> "pending"
                else -> "completed"
            }
            dao.updateRecord(record.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
            _records.value = dao.getRecordsForDate(_currentDate.value)
        }
    }

    fun addItem(name: String, requirements: String, cycleType: String, cycleConfig: String, startDate: String = todayString()) {
        if (name.isBlank()) return
        viewModelScope.launch {
            dao.insertItem(
                CheckInItemEntity(
                    name = name.trim(),
                    requirements = requirements.trim(),
                    startDate = startDate,
                    cycleType = cycleType,
                    cycleConfig = cycleConfig
                )
            )
            val current = _currentDate.value
            ensureRecords(current)
            _records.value = dao.getRecordsForDate(current)
        }
    }

    // --- Item management ---

    fun updateEndDate(item: CheckInItemEntity, newEndDate: String?) {
        viewModelScope.launch {
            dao.updateItem(item.copy(endDate = newEndDate))
            // Regenerate records from today if extending
            if (newEndDate == null || newEndDate >= todayString()) {
                dao.deleteRecordsFromDate(item.id, todayString())
                loadDate(_currentDate.value)
            }
        }
    }

    fun updateFrequency(item: CheckInItemEntity, newCycleType: String, newCycleConfig: String) {
        viewModelScope.launch {
            dao.updateItem(item.copy(cycleType = newCycleType, cycleConfig = newCycleConfig))
            // Delete future pending records and regenerate
            if (!item.isPaused) {
                dao.deleteRecordsFromDate(item.id, todayString())
                loadDate(_currentDate.value)
            }
        }
    }

    fun deleteItem(item: CheckInItemEntity) {
        viewModelScope.launch {
            // Only delete the item definition, not the records
            dao.deleteItem(item)
            loadDate(_currentDate.value)
        }
    }

    fun updateItemDetails(item: CheckInItemEntity, name: String, requirements: String) {
        viewModelScope.launch {
            dao.updateItem(item.copy(name = name.trim(), requirements = requirements.trim()))
        }
    }

    fun togglePause(item: CheckInItemEntity) {
        viewModelScope.launch {
            if (item.isPaused) {
                // Resume: enable and regenerate from today
                dao.updateItem(item.copy(isPaused = false))
                dao.deleteRecordsFromDate(item.id, todayString())
                loadDate(_currentDate.value)
            } else {
                // Pause: disable and remove future records
                dao.updateItem(item.copy(isPaused = true))
                dao.deleteRecordsFromDate(item.id, todayString())
                loadDate(_currentDate.value)
            }
        }
    }

    suspend fun getStats(item: CheckInItemEntity): Pair<Int, Int> {
        val today = todayString()
        val effectiveEnd = item.endDate ?: today
        val endForCalc = if (effectiveEnd > today) today else effectiveEnd
        val total = dao.getTotalDaysInRange(item.id, item.startDate, endForCalc)
        val completed = dao.getCompletedCount(item.id)
        return Pair(completed, total)
    }

    // --- Helpers ---

    private fun daysBetween(from: String, to: String): Long {
        return try {
            val fromDate = sdf.parse(from) ?: return 0
            val toDate = sdf.parse(to) ?: return 0
            (toDate.time - fromDate.time) / (24 * 60 * 60 * 1000)
        } catch (e: Exception) { 0 }
    }

    private fun parseDate(dateStr: String): Calendar {
        val cal = Calendar.getInstance()
        try { sdf.parse(dateStr)?.let { cal.time = it } } catch (_: Exception) {}
        return cal
    }

    fun addBackfillItem(item: CheckInItemEntity) {
        val today = todayString()
        viewModelScope.launch {
            val existing = dao.getRecordsForDate(today)
            if (existing.none { it.itemId == item.id }) {
                dao.insertRecord(CheckInRecordEntity(itemId = item.id, date = today, status = "pending", isBackfill = true))
                _records.value = dao.getRecordsForDate(today)
            }
        }
    }

    companion object {
        fun todayString(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
        fun formatDisplayDate(dateStr: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(dateStr) ?: return dateStr
                SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(date)
            } catch (e: Exception) { dateStr }
        }
    }
}
