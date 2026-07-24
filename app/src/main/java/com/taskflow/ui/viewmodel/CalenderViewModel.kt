package com.taskflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.taskflow.data.entity.Task
import com.taskflow.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarViewModel(repository: TaskRepository) : ViewModel() {

    /** All due-date tasks bucketed by calendar day — cheap enough to keep unfiltered by
     *  month and just slice per-day in the UI; avoids re-querying on every month flip. */
    val tasksByDate: StateFlow<Map<LocalDate, List<Task>>> = repository.observeTasksWithDueDates()
        .map { tasks ->
            val zone = ZoneId.systemDefault()
            tasks.groupBy { task ->
                Instant.ofEpochMilli(task.dueDate!!).atZone(zone).toLocalDate()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    fun goToPreviousMonth() {
        _visibleMonth.value = _visibleMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        _visibleMonth.value = _visibleMonth.value.plusMonths(1)
    }

    companion object {
        fun provideFactory(repository: TaskRepository) = viewModelFactory {
            initializer { CalendarViewModel(repository) }
        }
    }
}