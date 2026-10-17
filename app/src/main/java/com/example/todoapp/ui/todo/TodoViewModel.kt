package com.example.todoapp.ui.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.TodoApplication
import com.example.todoapp.data.entity.TodoItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/** 待办列表的月度分组键 */
data class YearMonth(val year: Int, val month: Int) : Comparable<YearMonth> {
    override fun compareTo(other: YearMonth): Int {
        val y = year.compareTo(other.year)
        return if (y != 0) y else month.compareTo(other.month)
    }

    fun display(): String = "${year}年${month}月"
}

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as TodoApplication).database.todoItemDao()

    val todos: StateFlow<List<TodoItemEntity>> = dao.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 已完成区块的筛选月份（默认当前月）；未完成待办不做月份筛选，始终全部展示
    private val _selectedMonth = MutableStateFlow(currentYearMonth())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    fun selectMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun addTodo(content: String, ddl: Long?) {
        if (content.isBlank()) return
        viewModelScope.launch {
            dao.insert(TodoItemEntity(content = content.trim(), ddl = ddl))
        }
    }

    fun toggleCompleted(todo: TodoItemEntity) {
        viewModelScope.launch {
            if (todo.isCompleted) {
                dao.update(todo.copy(isCompleted = false, completedAt = null))
            } else {
                dao.update(todo.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
            }
        }
    }

    fun updateTodo(todo: TodoItemEntity, content: String, ddl: Long?) {
        viewModelScope.launch {
            dao.update(todo.copy(content = content.trim(), ddl = ddl))
        }
    }

    fun deleteTodo(todo: TodoItemEntity) {
        viewModelScope.launch {
            dao.delete(todo)
        }
    }

    private fun currentYearMonth(): YearMonth {
        val c = Calendar.getInstance()
        return YearMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    }
}
