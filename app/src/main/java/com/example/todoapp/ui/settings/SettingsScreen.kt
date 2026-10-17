package com.example.todoapp.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.todoapp.ui.theme.ThemeColor
import com.example.todoapp.ui.theme.parseHexColor
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // Local editing state (applied only on "应用" click)
    var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "light") ?: "light") }
    var themeColor by remember { mutableStateOf(prefs.getString("theme_color", "MUTED_BLUE") ?: "MUTED_BLUE") }
    var reminderHour by remember { mutableStateOf(prefs.getInt("reminder_hour", -1)) }
    var reminderMinute by remember { mutableStateOf(prefs.getInt("reminder_minute", 0)) }
    var reminderText by remember {
        mutableStateOf(prefs.getString("reminder_text", "今天的目标和打卡完成了吗？打开 App 看看吧") ?: "")
    }
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            // === Appearance ===
            Text("外观", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            Text("主题模式", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            val modes = listOf("light" to "浅色", "dark" to "深色")
            Row(modifier = Modifier.fillMaxWidth()) {
                modes.forEach { (value, label) ->
                    Card(
                        onClick = { themeMode = value },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (themeMode == value)
                                MaterialTheme.colorScheme.primaryContainer  // 选中=深色
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)  // 未选=浅灰
                        )
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("主题颜色", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            val presetColors = ThemeColor.entries.filter { it != ThemeColor.CUSTOM }
            var showCustomDialog by remember { mutableStateOf(false) }
            var editingColorSlot by remember { mutableStateOf<ThemeColor?>(null) }
            var customHexInput by remember { mutableStateOf("") }
            var customNameInput by remember { mutableStateOf("") }

            // Per-slot custom color lookup
            fun getSlotHex(slot: ThemeColor): String =
                prefs.getString("slot_hex_${slot.name}", "") ?: ""
            fun getSlotName(slot: ThemeColor): String =
                prefs.getString("slot_name_${slot.name}", slot.displayName) ?: slot.displayName
            fun slotColor(slot: ThemeColor): Color {
                val h = getSlotHex(slot)
                return if (h.isNotBlank()) parseHexColor(h) ?: slot.primary else slot.primary
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                presetColors.forEach { tc ->
                    val currentColor = slotColor(tc)
                    val currentName = getSlotName(tc)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).combinedClickable(
                            onClick = { themeColor = tc.name },
                            onLongClick = {
                                editingColorSlot = tc
                                customHexInput = getSlotHex(tc)
                                customNameInput = getSlotName(tc)
                                showCustomDialog = true
                            }
                        )
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(currentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (themeColor == tc.name) Text("✓", color = androidx.compose.ui.graphics.Color.White)
                        }
                        Text(currentName, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 2.dp), maxLines = 1)
                    }
                }
            }

            if (showCustomDialog && editingColorSlot != null) {
                val slot = editingColorSlot!!
                AlertDialog(
                    onDismissRequest = { showCustomDialog = false },
                    title = { Text("自定义颜色") },
                    text = {
                        Column {
                            OutlinedTextField(value = customHexInput, onValueChange = { customHexInput = it },
                                label = { Text("色号") }, placeholder = { Text("#607D8B") },
                                singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = customNameInput, onValueChange = { customNameInput = it },
                                label = { Text("名称") }, singleLine = true)
                            parseHexColor(customHexInput)?.let { parsedColor ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(Modifier.size(28.dp).clip(CircleShape).background(parsedColor))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("注意：替代操作将永久覆盖当前颜色，不可撤销。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val parsed = parseHexColor(customHexInput)
                            if (parsed != null) {
                                prefs.edit()
                                    .putString("slot_hex_${slot.name}", customHexInput)
                                    .putString("slot_name_${slot.name}", customNameInput.ifBlank { slot.displayName })
                                    .apply()
                                themeColor = slot.name
                            }
                            showCustomDialog = false
                        },
                            enabled = parseHexColor(customHexInput) != null
                        ) { Text("替代") }
                    },
                    dismissButton = { TextButton(onClick = { showCustomDialog = false }) { Text("取消") } }
                )
            }

            // Apply button
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                onClick = {
                    val version = prefs.getInt("theme_version", 0) + 1
                    prefs.edit()
                        .putString("theme_mode", themeMode)
                        .putString("theme_color", themeColor)
                        .putInt("theme_version", version)
                        .apply()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                )
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("应用主题", style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 14.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // === Reminder ===
            var reminderOn by remember { mutableStateOf(reminderHour >= 0) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("每日提醒", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Switch(checked = reminderOn, onCheckedChange = {
                    reminderOn = it
                    if (!it) { reminderHour = -1; cancelReminder(context) }
                    else if (reminderHour < 0) { reminderHour = 9; reminderMinute = 0 }
                })
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Gray out when reminder is off
            Column(modifier = Modifier.then(if (!reminderOn) Modifier.alpha(0.35f) else Modifier)) {
                // Notification permission check (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasNotifPerm = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasNotifPerm) {
                        Card(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Text("⚠ 未开启通知权限，点击前往系统设置开启",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("提醒时间：", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (reminderHour >= 0) "${reminderHour}:${reminderMinute.toString().padStart(2, '0')}"
                        else "9:00",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(enabled = reminderOn) {
                            TimePickerDialog(context, { _, hour, minute ->
                                reminderHour = hour; reminderMinute = minute
                            }, if (reminderHour >= 0) reminderHour else 9, reminderMinute, true).show()
                        }.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reminderText,
                    onValueChange = { if (reminderOn) reminderText = it },
                    label = { Text("提醒内容") },
                    placeholder = { Text("自定义提醒消息...") },
                    singleLine = true,
                    enabled = reminderOn,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    onClick = {
                        if (reminderOn && reminderHour >= 0) {
                            prefs.edit()
                                .putInt("reminder_hour", reminderHour)
                                .putInt("reminder_minute", reminderMinute)
                                .putString("reminder_text", reminderText)
                                .apply()
                            scheduleReminder(context, reminderHour, reminderMinute, reminderText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    )
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("保存提醒设置", style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 14.dp))
                    }
                }
            }  // End gray-out Column

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // === Data Management ===
            Text("数据管理", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            // Export buttons
            Text("导出数据", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Card(
                    onClick = { DataMigration.exportDatabase(context) },
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    )
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SQLite格式", style = MaterialTheme.typography.bodyMedium)
                        Text("换手机导入用", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Card(
                    onClick = { DataMigration.exportTSV(context) },
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    )
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TSV格式", style = MaterialTheme.typography.bodyMedium)
                        Text("数据分析用", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Import button
            Text("导入数据", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                )
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("从文件导入", style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 14.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Import warning dialog
    if (showImportDialog && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false; pendingImportUri = null },
            title = { Text("⚠ 导入数据") },
            text = {
                Column {
                    Text(
                        "导入操作将覆盖当前所有数据，包括目标、打卡、待办和日志。此操作不可撤销！",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "建议先导出当前数据作为备份。确定要继续吗？",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    DataMigration.importDatabase(context, pendingImportUri!!)
                    showImportDialog = false; pendingImportUri = null
                }) { Text("确认覆盖", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false; pendingImportUri = null
                }) { Text("取消") }
            }
        )
    }
}

private fun scheduleReminder(context: Context, hour: Int, minute: Int, text: String) {
    try {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("reminder_text", text).apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        Toast.makeText(context, "已设置每日 ${hour}:${String.format("%02d", minute)} 提醒", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "设置失败: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun cancelReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
    Toast.makeText(context, "已取消提醒", Toast.LENGTH_SHORT).show()
}
