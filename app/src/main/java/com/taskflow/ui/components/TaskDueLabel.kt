package com.taskflow.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dueDateFormatter = DateTimeFormatter
    .ofPattern("MMM d, h:mm a")
    .withZone(ZoneId.systemDefault())

private val cherryRed = Color(0xFFDE3163)

/** Shows nothing if there's no due date. Renders in Cherry Red — reserved for "critical
 *  state indicators" per the design philosophy — when the task is overdue and not done. */
@Composable
fun TaskDueLabel(dueDate: Long?, isCompleted: Boolean) {
    if (dueDate == null) return
    val isOverdue = !isCompleted && dueDate < System.currentTimeMillis()
    Text(
        text = "Due " + dueDateFormatter.format(Instant.ofEpochMilli(dueDate)),
        style = MaterialTheme.typography.bodySmall,
        color = if (isOverdue) cherryRed else MaterialTheme.colorScheme.onSurfaceVariant
    )
}