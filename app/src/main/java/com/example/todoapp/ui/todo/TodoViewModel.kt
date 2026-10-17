package com.example.todoapp.ui.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.TodoApplication
import com.example.todoapp.data.entity.TodoItemEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as TodoApplication).database.todoItemDao()

    val todos: StateFlow<List<TodoItemEntity>> = dao.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

}
