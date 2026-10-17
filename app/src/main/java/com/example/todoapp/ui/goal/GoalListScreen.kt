package com.example.todoapp.ui.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNotifications
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.EditNotifications
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoapp.data.entity.GoalEntity
import com.example.todoapp.ui.goal.components.AddGoalDialog
import com.example.todoapp.ui.checkin.CheckInScreen
import com.example.todoapp.ui.checkin.CheckInViewModel
import com.example.todoapp.ui.journal.JournalScreen
import com.example.todoapp.ui.journal.JournalViewModel
import com.example.todoapp.ui.todo.TodoListScreen
import com.example.todoapp.ui.todo.TodoViewModel
import com.example.todoapp.ui.todo.components.AddTodoDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalListScreen(
    goalViewModel: GoalListViewModel,
    checkInViewModel: CheckInViewModel,
    todoViewModel: TodoViewModel,
    journalViewModel: JournalViewModel,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onGoalClick: (Long) -> Unit,
    onOpenCheckInManage: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val goals by goalViewModel.goals.collectAsState()
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddTodoDialog by remember { mutableStateOf(false) }

    val unachieved = goals.filter { !it.isAchieved }
    val achieved = goals.filter { it.isAchieved }

    val tabTitles = listOf("目标", "打卡", "待办", "日志")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tabTitles[selectedTab]) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    icon = { Icon(if (selectedTab == 0) Icons.Filled.Flag else Icons.Outlined.Flag, contentDescription = null) },
                    label = { Text("目标") },
                    selected = selectedTab == 0,
                    onClick = { onTabChange(0) }
                )
                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    icon = { Icon(if (selectedTab == 1) Icons.Filled.EditNotifications else Icons.Outlined.EditNotifications, contentDescription = null) },
                    label = { Text("打卡") },
                    selected = selectedTab == 1,
                    onClick = { onTabChange(1) }
                )
                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    icon = { Icon(if (selectedTab == 2) Icons.Filled.Checklist else Icons.Outlined.Checklist, contentDescription = null) },
                    label = { Text("待办") },
                    selected = selectedTab == 2,
                    onClick = { onTabChange(2) }
                )
                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    icon = {
                        Icon(
                            if (selectedTab == 3) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null
                        )
                    },
                    label = { Text("日志") },
                    selected = selectedTab == 3,
                    onClick = { onTabChange(3) }
                )
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    FloatingActionButton(
                        onClick = { showAddGoalDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加目标")
                    }
                }
                2 -> {
                    FloatingActionButton(
                        onClick = { showAddTodoDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加待办")
                    }
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> GoalsContent(
                unachieved = unachieved,
                achieved = achieved,
                onGoalClick = onGoalClick,
                modifier = Modifier.padding(padding)
            )
            1 -> Box(modifier = Modifier.padding(padding)) {
                CheckInScreen(
                    viewModel = checkInViewModel,
                    onOpenManage = onOpenCheckInManage
                )
            }
            2 -> Box(modifier = Modifier.padding(padding)) {
                TodoListScreen(viewModel = todoViewModel)
            }
            3 -> Box(modifier = Modifier.padding(padding)) {
                JournalScreen(viewModel = journalViewModel)
            }
        }
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { name, ddl ->
                goalViewModel.addGoal(name, ddl)
                showAddGoalDialog = false
            }
        )
    }

    if (showAddTodoDialog) {
        AddTodoDialog(
            onDismiss = { showAddTodoDialog = false },
            onConfirm = { content, ddl ->
                todoViewModel.addTodo(content, ddl)
                showAddTodoDialog = false
            }
        )
    }
}

@Composable
private fun GoalsContent(
    unachieved: List<GoalEntity>,
    achieved: List<GoalEntity>,
    onGoalClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllAchieved by remember { mutableStateOf(false) }
    if (unachieved.isEmpty() && achieved.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "暂无目标",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "点击右下角 + 添加新目标",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (unachieved.isNotEmpty()) {
                item {
                    SectionHeader("进行中")
                }
                items(unachieved, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        achieved = false,
                        onClick = { onGoalClick(goal.id) }
                    )
                }
            }
            if (achieved.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        SectionHeader("已实现 (${achieved.size})")
                        Spacer(modifier = Modifier.weight(1f))
                        if (achieved.size > 5) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showAllAchieved = !showAllAchieved }
                            ) {
                                Text(if (showAllAchieved) "收起" else "展开全部",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                                Icon(
                                    if (showAllAchieved) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                val visibleAchieved = if (achieved.size > 5 && !showAllAchieved)
                    listOf(achieved.first()) else achieved
                items(visibleAchieved, key = { it.id }) { goal ->
                    GoalCard(goal = goal, achieved = true, onClick = { onGoalClick(goal.id) })
                }
                if (achieved.size > 5 && !showAllAchieved) {
                    item {
                        Text("... 还有 ${achieved.size - 1} 个已实现目标",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,  // 主题色高饱和
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    achieved: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achieved) Color.Transparent
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (achieved)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (achieved) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                // Secondary info: DDL line 1, description line 2 (fixed height for consistency)
                Column(modifier = Modifier.padding(top = 4.dp).heightIn(min = 36.dp)) {
                    // Line 1: DDL or achieved time
                    if (goal.ddl != null && !achieved) {
                        val ddlStr = formatDate(goal.ddl)
                        val todayStr = formatDate(System.currentTimeMillis())
                        val ddlColor = when {
                            ddlStr < todayStr -> Color(0xFFD32F2F)
                            ddlStr == todayStr -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text("DDL: $ddlStr", style = MaterialTheme.typography.bodySmall, color = ddlColor,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (achieved && goal.achievedAt != null) {
                        Text("完成于 ${formatDate(goal.achievedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // Line 2: description (for unachieved goals)
                    if (!achieved && goal.implementationPath.isNotBlank()) {
                        Text(goal.implementationPath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

private fun formatDate(timestamp: Long): String {
    return dateFormat.format(Date(timestamp))
}
