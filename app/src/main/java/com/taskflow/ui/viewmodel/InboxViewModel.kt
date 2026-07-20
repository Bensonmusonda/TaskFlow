package com.taskflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.taskflow.data.entity.Task
import com.taskflow.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(private val repository: TaskRepository) : ViewModel() {

    val inboxTasks: StateFlow<List<Task>> = repository.getInboxTasks()
        .stateIn(
            scope = viewModelScope,
            // Keeps collecting 5s after the last observer goes away (e.g. rotation),
            // instead of dropping and re-querying immediately.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addTask(title: String, description: String? = null, dueDate: Long? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertTask(Task(title = title.trim(), description = description, dueDate = dueDate))
        }
    }

    fun updateTaskDetails(task: Task, title: String, description: String?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.updateTask(task.copy(title = title.trim(), description = description))
        }
    }

    fun toggleCompleted(task: Task) {
        viewModelScope.launch {
            repository.setTaskCompleted(task, completed = !task.isCompleted)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    /** Moves a task out of the Inbox onto a list; it naturally drops out of [inboxTasks]. */
    fun moveTaskToList(task: Task, listId: Long) {
        viewModelScope.launch {
            repository.moveTaskToList(task, listId)
        }
    }

    companion object {
        fun provideFactory(repository: TaskRepository) = viewModelFactory {
            initializer { InboxViewModel(repository) }
        }
    }
}