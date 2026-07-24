package com.taskflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.taskflow.data.entity.Task
import com.taskflow.data.repository.TaskRepository
import kotlinx.coroutines.launch

class AddTaskViewModel(private val repository: TaskRepository) : ViewModel() {

    /** Always creates into the Inbox (listId = null) — matches the mockup, which has no
     *  list picker. Move it to a list afterward from the Inbox if needed. */
    fun saveTask(title: String, description: String?, dueDate: Long?, onSaved: () -> Unit) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertTask(
                Task(title = title.trim(), description = description, dueDate = dueDate)
            )
            onSaved()
        }
    }

    companion object {
        fun provideFactory(repository: TaskRepository) = viewModelFactory {
            initializer { AddTaskViewModel(repository) }
        }
    }
}