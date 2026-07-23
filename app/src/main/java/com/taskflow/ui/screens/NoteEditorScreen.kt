package com.taskflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taskflow.data.entity.Note
import com.taskflow.ui.components.MarkdownText
import com.taskflow.ui.viewmodel.NoteViewModel

/**
 * No Scaffold-level insets worry here beyond the TopAppBar — this still needs its own
 * Scaffold (unlike the other screens) because it needs a back button + title bar, which
 * none of the other tab-content screens do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note,
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.contentMarkdown) }
    var showPreview by remember { mutableStateOf(false) }

    // Save on the way out (back press or navigating away) rather than on every keystroke —
    // avoids hammering the DB while the user is mid-sentence.
    val latestTitle by rememberUpdatedState(title)
    val latestContent by rememberUpdatedState(content)
    DisposableEffect(note.id) {
        onDispose {
            viewModel.updateNote(note, latestTitle, latestContent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title.ifBlank { "Untitled" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showPreview = !showPreview }) {
                        Icon(
                            if (showPreview) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPreview) "Edit" else "Preview"
                        )
                    }
                    IconButton(onClick = {
                        viewModel.deleteNote(note)
                        onBack()
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete note")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Title") }
            )

            if (showPreview) {
                MarkdownText(markdown = content, modifier = Modifier.fillMaxWidth())
            } else {
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text("Write in markdown — **bold**, *italic*, # headers, - bullets") },
                    colors = TextFieldDefaults.colors()
                )
            }
        }
    }
}