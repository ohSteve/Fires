package com.example.todoapp.ui.goal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.TodoApplication
import com.example.todoapp.data.entity.GoalEntity
import com.example.todoapp.data.entity.MilestoneEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GoalDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as TodoApplication).database
    private val goalDao = db.goalDao()
    private val milestoneDao = db.milestoneDao()

    private val goalIdFlow = MutableStateFlow<Long?>(null)

    val goal: StateFlow<GoalEntity?> = goalIdFlow.flatMapLatest { id ->
        if (id != null) goalDao.getGoalById(id)
        else kotlinx.coroutines.flow.flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val milestones: StateFlow<List<MilestoneEntity>> = goalIdFlow.flatMapLatest { id ->
        if (id != null) milestoneDao.getMilestonesByGoalId(id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initialize(goalId: Long) {
        goalIdFlow.value = goalId
    }

    fun updateName(name: String) {
        val g = goal.value ?: return
        viewModelScope.launch { goalDao.update(g.copy(name = name)) }
    }

    fun updateDdl(ddl: Long?) {
        val g = goal.value ?: return
        viewModelScope.launch { goalDao.update(g.copy(ddl = ddl)) }
    }

    fun updateImplementationPath(path: String) {
        val g = goal.value ?: return
        viewModelScope.launch { goalDao.update(g.copy(implementationPath = path)) }
    }

    fun achieveGoal() {
        val g = goal.value ?: return
        viewModelScope.launch {
            goalDao.update(g.copy(isAchieved = true, achievedAt = System.currentTimeMillis()))
        }
    }

    fun undoAchieve() {
        val g = goal.value ?: return
        viewModelScope.launch {
            goalDao.update(g.copy(isAchieved = false, achievedAt = null))
        }
    }

    fun addMilestone(date: Long, name: String, detail: String, timeSpent: String, difficulties: String, futurePlans: String) {
        val g = goal.value ?: return
        viewModelScope.launch {
            milestoneDao.insert(
                MilestoneEntity(
                    goalId = g.id,
                    date = date,
                    completedTaskName = name.trim(),
                    completedTaskDetail = detail.trim(),
                    timeSpent = timeSpent.trim(),
                    difficulties = difficulties.trim(),
                    futurePlans = futurePlans.trim()
                )
            )
        }
    }

    fun updateMilestone(milestone: MilestoneEntity) {
        viewModelScope.launch { milestoneDao.update(milestone) }
    }

    fun deleteMilestone(milestone: MilestoneEntity) {
        viewModelScope.launch { milestoneDao.delete(milestone) }
    }
}
