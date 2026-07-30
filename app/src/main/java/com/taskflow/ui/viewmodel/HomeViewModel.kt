package com.taskflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.taskflow.data.entity.Task
import com.taskflow.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private const val UPCOMING_LIMIT = 5

class HomeViewModel(private val repository: TaskRepository) : ViewModel() {

    private val dueDateTasks = repository.observeTasksWithDueDates()

    val upcomingTasks: StateFlow<List<Task>> = dueDateTasks
        .map { tasks -> tasks.filter { !it.isCompleted }.take(UPCOMING_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayTasks: StateFlow<List<Task>> = dueDateTasks
        .map { tasks ->
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            // Deliberately not filtering !isCompleted here — the Today card wants
            // checked-off tasks to stay visible (with the checkbox ticked), not disappear.
            tasks.filter { task ->
                task.dueDate != null &&
                        java.time.Instant.ofEpochMilli(task.dueDate).atZone(zone).toLocalDate() == today
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleCompleted(task: Task) {
        viewModelScope.launch {
            repository.setTaskCompleted(task, completed = !task.isCompleted)
        }
    }

    companion object {
        fun provideFactory(repository: TaskRepository) = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}