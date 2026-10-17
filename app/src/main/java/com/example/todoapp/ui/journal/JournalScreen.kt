package com.example.todoapp.ui.journal

import android.content.Intent
import android.provider.Settings
import android.app.DatePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.todoapp.ui.components.ReadOnlyTextBlock

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun JournalScreen(viewModel: JournalViewModel) {
    val currentDate by viewModel.currentDate.collectAsState()
    val goals by viewModel.goalsAchieved.collectAsState()
    val milestones by viewModel.milestones.collectAsState()
    val checkInRecords by viewModel.checkInRecords.collectAsState()
    val checkInItems by viewModel.checkInItems.collectAsState()
    val todos by viewModel.todosCompleted.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val entertainment by viewModel.entertainment.collectAsState()
    val instantGratification by viewModel.instantGratification.collectAsState()
    val usageMinutes by viewModel.usageMinutes.collectAsState()
    val topApps by viewModel.topApps.collectAsState()
    val usageUpdateTime by viewModel.usageUpdateTime.collectAsState()
    val canEdit by viewModel.canEdit.collectAsState()
    val pastEditUnlocked by viewModel.pastEditUnlocked.collectAsState()
    val canUpdateUsage by viewModel.canUpdateUsage.collectAsState()

    val context = LocalContext.current
    val today = JournalViewModel.todayString()
    val isToday = currentDate == today
    // 当天正常可编辑；今日之前的日期需双击确认后临时解锁；未来日期始终锁定
    val manualEditable = canEdit || pastEditUnlocked
    val canUnlockPast = !manualEditable && currentDate < today
    var isRefreshing by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showUnlockDialog by remember(currentDate) { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.refreshSummary()
            isRefreshing = false
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = { viewModel.selectDate(shiftDate(currentDate, -1)) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "前一天")
            }
            Text(
                text = if (isToday) "今日日志" else JournalViewModel.formatDisplayDate(currentDate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        // 非当天时：单击打开日历选择日期，双击直接回到今日
                        if (!isToday) {
                            Modifier.combinedClickable(
                                onClick = { showDatePicker = true },
                                onDoubleClick = { viewModel.goToToday() }
                            )
                        } else Modifier.clickable { showDatePicker = true }
                    ),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { viewModel.selectDate(shiftDate(currentDate, 1)) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "后一天")
            }
        }

        // Content area - pull-refresh only for today
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (isToday) Modifier.pullRefresh(pullRefreshState) else Modifier)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                val hasSummary = goals.isNotEmpty() || milestones.isNotEmpty() ||
                                 checkInRecords.isNotEmpty() || todos.isNotEmpty()

                if (!hasSummary && notes.isBlank() && entertainment.isBlank()) {
                    Spacer(modifier = Modifier.height(80.dp))
                    Text("暂无记录", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("当天的目标实现、打卡和待办完成将自动汇总于此",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }

                if (goals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader("实现的目标")
                    goals.forEach { SummaryCard(it.name) }
                }
                if (milestones.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionHeader("完成的里程碑")
                    milestones.forEach { (ms, goalName) ->
                        SummaryCard("[$goalName] ${ms.completedTaskName.ifBlank { "未命名" }}")
                    }
                }
                if (checkInRecords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionHeader("打卡记录")
                    checkInRecords.forEach { record ->
                        val itemName = checkInItems.find { it.id == record.itemId }?.name ?: return@forEach
                        val name = if (record.isBackfill) "「补」$itemName" else itemName
                        val label = if (record.status == "completed") "✓" else "✗"
                        val color = if (record.status == "completed") Color(0xFF388E3C) else Color(0xFFD32F2F)
                        SummaryCard("$name  $label", color)
                    }
                }
                if (todos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionHeader("完成的待办")
                    todos.forEach { SummaryCard(it.content) }
                }

                if (hasSummary) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isToday) "* 以上内容根据目标、打卡和待办自动汇总，下拉刷新"
                        else "* 以上内容根据目标、打卡和待办自动汇总",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("今天还干了啥", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                if (manualEditable) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.updateNotes(it) },
                        placeholder = { Text("今天还做了哪些事、有什么收获...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        ),
                        minLines = 2, maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ReadOnlyTextBlock(
                        text = notes,
                        onDoubleTap = if (canUnlockPast) ({ showUnlockDialog = true }) else null,
                        minHeight = 80.dp, maxHeight = 152.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("娱乐项目", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                if (manualEditable) {
                    OutlinedTextField(
                        value = entertainment,
                        onValueChange = { viewModel.updateEntertainment(it) },
                        placeholder = { Text("今天玩了什么游戏、看了什么视频...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        ),
                        minLines = 2, maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ReadOnlyTextBlock(
                        text = entertainment,
                        onDoubleTap = if (canUnlockPast) ({ showUnlockDialog = true }) else null,
                        minHeight = 80.dp, maxHeight = 152.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = instantGratification,
                        onCheckedChange = { if (manualEditable) viewModel.updateInstantGratification(it) },
                        enabled = manualEditable
                    )
                    Text("是否进行了「及时满足型娱乐」",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (manualEditable) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("手机使用时长", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (!viewModel.hasUsageStatsPermission())
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            else viewModel.loadUsageStats()
                        },
                        enabled = canUpdateUsage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text("更新") }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val capturedMinutes = usageMinutes
                when {
                    capturedMinutes == -1L -> Text("需开启「使用情况访问权限」",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    capturedMinutes != null -> {
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("屏幕使用时间", style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f))
                                    Text(formatUsageTime(capturedMinutes),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                if (topApps.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("使用时长 Top 5", style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    topApps.forEachIndexed { i, (name, ms) ->
                                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                            Text("${i + 1}. $name", style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f))
                                            Text(formatUsageTime(ms / (60 * 1000)),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                if (usageUpdateTime != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("更新时间: $usageUpdateTime",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                    else -> {
                        if (canUpdateUsage) {
                            Text("点击「更新」查看今日手机使用时长",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (isToday && canEdit) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("请在当日内完成编辑，过后无法修改。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                } else if (pastEditUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("已临时解锁，切换日期后将重新锁定。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            if (isToday) {
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }

    if (showUnlockDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = { Text("解锁编辑") },
            text = { Text("是否解锁编辑？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unlockPastEditing()
                    showUnlockDialog = false
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDatePicker) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        try { sdf.parse(currentDate)?.let { cal.time = it } } catch (_: Exception) {}
        DatePickerDialog(
            context,
            { _, year, month, day ->
                cal.set(year, month, day)
                viewModel.selectDate(sdf.format(cal.time))
                showDatePicker = false
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showDatePicker = false }
        }.show()
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
}

@Composable
private fun SummaryCard(text: String, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
    ) {
        Text("• $text", color = textColor, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

private fun shiftDate(dateStr: String, days: Int): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return dateStr
        val cal = java.util.Calendar.getInstance().apply { time = date }
        cal.add(java.util.Calendar.DAY_OF_MONTH, days)
        sdf.format(cal.time)
    } catch (e: Exception) { dateStr }
}

private fun formatUsageTime(minutes: Long): String =
    if (minutes < 60) "${minutes}分钟"
    else { val h = minutes / 60; val m = minutes % 60; if (m == 0L) "${h}小时" else "${h}小时${m}分钟" }
