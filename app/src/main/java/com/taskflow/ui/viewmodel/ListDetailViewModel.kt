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

class ListDetailViewModel(
    private val repository: TaskRepository,
    private val listId: Long
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.getTasksForList(listId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertTask(Task(title = title.trim(), listId = listId))
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

    /** Sends a task back to the Inbox. */
    fun removeFromList(task: Task) {
        viewModelScope.launch {
            repository.moveTaskToList(task, listId = null)
        }
    }

    companion object {
        fun provideFactory(repository: TaskRepository, listId: Long) = viewModelFactory {
            initializer { ListDetailViewModel(repository, listId) }
        }
    }
}