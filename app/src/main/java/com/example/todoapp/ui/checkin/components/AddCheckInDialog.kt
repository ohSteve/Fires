package com.example.todoapp.ui.checkin.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddCheckInDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, requirements: String, cycleType: String, cycleConfig: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var requirements by remember { mutableStateOf("") }
    var cycleType by remember { mutableStateOf("daily") }
    var cycleConfig by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加打卡项目") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("项目名称") },
                    placeholder = { Text("输入打卡项目名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = requirements,
                    onValueChange = { requirements = it },
                    label = { Text("项目要求") },
                    placeholder = { Text("具体要求（选填）") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                FrequencySelector(
                    cycleType = cycleType,
                    selectedDays = selectedDays,
                    onCycleTypeChange = { cycleType = it },
                    onCycleConfigChange = { cycleConfig = it },
                    onDaysChange = { selectedDays = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val config = if (cycleType == "weekly") selectedDays.sorted().joinToString(",") else cycleConfig
                    onConfirm(name, requirements, cycleType, config)
                },
                enabled = name.isNotBlank() && (cycleType != "weekly" || selectedDays.isNotEmpty())
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
