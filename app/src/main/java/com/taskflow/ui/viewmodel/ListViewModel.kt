package com.taskflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.taskflow.data.entity.TaskList
import com.taskflow.data.repository.ListRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListViewModel(private val repository: ListRepository) : ViewModel() {

    val lists: StateFlow<List<TaskList>> = repository.getAllLists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addList(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertList(TaskList(name = name.trim(), colorHex = colorHex))
        }
    }

    fun deleteList(list: TaskList) {
        viewModelScope.launch {
            repository.deleteList(list)
        }
    }

    companion object {
        fun provideFactory(repository: ListRepository) = viewModelFactory {
            initializer { ListViewModel(repository) }
        }
    }
}