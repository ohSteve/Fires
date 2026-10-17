package com.example.todoapp.ui.checkin.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencySelector(
    cycleType: String,
    selectedDays: Set<Int>,
    onCycleTypeChange: (String) -> Unit,
    onCycleConfigChange: (String) -> Unit,
    onDaysChange: (Set<Int>) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    val cycleOptions = listOf(
        "daily" to "每天",
        "weekly" to "周定期"
    )
    val dayLabels = listOf(1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 7 to "日")

    ExposedDropdownMenuBox(
        expanded = dropdownExpanded,
        onExpandedChange = { dropdownExpanded = it }
    ) {
        OutlinedTextField(
            value = cycleOptions.first { it.first == cycleType }.second,
            onValueChange = {},
            readOnly = true,
            label = { Text("打卡周期") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false }
        ) {
            cycleOptions.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onCycleTypeChange(value)
                        dropdownExpanded = false
                        if (value != "weekly") onDaysChange(emptySet())
                    }
                )
            }
        }
    }

    if (cycleType == "weekly") {
        Spacer(modifier = Modifier.height(8.dp))
        Text("选择星期：", style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { (num, label) ->
                Column(
                    modifier = Modifier.weight(1f).padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                    Checkbox(
                        checked = num in selectedDays,
                        onCheckedChange = { checked ->
                            val newDays = if (checked) selectedDays + num else selectedDays - num
                            onDaysChange(newDays)
                            onCycleConfigChange(newDays.sorted().joinToString(","))
                        }
                    )
                }
            }
        }
    }

}
