package com.example.todoapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.ui.checkin.CheckInViewModel
import com.example.todoapp.ui.checkin.MyCheckInScreen
import com.example.todoapp.ui.goal.GoalDetailViewModel
import com.example.todoapp.ui.goal.GoalDetailScreen
import com.example.todoapp.ui.goal.GoalListScreen
import com.example.todoapp.ui.goal.GoalListViewModel
import com.example.todoapp.ui.journal.JournalViewModel
import com.example.todoapp.ui.navigation.Screen
import com.example.todoapp.ui.settings.SettingsScreen
import com.example.todoapp.ui.theme.TodoTheme
import com.example.todoapp.ui.todo.TodoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentScreen by remember { mutableStateOf<Screen>(Screen.GoalList) }
            var savedTab by rememberSaveable { mutableIntStateOf(0) }

            // Key on currentScreen to force re-reading prefs after returning from Settings
            val prefs = remember { getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
            val themeMode = remember(currentScreen) { prefs.getString("theme_mode", "light") ?: "light" }
            val themeColor = remember(currentScreen) { prefs.getString("theme_color", "MUTED_BLUE") ?: "MUTED_BLUE" }
            TodoTheme(themeMode = themeMode, themeColor = themeColor) {
                val checkInViewModel: CheckInViewModel = viewModel()

                when (val screen = currentScreen) {
                    is Screen.GoalList -> {
                        val goalViewModel: GoalListViewModel = viewModel()
                        val todoViewModel: TodoViewModel = viewModel()
                        val journalViewModel: JournalViewModel = viewModel()
                        GoalListScreen(
                            goalViewModel = goalViewModel,
                            checkInViewModel = checkInViewModel,
                            todoViewModel = todoViewModel,
                            journalViewModel = journalViewModel,
                            selectedTab = savedTab,
                            onTabChange = { savedTab = it },
                            onGoalClick = { goalId -> currentScreen = Screen.GoalDetail(goalId) },
                            onOpenCheckInManage = { currentScreen = Screen.MyCheckIn },
                            onOpenSettings = { currentScreen = Screen.Settings }
                        )
                    }
                    is Screen.GoalDetail -> {
                        val detailViewModel: GoalDetailViewModel = viewModel()
                        GoalDetailScreen(
                            goalId = screen.goalId,
                            viewModel = detailViewModel,
                            onBack = { currentScreen = Screen.GoalList }
                        )
                    }
                    is Screen.MyCheckIn -> {
                        MyCheckInScreen(
                            viewModel = checkInViewModel,
                            onBack = { currentScreen = Screen.GoalList }
                        )
                    }
                    is Screen.Settings -> {
                        SettingsScreen(
                            onBack = { currentScreen = Screen.GoalList }
                        )
                    }
                }
            }
        }
    }
}
