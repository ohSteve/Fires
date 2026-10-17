package com.example.todoapp.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeColor(val primary: Color, val displayName: String) {
    MUTED_BLUE(Color(0xFF607D8B), "灰蓝"),
    SOFT_GREEN(Color(0xFF81C784), "柔绿"),
    SOFT_PURPLE(Color(0xFFB39DDB), "淡紫"),
    SOFT_ORANGE(Color(0xFFFFB74D), "暖橙"),
    SOFT_TEAL(Color(0xFF80CBC4), "浅青"),
    SOFT_ROSE(Color(0xFFF48FB1), "柔粉"),
    CUSTOM(Color(0xFF607D8B), "自定义")
}

fun lightColors(primary: Color) = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.12f),
    secondary = Color(0xFF546E7A),
    surface = Color(0xFFFEFBFF),
)

fun darkColors(primary: Color) = darkColorScheme(
    primary = primary.copy(alpha = 0.85f),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = primary.copy(alpha = 0.25f),
    secondary = Color(0xFFB0BEC5),
    surface = Color(0xFF1B1B1F),
)

fun parseHexColor(hex: String): Color? {
    return try {
        val clean = hex.trimStart('#')
        if (clean.length == 6) Color(0xFF000000 or (clean.toLong(16) and 0xFFFFFF))
        else null
    } catch (e: Exception) { null }
}

@Composable
fun TodoTheme(
    themeMode: String,
    themeColor: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    val primary = try {
        val slot = ThemeColor.valueOf(themeColor)
        val slotHex = prefs.getString("slot_hex_${slot.name}", "") ?: ""
        if (slotHex.isNotBlank()) parseHexColor(slotHex) ?: slot.primary
        else slot.primary
    } catch (e: Exception) {
        ThemeColor.MUTED_BLUE.primary
    }

    val darkTheme = themeMode == "dark"
    val colorScheme = if (darkTheme) darkColors(primary) else lightColors(primary)

    MaterialTheme(colorScheme = colorScheme, content = content)
}
