package com.example.todoapp.ui.checkin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.todoapp.data.entity.CheckInItemEntity
import com.example.todoapp.data.entity.CheckInRecordEntity
import com.example.todoapp.ui.checkin.components.AddCheckInDialog
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel,
    onOpenManage: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val records by viewModel.records.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBackfillDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val today = CheckInViewModel.todayString()
    val isToday = currentDate == today
    val isEditable = isToday  // only today is editable

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.goToToday {
                isRefreshing = false
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Date bar (outside pull-to-refresh area)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = { viewModel.selectDate(shiftDate(currentDate, -1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "前一天")
                }
                Text(
                    text = if (isToday) "今日打卡目标" else CheckInViewModel.formatDisplayDate(currentDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { viewModel.selectDate(shiftDate(currentDate, 1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "后一天")
                }
            }

            // "我的打卡项目" entry
            Card(
                onClick = onOpenManage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                    Text("我的打卡项目", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            // Pull-to-refresh area wrapping scrollable content directly
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pullRefresh(pullRefreshState)
            ) {
                if (records.isEmpty()) {
                    // Empty state wrapped in scrollable Column for pullRefresh to work
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(120.dp))
                        Text("暂无打卡项目", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("点击右下角 + 添加打卡项目", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val pending = records.filter { it.status == "pending" }
                    val done = records.filter { it.status != "pending" }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(pending, key = { it.id }) { record ->
                            val item = items.find { it.id == record.itemId }
                            if (item != null) {
                                PendingCard(item = item, record = record, viewModel = viewModel, isEditable = isEditable)
                            }
                        }
                        items(done, key = { it.id }) { record ->
                            val item = items.find { it.id == record.itemId }
                            if (item != null) {
                                DoneCard(item = item, record = record, viewModel = viewModel, isEditable = isEditable)
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }

                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        // FABs only on today
        if (isToday) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加打卡")
            }

            // Backfill button (bottom-left)
            FloatingActionButton(
                onClick = { showBackfillDialog = true },
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text("补", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showAddDialog) {
        AddCheckInDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, requirements, cycleType, cycleConfig ->
                viewModel.addItem(name, requirements, cycleType, cycleConfig, startDate = currentDate)
                showAddDialog = false
            }
        )
    }

    if (showBackfillDialog) {
        BackfillDialog(
            items = items,
            onDismiss = { showBackfillDialog = false },
            onSelect = { item ->
                viewModel.addBackfillItem(item)
                showBackfillDialog = false
            }
        )
    }
}

@Composable
private fun BackfillDialog(
    items: List<CheckInItemEntity>,
    onDismiss: () -> Unit,
    onSelect: (CheckInItemEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择补充打卡的项目") },
        text = {
            if (items.isEmpty()) {
                Text("暂无打卡项目，请先添加")
            } else {
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(items, key = { it.id }) { item ->
                        Card(
                            onClick = { onSelect(item) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp).height(44.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("「补」${item.name}", style = MaterialTheme.typography.titleSmall)
                                    if (item.requirements.isNotBlank()) {
                                        Text(item.requirements, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PendingCard(
    item: CheckInItemEntity,
    record: CheckInRecordEntity,
    viewModel: CheckInViewModel,
    isEditable: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (record.isBackfill) "「补」${item.name}" else item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
                Box(Modifier.height(20.dp).padding(start = 8.dp), contentAlignment = Alignment.CenterStart) {
                    if (item.requirements.isNotBlank()) {
                        Text(item.requirements, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            CircleXButton(
                color = if (!isEditable) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error,
                onClick = if (!isEditable) ({}) else ({ viewModel.markAbandoned(record) })
            )
            Spacer(modifier = Modifier.width(12.dp))
            CircleCheckButton(
                color = if (!isEditable) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary,
                onClick = if (!isEditable) ({}) else ({ viewModel.markCompleted(record) })
            )
        }
    }
}

@Composable
private fun DoneCard(
    item: CheckInItemEntity,
    record: CheckInRecordEntity,
    viewModel: CheckInViewModel,
    isEditable: Boolean
) {
    val isCompleted = record.status == "completed"
    val bgColor = if (isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)  // 放弃=未决定同色灰底
    val symbolColor = if (isCompleted) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.error
    var showUndoDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (record.isBackfill) "「补」${item.name}" else item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
                Box(Modifier.height(20.dp).padding(start = 8.dp), contentAlignment = Alignment.CenterStart) {
                    if (item.requirements.isNotBlank()) {
                        Text(item.requirements, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier.size(36.dp)
                    .then(if (isEditable) Modifier.clickable { showUndoDialog = true } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) CircleCheckIcon(color = symbolColor)
                else CircleXIcon(color = symbolColor)
            }
        }
    }

    if (showUndoDialog) {
        AlertDialog(
            onDismissRequest = { showUndoDialog = false },
            title = { Text("撤销打卡状态") },
            text = { Text("确定要撤销「${item.name}」的打卡状态吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.toggleRecord(record); showUndoDialog = false }) {
                    Text("确认撤销")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUndoDialog = false }) { Text("取消") }
            }
        )
    }
}

// --- Circle icons ---

@Composable
private fun CircleCheckButton(color: Color, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        CircleCheckIcon(color)
    }
}

@Composable
private fun CircleXButton(color: Color, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        CircleXIcon(color)
    }
}

@Composable
private fun CircleCheckIcon(color: Color) {
    Canvas(Modifier.size(28.dp)) {
        val sw = 2.dp.toPx()
        val r = size.minDimension / 2 - sw / 2
        drawCircle(color, r, style = Stroke(sw))
        val cx = size.width / 2; val cy = size.height / 2; val o = 5.dp.toPx()
        drawLine(color, Offset(cx - o, cy), Offset(cx - o/3, cy + o), 2.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(cx - o/3, cy + o), Offset(cx + o, cy - o), 2.5.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun CircleXIcon(color: Color) {
    Canvas(Modifier.size(28.dp)) {
        val sw = 2.dp.toPx()
        val r = size.minDimension / 2 - sw / 2
        drawCircle(color, r, style = Stroke(sw))
        val cx = size.width / 2; val cy = size.height / 2; val o = 5.dp.toPx()
        drawLine(color, Offset(cx - o, cy - o), Offset(cx + o, cy + o), 2.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(cx + o, cy - o), Offset(cx - o, cy + o), 2.5.dp.toPx(), cap = StrokeCap.Round)
    }
}

private fun shiftDate(dateStr: String, days: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return dateStr
        val cal = java.util.Calendar.getInstance().apply { time = date }
        cal.add(java.util.Calendar.DAY_OF_MONTH, days)
        sdf.format(cal.time)
    } catch (e: Exception) { dateStr }
}
