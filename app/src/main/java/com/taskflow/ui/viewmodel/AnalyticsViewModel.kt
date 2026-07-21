package com.taskflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.taskflow.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One calendar day's worth of activity.
 * Score = Total Tasks Completed + Unique Growth Domains Touched (spec section 4).
 */
data class DayActivity(
    val date: LocalDate,
    val taskCount: Int,
    val uniqueTagCount: Int
) {
    val score: Int get() = taskCount + uniqueTagCount
}

/**
 * Any day at or above this score renders identically as "Full Bloom" — caps the visual
 * scale so one unusually active day doesn't wash out every other day's intensity.
 */
const val FULL_BLOOM_THRESHOLD = 5

/** Buckets a raw score into a 0–4 display level, capped at [FULL_BLOOM_THRESHOLD]. */
fun activityLevel(score: Int): Int = when {
    score <= 0 -> 0
    score == 1 -> 1
    score in 2..3 -> 2
    score == 4 -> 3
    else -> 4 // Full Bloom
}

class AnalyticsViewModel(taskRepository: TaskRepository) : ViewModel() {

    /** Keyed by calendar day (device-local time zone), only days with activity present. */
    val dailyActivity: StateFlow<Map<LocalDate, DayActivity>> = taskRepository
        .observeCompletedTasksWithTags()
        .map { tasksWithTags ->
            tasksWithTags
                .filter { it.task.completedAt != null }
                .groupBy { entry ->
                    Instant.ofEpochMilli(entry.task.completedAt!!)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }
                .mapValues { (date, entries) ->
                    val uniqueTagIds = entries.flatMap { it.tags }.map { it.id }.toSet()
                    DayActivity(
                        date = date,
                        taskCount = entries.size,
                        uniqueTagCount = uniqueTagIds.size
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    companion object {
        fun provideFactory(taskRepository: TaskRepository) = viewModelFactory {
            initializer { AnalyticsViewModel(taskRepository) }
        }
    }
}