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

class ListDetailViewModel(
    private val repository: TaskRepository,
    private val tagRepository: TagRepository,
    private val listId: Long
) : ViewModel() {

    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagIdFlow: StateFlow<Long?> = selectedTagId.asStateFlow()

    val tasks: StateFlow<List<Task>> = combine(
        repository.getTasksForList(listId),
        selectedTagId.flatMapLatest { tagId ->
            if (tagId == null) flowOf<List<Task>?>(null) else repository.getTasksByTag(tagId)
        }
    ) { listTasks, taggedOrNull ->
        if (taggedOrNull == null) {
            listTasks
        } else {
            val taggedIds = taggedOrNull.map { it.id }.toSet()
            listTasks.filter { it.id in taggedIds }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setTagFilter(tagId: Long?) {
        selectedTagId.value = tagId
    }

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertTask(Task(title = title.trim(), listId = listId))
        }
    }

    fun updateTaskDetails(task: Task, title: String, description: String?, dueDate: Long?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.updateTask(task.copy(title = title.trim(), description = description, dueDate = dueDate))
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
        fun provideFactory(repository: TaskRepository, tagRepository: TagRepository, listId: Long) = viewModelFactory {
            initializer { ListDetailViewModel(repository, tagRepository, listId) }
        }
    }
}