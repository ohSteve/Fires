package com.example.todoapp.ui.goal.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.todoapp.data.entity.MilestoneEntity
import com.example.todoapp.ui.components.ReadOnlyTextBlock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneSheet(
    existing: MilestoneEntity?,
    readOnly: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (date: Long, name: String, detail: String, timeSpent: String, difficulties: String, futurePlans: String) -> Unit,
    onDelete: (() -> Unit)?
) {
    var date by remember { mutableStateOf(existing?.date ?: System.currentTimeMillis()) }
    var name by remember { mutableStateOf(existing?.completedTaskName ?: "") }
    var detail by remember { mutableStateOf(existing?.completedTaskDetail ?: "") }
    var timeSpent by remember { mutableStateOf(existing?.timeSpent ?: "") }
    var difficulties by remember { mutableStateOf(existing?.difficulties ?: "") }
    var futurePlans by remember { mutableStateOf(existing?.futurePlans ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (existing != null) "里程碑详情" else "新建里程碑",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Date selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!readOnly) Modifier.clickable { showDatePicker = true }
                        else Modifier
                    )
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = if (readOnly) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                           else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = formatDate(date),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (readOnly) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (readOnly) {
                ReadOnlyTextBlock(
                    text = name,
                    label = "已完成的任务名称",
                    minHeight = 56.dp,
                    maxHeight = 120.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("已完成的任务名称 *") },
                    placeholder = { Text("任务名称（必填）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (readOnly) {
                ReadOnlyTextBlock(
                    text = detail,
                    label = "已完成的任务具体内容",
                    minHeight = 80.dp,
                    maxHeight = 128.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("已完成的任务具体内容") },
                    placeholder = { Text("具体描述（选填）") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time spent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!readOnly) Modifier.clickable { showDurationPicker = true }
                        else Modifier
                    )
            ) {
                OutlinedTextField(
                    value = timeSpent,
                    onValueChange = {},
                    label = { Text("耗时") },
                    placeholder = { Text("未设置") },
                    readOnly = true,
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (!readOnly) {
                        OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else {
                        OutlinedTextFieldDefaults.colors()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (readOnly) {
                ReadOnlyTextBlock(
                    text = difficulties,
                    label = "难点",
                    minHeight = 80.dp,
                    maxHeight = 128.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = difficulties,
                    onValueChange = { difficulties = it },
                    label = { Text("难点") },
                    placeholder = { Text("遇到的困难（选填）") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (readOnly) {
                ReadOnlyTextBlock(
                    text = futurePlans,
                    label = "未来的计划",
                    minHeight = 80.dp,
                    maxHeight = 128.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = futurePlans,
                    onValueChange = { futurePlans = it },
                    label = { Text("未来的计划") },
                    placeholder = { Text("下一步计划（选填）") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!readOnly) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onSave(date, name, detail, timeSpent, difficulties, futurePlans) },
                        enabled = name.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val context = LocalContext.current
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                cal.set(year, month, day)
                date = cal.timeInMillis
                showDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            setOnDismissListener { showDatePicker = false }
        }.show()
    }

    if (showDurationPicker) {
        DurationPickerDialog(
            initialValue = timeSpent,
            onDismiss = { showDurationPicker = false },
            onConfirm = { result ->
                timeSpent = result
                showDurationPicker = false
            }
        )
    }
}

private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

private fun formatDate(timestamp: Long): String {
    return dateFormat.format(Date(timestamp))
}
