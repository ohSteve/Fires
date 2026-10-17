package com.example.todoapp.ui.search

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.TodoApplication
import com.example.todoapp.data.entity.GoalEntity
import com.example.todoapp.data.entity.JournalEntry
import com.example.todoapp.data.entity.MilestoneEntity
import com.example.todoapp.data.entity.TodoItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 检索分类 */
enum class SearchCategory(val label: String) {
    JOURNAL("日志"),
    GOAL("目标"),
    TODO("待办")
}

/**
 * 单条检索命中：来源标注 + 日期标注 + 排序键 + 命中原文 + 跳转信息。
 * journalDate 非空表示日志类命中（跳转到该日期）；goalId 非空表示目标类命中（跳转到目标详情）。
 */
data class SearchHit(
    val category: SearchCategory,
    val source: String,
    val dateLabel: String,
    val sortKey: Long?,
    val text: String,
    val journalDate: String? = null,
    val goalId: Long? = null
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as TodoApplication).database
    private val journalDao = db.journalDao()
    private val goalDao = db.goalDao()
    private val milestoneDao = db.milestoneDao()
    private val todoDao = db.todoItemDao()
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _keyword = MutableStateFlow("")
    val keyword: StateFlow<String> = _keyword

    private val _selectedCategories = MutableStateFlow(SearchCategory.entries.toSet())
    val selectedCategories: StateFlow<Set<SearchCategory>> = _selectedCategories

    private val _results = MutableStateFlow<List<SearchHit>>(emptyList())
    val results: StateFlow<List<SearchHit>> = _results

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history

    // 数据源快照：跟随 Room Flow 实时更新，新加入/修改的内容立即可被检索
    private var journals: List<JournalEntry> = emptyList()
    private var goals: List<GoalEntity> = emptyList()
    private var milestonePairs: List<Pair<MilestoneEntity, String>> = emptyList()
    private var completedTodos: List<TodoItemEntity> = emptyList()

    init {
        _history.value = loadHistory()
        viewModelScope.launch {
            combine(
                journalDao.getAllJournals(),
                goalDao.getAllGoals(),
                milestoneDao.getAllMilestones(),
                todoDao.getAllTodos()
            ) { journalList, goalList, milestoneList, todoList ->
                Snapshot(journalList, goalList, milestoneList, todoList)
            }.collect { snapshot ->
                applySnapshot(snapshot)
                runSearch()
            }
        }
    }

    private data class Snapshot(
        val journals: List<JournalEntry>,
        val goals: List<GoalEntity>,
        val milestones: List<MilestoneEntity>,
        val todos: List<TodoItemEntity>
    )

    private fun applySnapshot(snapshot: Snapshot) {
        journals = snapshot.journals
        goals = snapshot.goals
        val goalNames = snapshot.goals.associate { it.id to it.name }
        milestonePairs = snapshot.milestones.map { it to (goalNames[it.goalId] ?: "未知目标") }
        completedTodos = snapshot.todos.filter { it.isCompleted }
    }

    // --- 检索 ---

    fun setKeyword(value: String) {
        _keyword.value = value
        runSearch()
    }

    /**
     * 分类选择规则：
     * - 全选状态下点任意一个 → 单选该分类；
     * - 已选中的（非全选）点击 → 取消，但只剩一个时不允许取消；
     * - 未选中的点击 → 追加选中。
     */
    fun toggleCategory(category: SearchCategory) {
        val current = _selectedCategories.value
        val all = SearchCategory.entries.toSet()
        _selectedCategories.value = when {
            current == all -> setOf(category)
            category in current -> if (current.size == 1) current else current - category
            else -> current + category
        }
        runSearch()
    }

    private fun runSearch() {
        val kw = _keyword.value.trim()
        if (kw.isEmpty()) {
            _results.value = emptyList()
            return
        }
        val categories = _selectedCategories.value
        val hits = mutableListOf<SearchHit>()

        if (SearchCategory.JOURNAL in categories) {
            for (j in journals) {
                val dateLabel = formatJournalDate(j.date)
                val ts = journalTimestamp(j.date)
                if (j.notes.contains(kw, ignoreCase = true)) {
                    hits += SearchHit(SearchCategory.JOURNAL, "日志 · 今天还干了啥", dateLabel, ts, j.notes,
                        journalDate = j.date)
                }
                if (j.entertainment.contains(kw, ignoreCase = true)) {
                    hits += SearchHit(SearchCategory.JOURNAL, "日志 · 娱乐项目", dateLabel, ts, j.entertainment,
                        journalDate = j.date)
                }
            }
        }

        if (SearchCategory.GOAL in categories) {
            for (g in goals) {
                val achieved = g.isAchieved && g.achievedAt != null
                val dateLabel = if (achieved) "完成于 ${formatDay(g.achievedAt!!)}" else "未完成"
                val sortKey = if (achieved) g.achievedAt else null
                if (g.name.contains(kw, ignoreCase = true)) {
                    hits += SearchHit(SearchCategory.GOAL, "目标 · 目标名称", dateLabel, sortKey, g.name,
                        goalId = g.id)
                }
                if (g.implementationPath.contains(kw, ignoreCase = true)) {
                    hits += SearchHit(SearchCategory.GOAL, "目标 · 目标描述", dateLabel, sortKey, g.implementationPath,
                        goalId = g.id)
                }
            }
            for ((m, goalName) in milestonePairs) {
                val dateLabel = formatDay(m.date)
                if (m.completedTaskName.contains(kw, ignoreCase = true)) {
                    hits += SearchHit(SearchCategory.GOAL, "目标 · 里程碑任务名称", dateLabel, m.date,
                        "[$goalName] ${m.completedTaskName}", goalId = m.goalId)
                }
                if (m.completedTaskDetail.contains(kw, ignoreCase = true)) {
                    hits += SearchHit(SearchCategory.GOAL, "目标 · 里程碑具体内容", dateLabel, m.date,
                        "[$goalName] ${m.completedTaskDetail}", goalId = m.goalId)
                }
                if (m.difficulties.contains(kw, ignoreCase = true)) {
                    hits += SearchHit(SearchCategory.GOAL, "目标 · 里程碑难点", dateLabel, m.date,
                        "[$goalName] ${m.difficulties}", goalId = m.goalId)
                }
                if (m.futurePlans.contains(kw, ignoreCase = true)) {
                    hits += SearchHit(SearchCategory.GOAL, "目标 · 里程碑未来计划", dateLabel, m.date,
                        "[$goalName] ${m.futurePlans}", goalId = m.goalId)
                }
            }
        }

        if (SearchCategory.TODO in categories) {
            for (t in completedTodos) {
                if (t.content.contains(kw, ignoreCase = true)) {
                    val ts = t.completedAt ?: t.createdAt
                    hits += SearchHit(SearchCategory.TODO, "待办 · 已完成", "完成于 ${formatDay(ts)}", ts, t.content)
                }
            }
        }

        // 排序：有日期的按时间从近到远；无日期（未完成目标）排在最后
        _results.value = hits.sortedWith(
            compareByDescending<SearchHit> { it.sortKey != null }
                .thenByDescending { it.sortKey ?: 0L }
        )
    }

    // --- 搜索历史（本机 SharedPreferences，不写数据库）---

    fun commitToHistory() {
        val kw = _keyword.value.trim()
        if (kw.isEmpty()) return
        val updated = (listOf(kw) + _history.value.filter { it != kw }).take(MAX_HISTORY)
        _history.value = updated
        saveHistory(updated)
    }

    fun removeHistory(item: String) {
        val updated = _history.value.filter { it != item }
        _history.value = updated
        saveHistory(updated)
    }

    fun clearHistory() {
        _history.value = emptyList()
        saveHistory(emptyList())
    }

    private fun saveHistory(items: List<String>) {
        prefs.edit().putString(KEY_HISTORY, JSONArray(items).toString()).apply()
    }

    private fun loadHistory(): List<String> {
        return try {
            val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                arr.optString(i, "").takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val PREFS_NAME = "search_history"
        private const val KEY_HISTORY = "history_keywords"
        private const val MAX_HISTORY = 30

        private fun journalTimestamp(date: String): Long =
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)?.time ?: 0L
            } catch (e: Exception) { 0L }

        /** 日志日期显示格式：2026年10月10日 */
        private fun formatJournalDate(date: String): String =
            try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                if (d != null) SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(d) else date
            } catch (e: Exception) { date }

        /** 其他日期显示格式：2026/10/10 */
        private fun formatDay(timestamp: Long): String =
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(timestamp))
    }
}
