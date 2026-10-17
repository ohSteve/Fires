package com.example.todoapp.ui.checkin

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoapp.data.entity.CheckInItemEntity
import com.example.todoapp.ui.checkin.components.FrequencySelector

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyCheckInScreen(
    viewModel: CheckInViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val items by viewModel.items.collectAsState()
    var editingItem by remember { mutableStateOf<CheckInItemEntity?>(null) }
    var deletingItem by remember { mutableStateOf<CheckInItemEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的打卡项目") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无打卡项目", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ItemManageCard(
                        item = item,
                        viewModel = viewModel,
                        onClick = { editingItem = item },
                        onLongClick = { deletingItem = item }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "注意：打卡需在当日内完成。\n超时未决定的打卡项目将自动视为放弃，不可更改。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (editingItem != null) {
        EditItemSheet(
            item = editingItem!!,
            viewModel = viewModel,
            onDismiss = { editingItem = null }
        )
    }

    if (deletingItem != null) {
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text("删除打卡项目") },
            text = { Text("确定要删除「${deletingItem!!.name}」吗？\n\n注意：该项目的已完成和未完成打卡记录将被保留。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(deletingItem!!)
                    deletingItem = null
                }) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemManageCard(
    item: CheckInItemEntity,
    viewModel: CheckInViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var stats by remember { mutableStateOf(Pair(0, 0)) }

    androidx.compose.runtime.LaunchedEffect(item.id) {
        stats = viewModel.getStats(item)
    }

    val completionRate = if (stats.second > 0)
        (stats.first.toFloat() / stats.second * 100).toInt()
    else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).height(IntrinsicSize.Min)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium)
                        if (item.isPaused) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("已暂停", style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD32F2F))
                        }
                    }
                    if (item.requirements.isNotBlank()) {
                        Text(item.requirements, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "${item.startDate} 起 · ${cycleLabel(item.cycleType)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "完成率: $completionRate%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    " (${stats.first}/${stats.second})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { viewModel.togglePause(item) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (item.isPaused) "启动打卡" else "暂停打卡")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditItemSheet(
    item: CheckInItemEntity,
    viewModel: CheckInViewModel,
    onDismiss: () -> Unit
) {
    var editName by remember { mutableStateOf(item.name) }
    var editRequirements by remember { mutableStateOf(item.requirements) }
    var cycleType by remember { mutableStateOf(item.cycleType) }
    var cycleConfig by remember { mutableStateOf(item.cycleConfig) }
    var selectedDays by remember {
        mutableStateOf(
            item.cycleConfig.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("编辑打卡项目", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = editName, onValueChange = { editName = it },
                label = { Text("项目名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = editRequirements, onValueChange = { editRequirements = it },
                label = { Text("项目要求") }, minLines = 2, maxLines = 3, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))

            // Start date (read-only)
            OutlinedTextField(
                value = item.startDate,
                onValueChange = {},
                label = { Text("起始日期") },
                readOnly = true,
                enabled = false,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Frequency selector
            FrequencySelector(
                cycleType = cycleType,
                selectedDays = selectedDays,
                onCycleTypeChange = { cycleType = it },
                onCycleConfigChange = { cycleConfig = it },
                onDaysChange = { selectedDays = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Stats
            var stats by remember { mutableStateOf(Pair(0, 0)) }
            androidx.compose.runtime.LaunchedEffect(item.id) {
                stats = viewModel.getStats(item)
            }
            val rate = if (stats.second > 0)
                (stats.first.toFloat() / stats.second * 100).toInt() else 0
            Text("完成率: $rate% (${stats.first}/${stats.second})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
            Text(if (item.isPaused) "状态: 已暂停" else "状态: 进行中",
                style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.togglePause(item); onDismiss() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (item.isPaused) "启动打卡" else "暂停打卡")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        val config = if (cycleType == "weekly") selectedDays.sorted().joinToString(",") else cycleConfig
                        viewModel.updateFrequency(item, cycleType, config)
                        if (editName != item.name || editRequirements != item.requirements) {
                            viewModel.updateItemDetails(item, editName, editRequirements)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
            }
        }
    }
}

private fun cycleLabel(type: String): String = when (type) {
    "daily" -> "每天"
    "weekly" -> "周定期"
    "every_two_days" -> "每两天"
    else -> "自定义"
}
