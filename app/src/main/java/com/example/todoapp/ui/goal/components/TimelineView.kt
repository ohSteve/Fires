package com.example.todoapp.ui.goal.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoapp.data.entity.MilestoneEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineView(
    milestones: List<MilestoneEntity>,
    onMilestoneClick: (MilestoneEntity) -> Unit,
    isAchieved: Boolean
) {
    if (milestones.isEmpty()) {
        Text(
            "暂无里程碑",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    ) {
        milestones.forEachIndexed { index, milestone ->
            val isFirst = index == 0
            val isLast = index == milestones.lastIndex
            TimelineItem(
                milestone = milestone,
                isFirst = isFirst,
                isLast = isLast,
                noMoreItems = isLast && isAchieved,
                onClick = { onMilestoneClick(milestone) }
            )
        }
    }
}

@Composable
private fun TimelineItem(
    milestone: MilestoneEntity,
    isFirst: Boolean,
    isLast: Boolean,
    noMoreItems: Boolean,
    onClick: () -> Unit
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline column: dot centered vertically, lines connecting through
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Draw lines and dot using Canvas
            Canvas(modifier = Modifier.fillMaxHeight().width(32.dp)) {
                val centerX = size.width / 2
                val dotY = size.height / 2
                val dotRadius = 6.dp.toPx()

                // Top connecting line (from top to dot top edge)
                if (!isFirst) {
                    drawLine(
                        color = lineColor,
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, dotY - dotRadius),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Bottom connecting line (from dot bottom edge to bottom)
                val bottomEnd = if (isLast && noMoreItems) dotY + dotRadius + 12.dp.toPx()
                                else size.height
                drawLine(
                    color = if (isLast && noMoreItems) lineColor.copy(alpha = 0f)
                            else lineColor,
                    start = Offset(centerX, dotY + dotRadius),
                    end = Offset(centerX, bottomEnd),
                    strokeWidth = 2.dp.toPx()
                )

                // Dot
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(centerX, dotY)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Milestone card
        Card(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = formatDate(milestone.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = milestone.completedTaskName.ifBlank { "未命名" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (milestone.completedTaskDetail.isNotBlank()) {
                    Text(
                        text = milestone.completedTaskDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

private fun formatDate(timestamp: Long): String {
    return dateFormat.format(Date(timestamp))
}
