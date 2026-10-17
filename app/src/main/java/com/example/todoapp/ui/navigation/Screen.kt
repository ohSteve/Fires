package com.example.todoapp.ui.navigation

sealed class Screen {
    data object GoalList : Screen()
    data class GoalDetail(val goalId: Long) : Screen()
    data object MyCheckIn : Screen()
    data object Settings : Screen()
    data object Search : Screen()
}
