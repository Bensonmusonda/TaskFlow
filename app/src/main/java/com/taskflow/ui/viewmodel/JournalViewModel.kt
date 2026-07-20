package com.taskflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.taskflow.data.entity.Task
import com.taskflow.data.repository.TagRepository
import com.taskflow.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JournalViewModel(
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    /** Completed-task history feed — includes tasks completed normally and retrospectively. */
    val completedTasks: StateFlow<List<Task>> = taskRepository.getCompletedTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Adds a "Retrospective Entry" — a task that's already done — with optional tags.
     * [tagNamesCsv] is a raw comma-separated string straight from a text field; tags that
     * don't exist yet are created on the fly (get-or-create by name).
     */
    fun addRetrospectiveEntry(title: String, description: String?, tagNamesCsv: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val tagNames = tagNamesCsv.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

            val tagIds = tagNames.map { name -> tagRepository.getOrCreateTag(name).id }

            taskRepository.insertRetrospectiveEntry(
                task = Task(title = title.trim(), description = description),
                tagIds = tagIds.ifEmpty { null }
            )
        }
    }

    companion object {
        fun provideFactory(taskRepository: TaskRepository, tagRepository: TagRepository) = viewModelFactory {
            initializer { JournalViewModel(taskRepository, tagRepository) }
        }
    }
}