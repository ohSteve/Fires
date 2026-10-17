package com.example.todoapp.ui.goal

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.todoapp.data.entity.MilestoneEntity
import com.example.todoapp.ui.components.ReadOnlyTextBlock
import com.example.todoapp.ui.goal.components.MilestoneSheet
import com.example.todoapp.ui.goal.components.TimelineView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GoalDetailScreen(
    goalId: Long,
    viewModel: GoalDetailViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(goalId) {
        viewModel.initialize(goalId)
    }

    val goal by viewModel.goal.collectAsState()
    val milestones by viewModel.milestones.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showMilestoneSheet by remember { mutableStateOf(false) }
    var editingMilestone by remember { mutableStateOf<MilestoneEntity?>(null) }
    var showUndoDialog by remember { mutableStateOf(false) }
    var showClearDdlDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val achieved = goal?.isAchieved ?: false
    var nameText by rememberSaveable(goal) { mutableStateOf(goal?.name ?: "") }
    var pathText by rememberSaveable(goal) { mutableStateOf(goal?.implementationPath ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("目标详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val g = goal
        if (g == null) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Goal name
            if (achieved) {
                ReadOnlyTextBlock(
                    text = nameText,
                    label = "目标名称",
                    minHeight = 56.dp,
                    maxHeight = 120.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        viewModel.updateName(it)
                    },
                    label = { Text("目标名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // DDL：单击修改；已设置时双击弹窗取消
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!achieved) {
                            Modifier.combinedClickable(
                                onClick = { showDatePicker = true },
                                onDoubleClick = if (g.ddl != null) ({ showClearDdlDialog = true }) else null
                            )
                        } else Modifier
                    )
                    .padding(vertical = 12.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = if (achieved) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                           else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "DDL: ${g.ddl?.let { formatDate(it) } ?: "未设置"}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (achieved) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Goal description
            if (achieved) {
                ReadOnlyTextBlock(
                    text = pathText,
                    label = "目标描述",
                    minHeight = 104.dp,
                    maxHeight = 176.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = pathText,
                    onValueChange = {
                        pathText = it
                        viewModel.updateImplementationPath(it)
                    },
                    label = { Text("目标描述") },
                    placeholder = { Text("描述这个目标的具体内容...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Timeline section
            Text(
                "里程碑",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            TimelineView(
                milestones = milestones,
                onMilestoneClick = { milestone ->
                    editingMilestone = milestone
                    showMilestoneSheet = true
                },
                isAchieved = achieved
            )

            if (!achieved) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        editingMilestone = null
                        showMilestoneSheet = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ 添加里程碑")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Achieve / Undo button
            if (achieved) {
                // Double-click detection for undo
                var lastClickTime by remember { mutableStateOf(0L) }
                OutlinedButton(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime < 500) {
                            showUndoDialog = true
                        }
                        lastClickTime = now
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("该目标已实现")
                }
            } else {
                Button(
                    onClick = { viewModel.achieveGoal() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("实现目标")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Undo confirmation dialog
    if (showUndoDialog) {
        AlertDialog(
            onDismissRequest = { showUndoDialog = false },
            title = { Text("确认撤销") },
            text = { Text("确定要撤销该目标的实现状态吗？撤销后所有内容可以重新编辑。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.undoAchieve()
                    showUndoDialog = false
                }) {
                    Text("确认撤销")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUndoDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 取消 DDL 确认弹窗（双击 DDL 栏触发）
    if (showClearDdlDialog) {
        AlertDialog(
            onDismissRequest = { showClearDdlDialog = false },
            title = { Text("取消 DDL") },
            text = { Text("是否取消该目标的 DDL？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDdl(null)
                    showClearDdlDialog = false
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDdlDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Date picker dialog for DDL (future dates only)
    if (showDatePicker) {
        val cal = Calendar.getInstance()
        goal?.ddl?.let { cal.timeInMillis = it }
        DatePickerDialog(
            LocalContext.current,
            { _, year, month, day ->
                cal.set(year, month, day)
                viewModel.updateDdl(cal.timeInMillis)
                showDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            setOnDismissListener { showDatePicker = false }
        }.show()
    }

    // Milestone sheet
    if (showMilestoneSheet) {
        MilestoneSheet(
            existing = editingMilestone,
            readOnly = achieved,
            onDismiss = {
                showMilestoneSheet = false
                editingMilestone = null
            },
            onSave = { date, name, detail, timeSpent, difficulties, futurePlans ->
                val existing = editingMilestone
                if (existing != null) {
                    viewModel.updateMilestone(
                        existing.copy(
                            date = date,
                            completedTaskName = name,
                            completedTaskDetail = detail,
                            timeSpent = timeSpent,
                            difficulties = difficulties,
                            futurePlans = futurePlans
                        )
                    )
                } else {
                    viewModel.addMilestone(date, name, detail, timeSpent, difficulties, futurePlans)
                }
                showMilestoneSheet = false
                editingMilestone = null
            },
            onDelete = if (editingMilestone != null) {
                {
                    editingMilestone?.let { viewModel.deleteMilestone(it) }
                    showMilestoneSheet = false
                    editingMilestone = null
                }
            } else null
        )
    }
}

private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

private fun formatDate(timestamp: Long): String {
    return dateFormat.format(Date(timestamp))
}
