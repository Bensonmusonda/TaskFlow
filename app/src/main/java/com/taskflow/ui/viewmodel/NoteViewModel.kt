package com.taskflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.taskflow.data.entity.Note
import com.taskflow.data.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    val notes: StateFlow<List<Note>> = repository.getAllNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Creates a blank note immediately and hands its id back so the caller can open it. */
    fun createNote(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertNote(Note(title = "Untitled", contentMarkdown = ""))
            onCreated(id)
        }
    }

    fun updateNote(note: Note, title: String, contentMarkdown: String) {
        viewModelScope.launch {
            repository.updateNote(
                note.copy(
                    title = title.ifBlank { "Untitled" },
                    contentMarkdown = contentMarkdown,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    companion object {
        fun provideFactory(repository: NoteRepository) = viewModelFactory {
            initializer { NoteViewModel(repository) }
        }
    }
}