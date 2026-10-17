package com.example.todoapp.ui.journal

import android.app.Application
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.TodoApplication
import com.example.todoapp.data.entity.GoalEntity
import com.example.todoapp.data.entity.JournalEntry
import com.example.todoapp.data.entity.MilestoneEntity
import com.example.todoapp.data.entity.TodoItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as TodoApplication).database
    private val journalDao = db.journalDao()
    private val goalDao = db.goalDao()
    private val milestoneDao = db.milestoneDao()
    private val todoDao = db.todoItemDao()
    private val checkInDao = db.checkInDao()

    private val _currentDate = MutableStateFlow(todayString())
    val currentDate: StateFlow<String> = _currentDate

    private val _goalsAchieved = MutableStateFlow<List<GoalEntity>>(emptyList())
    val goalsAchieved: StateFlow<List<GoalEntity>> = _goalsAchieved

    private val _milestones = MutableStateFlow<List<Pair<MilestoneEntity, String>>>(emptyList())
    val milestones: StateFlow<List<Pair<MilestoneEntity, String>>> = _milestones

    private val _todosCompleted = MutableStateFlow<List<TodoItemEntity>>(emptyList())
    val todosCompleted: StateFlow<List<TodoItemEntity>> = _todosCompleted

    private val _checkInRecords = MutableStateFlow<List<com.example.todoapp.data.entity.CheckInRecordEntity>>(emptyList())
    val checkInRecords: StateFlow<List<com.example.todoapp.data.entity.CheckInRecordEntity>> = _checkInRecords

    private val _checkInItems = MutableStateFlow<List<com.example.todoapp.data.entity.CheckInItemEntity>>(emptyList())
    val checkInItems: StateFlow<List<com.example.todoapp.data.entity.CheckInItemEntity>> = _checkInItems

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes

    private val _entertainment = MutableStateFlow("")
    val entertainment: StateFlow<String> = _entertainment

    private val _instantGratification = MutableStateFlow(false)
    val instantGratification: StateFlow<Boolean> = _instantGratification

    private val _usageMinutes = MutableStateFlow<Long?>(null)
    val usageMinutes: StateFlow<Long?> = _usageMinutes

    private val _topApps = MutableStateFlow<List<Pair<String, Long>>>(emptyList())
    val topApps: StateFlow<List<Pair<String, Long>>> = _topApps

    private val _usageUpdateTime = MutableStateFlow<String?>(null)
    val usageUpdateTime: StateFlow<String?> = _usageUpdateTime

    private val _canEdit = MutableStateFlow(true)
    val canEdit: StateFlow<Boolean> = _canEdit

    // 今日之前的日志默认锁定；经用户双击确认后可临时解锁本次查看
    private val _pastEditUnlocked = MutableStateFlow(false)
    val pastEditUnlocked: StateFlow<Boolean> = _pastEditUnlocked

    private val _canUpdateUsage = MutableStateFlow(true)
    val canUpdateUsage: StateFlow<Boolean> = _canUpdateUsage

    // Tracking last-saved values to avoid duplicate writes
    private var savedNotes = ""
    private var savedEntertainment = ""
    private var savedInstantGratification = false

    init {
        viewModelScope.launch { loadDate(_currentDate.value) }
    }

    fun selectDate(dateStr: String) {
        // Force-save current state before switching
        saveNow()
        // 切换到其他日期后，历史日志重新回到锁定状态
        _pastEditUnlocked.value = false
        viewModelScope.launch {
            _currentDate.value = dateStr
            loadDateSync(dateStr)
        }
    }

    fun refreshSummary() {
        viewModelScope.launch { loadSummary(_currentDate.value) }
    }

    fun goToToday(onDone: () -> Unit = {}) {
        saveNow()
        // 切换到其他日期后，历史日志重新回到锁定状态
        _pastEditUnlocked.value = false
        viewModelScope.launch {
            val today = todayString()
            _currentDate.value = today
            // 必须完整加载该日期的手动笔记与使用时长，否则会把上一个日期的内容带到今天
            loadDateSync(today)
            onDone()
        }
    }

    private fun loadDate(dateStr: String) {
        viewModelScope.launch { loadDateSync(dateStr) }
    }

    private suspend fun loadDateSync(dateStr: String) {
        loadSummary(dateStr)
        val entry = journalDao.getEntry(dateStr).first()
        _notes.value = entry?.notes ?: ""
        _entertainment.value = entry?.entertainment ?: ""
        _instantGratification.value = entry?.instantGratification ?: false
        savedNotes = _notes.value
        savedEntertainment = _entertainment.value
        savedInstantGratification = _instantGratification.value

        // Restore saved usage data
        _usageMinutes.value = entry?.usageMinutes
        _usageUpdateTime.value = entry?.usageUpdateTime
        _topApps.value = if (entry?.usageTopApps.isNullOrBlank()) emptyList()
            else entry!!.usageTopApps.split("|||").mapNotNull { part ->
                val split = part.split(":::")
                if (split.size == 2) split[0] to (split[1].toLongOrNull() ?: 0L) else null
            }

        refreshEditState(dateStr)
    }

    private suspend fun loadSummary(dateStr: String) {
        // Auto-abandon all pending check-in records from before today (same logic as CheckInViewModel)
        val today = todayString()
        if (dateStr == today) {
            val staleRecords = checkInDao.getPendingRecordsBefore(today)
            for (r in staleRecords) {
                checkInDao.updateRecord(r.copy(status = "abandoned", updatedAt = System.currentTimeMillis()))
            }
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayStart = sdf.parse(dateStr)?.time ?: return
        val dayEnd = dayStart + 24 * 60 * 60 * 1000L

        _goalsAchieved.value = goalDao.getAchievedOnDay(dayStart, dayEnd)
        val ms = milestoneDao.getMilestonesOnDay(dayStart, dayEnd)
        val goalMap = goalDao.getAllGoals().first().associate { it.id to it.name }
        _milestones.value = ms.map { it to (goalMap[it.goalId] ?: "未知目标") }
        _todosCompleted.value = todoDao.getCompletedOnDay(dayStart, dayEnd)
        _checkInRecords.value = checkInDao.getRecordsForDate(dateStr)
            .filter { it.status == "completed" || it.status == "abandoned" }
        _checkInItems.value = checkInDao.getAllItems().first()
    }

    private fun refreshEditState(dateStr: String) {
        _canEdit.value = canEditDate(dateStr)
        _canUpdateUsage.value = (dateStr == todayString())
    }

    // --- Auto-save on every change ---

    /** 仅允许解锁“今天之前”的历史日志做临时修改；未来日期始终锁定 */
    fun unlockPastEditing() {
        val date = _currentDate.value
        if (date >= todayString()) return
        _pastEditUnlocked.value = true
    }

    /** 手动笔记类内容的写入守卫：仅当天或已临时解锁的历史日期允许写入 */
    private fun manualEditAllowed(): Boolean {
        val date = _currentDate.value
        val today = todayString()
        return date == today || (date < today && _pastEditUnlocked.value)
    }

    fun updateNotes(text: String) {
        if (!manualEditAllowed()) return
        _notes.value = text
        if (text == savedNotes) return
        savedNotes = text
        doSave()
    }

    fun updateEntertainment(text: String) {
        if (!manualEditAllowed()) return
        _entertainment.value = text
        if (text == savedEntertainment) return
        savedEntertainment = text
        doSave()
    }

    fun updateInstantGratification(value: Boolean) {
        if (!manualEditAllowed()) return
        _instantGratification.value = value
        if (value == savedInstantGratification) return
        savedInstantGratification = value
        doSave()
    }

    private fun saveNow() {
        // 无实际改动时不做任何写入：数据只应因用户输入而变化，
        // 纯粹浏览日期不应产生（空的）日志记录
        if (_notes.value == savedNotes &&
            _entertainment.value == savedEntertainment &&
            _instantGratification.value == savedInstantGratification
        ) return
        savedNotes = _notes.value
        savedEntertainment = _entertainment.value
        savedInstantGratification = _instantGratification.value
        doSave()
    }

    /**
     * 写入当前日期的日志行。
     * 日期与内容在调用点同步捕获为快照后再异步落库——
     * 保证任何情况下都不会把 A 日期的内容写进 B 日期的行。
     */
    private fun doSave() {
        val date = _currentDate.value
        val notes = _notes.value
        val entertainment = _entertainment.value
        val instantGratification = _instantGratification.value
        val usageMinutes = _usageMinutes.value
        val usageTopApps = _topApps.value.joinToString("|||") { "${it.first}:::${it.second}" }
        val usageUpdateTime = _usageUpdateTime.value
        viewModelScope.launch {
            journalDao.upsert(
                JournalEntry(
                    date = date,
                    notes = notes,
                    entertainment = entertainment,
                    instantGratification = instantGratification,
                    usageMinutes = usageMinutes,
                    usageTopApps = usageTopApps,
                    usageUpdateTime = usageUpdateTime
                )
            )
        }
    }

    // --- Phone usage ---

    fun loadUsageStats() {
        val context = getApplication<Application>()
        if (!checkUsageStatsPermission(context)) { _usageMinutes.value = -1; return }
        val dateStr = _currentDate.value
        if (dateStr != todayString()) return
        viewModelScope.launch {
            try {
                val pm = context.packageManager

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dayStart = sdf.parse(dateStr)?.time ?: return@launch
                val dayEnd = dayStart + 24 * 60 * 60 * 1000L
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val stats: List<UsageStats> = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStart, dayEnd)
                val totalMs = stats.sumOf { it.totalTimeInForeground }
                _usageMinutes.value = totalMs / (60 * 1000)
                _topApps.value = stats
                    .filter { it.totalTimeInForeground > 0 }
                    .sortedByDescending { it.totalTimeInForeground }
                    .take(5)
                    .map { stat ->
                        val name = try {
                            pm.getApplicationInfo(stat.packageName, 0).loadLabel(pm).toString()
                        } catch (_: Exception) { stat.packageName }
                        name to stat.totalTimeInForeground
                    }
                _usageUpdateTime.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                doSave()
            } catch (e: Exception) { _usageMinutes.value = 0 }
        }
    }

    fun hasUsageStatsPermission(): Boolean = checkUsageStatsPermission(getApplication())

    companion object {
        fun todayString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        fun formatDisplayDate(dateStr: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(dateStr) ?: return dateStr
                SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(date)
            } catch (e: Exception) { dateStr }
        }

        @Suppress("DEPRECATION")
        fun checkUsageStatsPermission(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.packageName
            )
            return mode == android.app.AppOpsManager.MODE_ALLOWED
        }

        fun canEditDate(dateStr: String): Boolean = dateStr == todayString()

    }
}
