package com.example.todoapp.ui.todo

import android.app.DatePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoapp.data.entity.TodoItemEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class YearMonth(val year: Int, val month: Int) : Comparable<YearMonth> {
    override fun compareTo(other: YearMonth): Int {
        val y = year.compareTo(other.year)
        return if (y != 0) y else month.compareTo(other.month)
    }

    fun display(): String = "${year}年${month}月"
}

@Composable
fun TodoListScreen(viewModel: TodoViewModel) {
    val todos by viewModel.todos.collectAsState()

    if (todos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("暂无待办", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text("点击右下角 + 添加待办", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val cal = Calendar.getInstance()
    val currentMonth = YearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

    // Collect all months from todos
    val allMonths = remember(todos) {
        val months = mutableSetOf(currentMonth)
        todos.forEach { todo ->
            val ts = if (todo.isCompleted) todo.completedAt else todo.ddl
            if (ts != null) {
                val c = Calendar.getInstance().apply { timeInMillis = ts }
                months.add(YearMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1))
            }
        }
        months.toList().sorted()
    }

    var selectedMonth by remember { mutableStateOf(currentMonth) }

    // Ensure selected month is in range if months changed
    if (selectedMonth !in allMonths && allMonths.isNotEmpty()) {
        // Find closest month
        selectedMonth = allMonths.minByOrNull { kotlin.math.abs(compareMonths(it, currentMonth)) } ?: currentMonth
    }

    // Filter todos for selected month
    val filteredTodos = remember(todos, selectedMonth) {
        todos.filter { todo ->
            val ts = if (todo.isCompleted) todo.completedAt else todo.ddl
            if (ts == null) {
                // Uncompleted without DDL: show in current month
                selectedMonth == currentMonth
            } else {
                val c = Calendar.getInstance().apply { timeInMillis = ts }
                YearMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1) == selectedMonth
            }
        }
    }

    val uncompleted = filteredTodos.filter { !it.isCompleted }
    val completed = filteredTodos.filter { it.isCompleted }
    var showAllCompleted by remember { mutableStateOf(false) }
    var showMonthMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Month subtitle with dropdown
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showMonthMenu = true }
            ) {
                Text(
                    text = selectedMonth.display(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "选择月份",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(
                expanded = showMonthMenu,
                onDismissRequest = { showMonthMenu = false }
            ) {
                allMonths.forEach { month ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                month.display(),
                                color = if (month == selectedMonth) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            selectedMonth = month
                            showMonthMenu = false
                        }
                    )
                }
            }
        }

        if (filteredTodos.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${selectedMonth.display()} 无待办事项",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Uncompleted items
                items(uncompleted, key = { it.id }) { todo ->
                    TodoCard(todo = todo, onToggle = { viewModel.toggleCompleted(todo) },
                        onEdit = { updated -> viewModel.updateTodo(todo, updated.content, updated.ddl) },
                        onDelete = { viewModel.deleteTodo(it) })
                }

                // Completed items
                if (completed.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            Text(
                                "已完成 (${completed.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (completed.size > 5) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showAllCompleted = !showAllCompleted }
                                ) {
                                    Text(
                                        if (showAllCompleted) "收起" else "展开全部",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        if (showAllCompleted) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    val visibleCompleted = if (completed.size > 5 && !showAllCompleted) {
                        listOf(completed.first())
                    } else {
                        completed
                    }

                    items(visibleCompleted, key = { it.id }) { todo ->
                        TodoCard(
                            todo = todo,
                            onToggle = { viewModel.toggleCompleted(todo) },
                            onEdit = { updated -> viewModel.updateTodo(todo, updated.content, updated.ddl) },
                            onDelete = { viewModel.deleteTodo(it) }
                        )
                    }

                    if (completed.size > 5 && !showAllCompleted) {
                        item {
                            Text(
                                "... 还有 ${completed.size - 1} 项已完成待办",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 56.dp, bottom = 8.dp)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

private fun compareMonths(a: YearMonth, b: YearMonth): Int {
    val ya = a.year.compareTo(b.year)
    return if (ya != 0) ya else a.month.compareTo(b.month)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TodoCard(
    todo: TodoItemEntity,
    onToggle: () -> Unit,
    onEdit: ((TodoItemEntity) -> Unit)? = null,
    onDelete: ((TodoItemEntity) -> Unit)? = null
) {
    val completed = todo.isCompleted
    var showEditDialog by remember { mutableStateOf(false) }
    var editContent by remember(todo) { mutableStateOf(todo.content) }
    var editDdl by remember(todo) { mutableStateOf(todo.ddl) }

    val todayStr = formatDate(System.currentTimeMillis())
    val ddlDateStr = todo.ddl?.let { formatDate(it) }
    val ddlColor = when {
        todo.ddl == null -> MaterialTheme.colorScheme.onSurfaceVariant
        ddlDateStr != null && ddlDateStr < todayStr -> Color(0xFFD32F2F)
        ddlDateStr == todayStr -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .combinedClickable(
                onClick = { /* handled by checkbox */ },
                onLongClick = { showEditDialog = true }
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) Color.Transparent
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square checkbox
            val checkColor = if (completed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                             else MaterialTheme.colorScheme.outline
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onToggle() }
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    val r = 4.dp.toPx()
                    drawRoundRect(
                        color = checkColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                        style = if (completed) androidx.compose.ui.graphics.drawscope.Fill
                                else androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                    )
                    if (completed) {
                        val cx = size.width / 2
                        val cy = size.height / 2
                        drawLine(
                            color = Color.White,
                            start = androidx.compose.ui.geometry.Offset(cx - 4.dp.toPx(), cy),
                            end = androidx.compose.ui.geometry.Offset(cx - 1.dp.toPx(), cy + 4.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = Color.White,
                            start = androidx.compose.ui.geometry.Offset(cx - 1.dp.toPx(), cy + 4.dp.toPx()),
                            end = androidx.compose.ui.geometry.Offset(cx + 4.dp.toPx(), cy - 4.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.content,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Fixed-height secondary row for consistent card size
                Box(modifier = Modifier.height(18.dp), contentAlignment = Alignment.CenterStart) {
                    Row {
                        if (todo.ddl != null) {
                            Text(
                                text = "DDL: ${formatDate(todo.ddl)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        else ddlColor
                            )
                        }
                        if (completed && todo.completedAt != null) {
                            if (todo.ddl != null) {
                                Text(
                                    "  |  ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                            Text(
                                text = "完成时间: ${formatDate(todo.completedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var showDdlPicker by remember { mutableStateOf(false) }
        ModalBottomSheet(
            onDismissRequest = { showEditDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)
            ) {
                Text("编辑待办", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    label = { Text("待办内容") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // DDL selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .clickable { showDdlPicker = true }
                        .padding(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "DDL: ${editDdl?.let { formatDate(it) } ?: "未设置"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        onDelete?.invoke(todo)
                        showEditDialog = false
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = {
                        onEdit?.invoke(todo.copy(content = editContent, ddl = editDdl))
                        showEditDialog = false
                    }) { Text("保存") }
                }
            }
        }

        if (showDdlPicker) {
            val cal = Calendar.getInstance()
            editDdl?.let { cal.timeInMillis = it }
            DatePickerDialog(
                LocalContext.current,
                { _, year, month, day ->
                    cal.set(year, month, day)
                    editDdl = cal.timeInMillis
                    showDdlPicker = false
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).apply { setOnDismissListener { showDdlPicker = false } }.show()
        }
    }
}

private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

private fun formatDate(timestamp: Long?): String {
    return if (timestamp != null) dateFormat.format(Date(timestamp)) else ""
}
