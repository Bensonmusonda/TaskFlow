package com.taskflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.taskflow.data.entity.Tag
import com.taskflow.data.entity.Task
import com.taskflow.data.repository.TagRepository
import com.taskflow.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(
    private val repository: TaskRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagIdFlow: StateFlow<Long?> = selectedTagId.asStateFlow()

    /**
     * Inbox tasks, optionally filtered down to only those carrying [selectedTagId].
     * When no tag is selected, this is just the plain Inbox list.
     */
    val inboxTasks: StateFlow<List<Task>> = combine(
        repository.getInboxTasks(),
        selectedTagId.flatMapLatest { tagId ->
            if (tagId == null) flowOf<List<Task>?>(null) else repository.getTasksByTag(tagId)
        }
    ) { inbox, taggedOrNull ->
        if (taggedOrNull == null) {
            inbox
        } else {
            val taggedIds = taggedOrNull.map { it.id }.toSet()
            inbox.filter { it.id in taggedIds }
        }
    }.stateIn(
        scope = viewModelScope,
        // Keeps collecting 5s after the last observer goes away (e.g. rotation),
        // instead of dropping and re-querying immediately.
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setTagFilter(tagId: Long?) {
        selectedTagId.value = tagId
    }

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

    suspend fun getTagsForTask(taskId: Long): List<Tag> =
        repository.getTaskWithTags(taskId)?.tags ?: emptyList()

    fun toggleTagOnTask(taskId: Long, tag: Tag, currentlyAttached: Boolean) {
        viewModelScope.launch {
            if (currentlyAttached) repository.detachTag(taskId, tag.id) else repository.attachTag(taskId, tag.id)
        }
    }

    fun createAndAttachTag(taskId: Long, name: String) {
        viewModelScope.launch {
            val tag = tagRepository.getOrCreateTag(name)
            repository.attachTag(taskId, tag.id)
        }
    }

    companion object {
        fun provideFactory(repository: TaskRepository, tagRepository: TagRepository) = viewModelFactory {
            initializer { InboxViewModel(repository, tagRepository) }
        }
    }
}