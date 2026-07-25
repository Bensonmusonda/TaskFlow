package com.taskflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskflow.data.entity.Task
import com.taskflow.data.entity.TaskList
import com.taskflow.ui.components.AccentLinkButton
import com.taskflow.ui.components.FloatingDivider
import com.taskflow.ui.components.TaskDueLabel
import com.taskflow.ui.components.TaskFlowAccent
import com.taskflow.ui.components.TaskFlowCheckbox
import com.taskflow.ui.viewmodel.DayActivity
import com.taskflow.ui.viewmodel.HomeViewModel
import com.taskflow.ui.viewmodel.JournalViewModel
import com.taskflow.ui.viewmodel.ListViewModel
import com.taskflow.ui.viewmodel.activityLevel
import java.time.LocalDate

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    listViewModel: ListViewModel,
    journalViewModel: JournalViewModel,
    dailyActivity: Map<LocalDate, DayActivity>,
    onAddTask: () -> Unit,
    onOpenLists: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenAnalytics: () -> Unit
) {
    val upcoming by homeViewModel.upcomingTasks.collectAsStateWithLifecycle()
    val today by homeViewModel.todayTasks.collectAsStateWithLifecycle()
    val lists by listViewModel.lists.collectAsStateWithLifecycle()
    val journalEntries by journalViewModel.completedTasks.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    var journalDraft by remember { mutableStateOf("") }

    // No Scaffold here on purpose — a Scaffold topBar stays pinned while content scrolls
    // beneath it. The app name/hamburger row is just the first item in the same scrollable
    // column as everything else, so it scrolls away like the rest of the page.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("TaskFlow", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Add task") },
                        onClick = { menuExpanded = false; onAddTask() }
                    )
                    DropdownMenuItem(
                        text = { Text("Lists") },
                        onClick = { menuExpanded = false; onOpenLists() }
                    )
                }
            }
        }

        // Upcoming
        Text(
            "Upcoming",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        upcoming.forEachIndexed { index, task ->
            UpcomingRow(task = task, onToggle = { homeViewModel.toggleCompleted(task) })
            if (index != upcoming.lastIndex) FloatingDivider()
        }
        if (upcoming.isEmpty()) {
            Text("Nothing upcoming.", style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Today", fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 6.dp))
                TodayCard(tasks = today, onToggle = { homeViewModel.toggleCompleted(it) })
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Lists", fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 6.dp))
                ListsPreviewCard(lists = lists.take(3), onOpenLists = onOpenLists)
            }
        }

        // Journal preview
        Text(
            "Journal",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TextField(
            value = journalDraft,
            onValueChange = { journalDraft = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            placeholder = { Text("What did you get done today?") },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            trailingIcon = {
                IconButton(onClick = {
                    if (journalDraft.isNotBlank()) {
                        journalViewModel.addRetrospectiveEntry(journalDraft, null, "")
                        journalDraft = ""
                    }
                }) {
                    Icon(Icons.Filled.Send, contentDescription = "Log entry", tint = TaskFlowAccent)
                }
            }
        )
        journalEntries.firstOrNull()?.let { entry ->
            Text(
                entry.title,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        AccentLinkButton(
            text = "View journal",
            onClick = onOpenJournal,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        // Analytics preview
        Text(
            "Analytics",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .clickable(onClick = onOpenAnalytics)
        )
        CompactHeatmap(
            dailyActivity = dailyActivity,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .clickable(onClick = onOpenAnalytics)
        )
    }
}

@Composable
private fun UpcomingRow(task: Task, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.title,
                fontWeight = FontWeight.ExtraBold,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
            )
            TaskDueLabel(dueDate = task.dueDate, isCompleted = task.isCompleted)
        }
        TaskFlowCheckbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun TodayCard(tasks: List<Task>, onToggle: (Task) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TaskFlowAccent),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (tasks.isEmpty()) {
                Text("Nothing due today", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            } else {
                tasks.take(3).forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TaskFlowCheckbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggle(task) },
                            uncheckedTint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            task.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListsPreviewCard(lists: List<TaskList>, onOpenLists: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        // Extra start padding per the latest feedback — left edge felt cramped against
        // the border compared to the Today card's tighter, uniform padding.
        Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 10.dp, bottom = 12.dp)) {
            if (lists.isEmpty()) {
                Text("No lists yet", style = MaterialTheme.typography.bodySmall)
            } else {
                lists.forEach { list ->
                    Text(
                        list.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            AccentLinkButton(text = "All lists", onClick = onOpenLists)
        }
    }
}

/**
 * Fills the full available width regardless of how many weeks that ends up showing —
 * cell size is derived from the width, not fixed, and each column is exactly
 * [ROWS_PER_WEEK] cells tall (5, per the latest feedback) rather than a full 7-day week
 * like the real Analytics screen. Shows the most recent [ROWS_PER_WEEK]-day slice of
 * each week, ending at the current week.
 */
private const val ROWS_PER_WEEK = 5
private val CELL_GAP = 3.dp
private val TARGET_CELL_SIZE = 14.dp

@Composable
private fun CompactHeatmap(
    dailyActivity: Map<LocalDate, DayActivity>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val base = MaterialTheme.colorScheme.surfaceVariant

    BoxWithConstraints(modifier = modifier) {
        val weeksToShow = (((maxWidth + CELL_GAP) / (TARGET_CELL_SIZE + CELL_GAP)))
            .toInt()
            .coerceAtLeast(1)

        val weeks = remember(today, weeksToShow) {
            val daysSinceSunday = today.dayOfWeek.value % 7
            val currentWeekStart = today.minusDays(daysSinceSunday.toLong())
            val gridStart = currentWeekStart.minusWeeks((weeksToShow - 1).toLong())
            (0 until weeksToShow).map { w ->
                (0 until ROWS_PER_WEEK).map { d -> gridStart.plusWeeks(w.toLong()).plusDays(d.toLong()) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CELL_GAP)
        ) {
            weeks.forEach { week ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(CELL_GAP)
                ) {
                    week.forEach { date ->
                        val level = if (date.isAfter(today)) -1 else activityLevel(dailyActivity[date]?.score ?: 0)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(
                                    when {
                                        level < 0 -> Color.Transparent
                                        level == 0 -> base
                                        else -> lerp(base, TaskFlowAccent, level / 4f)
                                    },
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}