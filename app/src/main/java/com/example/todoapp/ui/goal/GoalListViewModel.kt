package com.example.todoapp.ui.goal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.TodoApplication
import com.example.todoapp.data.entity.GoalEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalListViewModel(application: Application) : AndroidViewModel(application) {

    private val goalDao = (application as TodoApplication).database.goalDao()

    val goals: StateFlow<List<GoalEntity>> = goalDao.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(name: String, ddl: Long?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            goalDao.insert(GoalEntity(name = name.trim(), ddl = ddl))
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalDao.delete(goal)
        }
    }
}
