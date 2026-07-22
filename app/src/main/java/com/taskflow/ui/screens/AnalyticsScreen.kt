package com.taskflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskflow.ui.viewmodel.AnalyticsViewModel
import com.taskflow.ui.viewmodel.DayActivity
import com.taskflow.ui.viewmodel.activityLevel
import java.time.LocalDate

private val cherryRed = Color(0xFFDE3163)
private const val WEEKS_SHOWN = 20

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val dailyActivity by viewModel.dailyActivity.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val weeks = remember(today) { buildWeekGrid(today, WEEKS_SHOWN) }

    val totalTasksThisPeriod = remember(dailyActivity) {
        dailyActivity.values.sumOf { it.taskCount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Activity Heatmap", style = MaterialTheme.typography.titleMedium)
        Text(
            "Score = tasks completed + unique Growth Domains touched, per day.",
            style = MaterialTheme.typography.bodySmall
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(weeks) { week ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { date ->
                        DayCell(
                            activity = dailyActivity[date],
                            isFuture = date.isAfter(today)
                        )
                    }
                }
            }
        }

        Legend()

        Text(
            "$totalTasksThisPeriod tasks completed in the last $WEEKS_SHOWN weeks.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DayCell(activity: DayActivity?, isFuture: Boolean) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val color = if (isFuture) {
        Color.Transparent
    } else {
        levelColor(activityLevel(activity?.score ?: 0), base)
    }
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
    )
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Less", style = MaterialTheme.typography.bodySmall)
        val base = MaterialTheme.colorScheme.surfaceVariant
        for (level in 0..4) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(levelColor(level, base))
            )
        }
        Text("Full Bloom", style = MaterialTheme.typography.bodySmall)
    }
}

private fun levelColor(level: Int, base: Color): Color {
    if (level == 0) return base
    return lerp(base, cherryRed, level / 4f)
}

/**
 * Builds [weekCount] weeks of dates (Sun-Sat), ending on the week containing [today].
 * Each inner list is 7 dates; dates after [today] are included so the current week's
 * column is a full 7-cell column (the screen just renders those cells blank).
 */
private fun buildWeekGrid(today: LocalDate, weekCount: Int): List<List<LocalDate>> {
    // DayOfWeek.SUNDAY.value == 7; we want 0 when today IS Sunday, working backward from there.
    val daysSinceSunday = today.dayOfWeek.value % 7
    val currentWeekStart = today.minusDays(daysSinceSunday.toLong())
    val gridStart = currentWeekStart.minusWeeks((weekCount - 1).toLong())

    return (0 until weekCount).map { weekIndex ->
        val weekStart = gridStart.plusWeeks(weekIndex.toLong())
        (0..6).map { dayOffset -> weekStart.plusDays(dayOffset.toLong()) }
    }
}