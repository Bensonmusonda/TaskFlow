package com.taskflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskflow.data.entity.Task
import com.taskflow.ui.components.TaskFlowAccent
import com.taskflow.ui.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val visibleMonth by viewModel.visibleMonth.collectAsStateWithLifecycle()
    val tasksByDate by viewModel.tasksByDate.collectAsStateWithLifecycle()
    var selectedDate by remember(visibleMonth) { mutableStateOf<LocalDate?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.goToPreviousMonth() }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = monthFormatter.format(visibleMonth),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { viewModel.goToNextMonth() }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        val firstOfMonth = visibleMonth.atDay(1)
        val leadingBlanks = firstOfMonth.dayOfWeek.value % 7 // Sunday-start grid
        val daysInMonth = visibleMonth.lengthOfMonth()
        val cells: List<LocalDate?> = List(leadingBlanks) { null } + (1..daysInMonth).map { visibleMonth.atDay(it) }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(cells) { date ->
                DayCell(
                    date = date,
                    hasTasks = date != null && !tasksByDate[date].isNullOrEmpty(),
                    isSelected = date != null && date == selectedDate,
                    isToday = date != null && date == LocalDate.now(),
                    onClick = { date?.let { selectedDate = if (selectedDate == it) null else it } }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        val agendaDate = selectedDate
        val agendaTasks = agendaDate?.let { tasksByDate[it] } ?: emptyList()
        if (agendaDate == null) {
            Text("Tap a day to see what's due.", style = MaterialTheme.typography.bodySmall)
        } else if (agendaTasks.isEmpty()) {
            Text("Nothing due on ${agendaDate}.", style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn {
                items(agendaTasks, key = { it.id }) { task -> AgendaRow(task) }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    hasTasks: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) TaskFlowAccent.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
            .then(if (date != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (isToday) TaskFlowAccent else MaterialTheme.colorScheme.onSurface
                )
                if (hasTasks) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(TaskFlowAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaRow(task: Task) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
        )
    }
}