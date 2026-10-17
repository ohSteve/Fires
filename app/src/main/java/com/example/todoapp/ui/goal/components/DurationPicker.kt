package com.example.todoapp.ui.goal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun DurationPickerDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Parse initial value like "2.5 小时" or "3 天"
    val initialNumber = remember(initialValue) {
        initialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull()?.toString() ?: ""
    }
    val initialUnit = remember(initialValue) {
        when {
            initialValue.contains("天") -> "天"
            initialValue.contains("周") -> "周"
            else -> "小时"
        }
    }

    var number by remember { mutableStateOf(initialNumber) }
    var unit by remember { mutableStateOf(initialUnit) }

    val units = listOf("小时", "天", "周")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("耗时") },
        text = {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    units.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = number,
                    onValueChange = { v ->
                        if (v.isEmpty() || v.matches(Regex("^\\d+(\\.\\d?)?$"))) {
                            number = v
                        }
                    },
                    label = { Text("时长") },
                    placeholder = { Text("例如 2.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val num = number.toDoubleOrNull()
                    if (num != null && num > 0) {
                        val formatted = if (num == num.toLong().toDouble()) num.toLong().toString()
                                        else num.toString()
                        onConfirm("$formatted $unit")
                    }
                },
                enabled = number.toDoubleOrNull() != null && (number.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm("") }) {
                Text("清除")
            }
        }
    )
}
