package com.example.todoapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 锁定状态下的只读文本框：不可编辑，但内容超出高度上限时可上下滑动预览全部内容。
 * 外观沿用 OutlinedTextField 的描边风格；[onDoubleTap] 为空表示无解锁入口。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReadOnlyTextBlock(
    text: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    minHeight: Dp = 80.dp,
    maxHeight: Dp = 152.dp,
    onDoubleTap: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val hasOverflow = scrollState.maxValue > 0

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight, max = maxHeight)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.extraSmall
                )
                .verticalScroll(scrollState)
                .then(
                    if (onDoubleTap != null) {
                        Modifier.combinedClickable(onClick = {}, onDoubleClick = onDoubleTap)
                    } else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = text.ifBlank { "—" },
                style = MaterialTheme.typography.bodyLarge,
                color = if (text.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                }
            )
        }

        if (hasOverflow) {
            Text(
                text = "可上下滑动查看全部内容",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}
