package com.taskflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskflow.data.entity.Tag
import com.taskflow.data.entity.Task
import com.taskflow.ui.components.EditTaskDialog
import com.taskflow.ui.components.TagPickerDialog
import com.taskflow.ui.viewmodel.ListDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    listName: String,
    viewModel: ListDetailViewModel,
    onBack: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val selectedTagId by viewModel.selectedTagIdFlow.collectAsStateWithLifecycle()

    var newTaskTitle by remember { mutableStateOf("") }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var taggingTask by remember { mutableStateOf<Task?>(null) }
    var taggingTaskCurrentTags by remember { mutableStateOf<List<Tag>>(emptyList()) }

    editingTask?.let { task ->
        EditTaskDialog(
            task = task,
            onConfirm = { title, description ->
                viewModel.updateTaskDetails(task, title, description)
                editingTask = null
            },
            onDismiss = { editingTask = null }
        )
    }

    taggingTask?.let { task ->
        LaunchedEffect(task.id) {
            taggingTaskCurrentTags = viewModel.getTagsForTask(task.id)
        }
        val attachedIds = taggingTaskCurrentTags.map { it.id }.toSet()
        TagPickerDialog(
            allTags = allTags,
            attachedTagIds = attachedIds,
            onToggleTag = { tag ->
                val currentlyAttached = tag.id in attachedIds
                viewModel.toggleTagOnTask(task.id, tag, currentlyAttached)
                taggingTaskCurrentTags = if (currentlyAttached) {
                    taggingTaskCurrentTags.filterNot { it.id == tag.id }
                } else {
                    taggingTaskCurrentTags + tag
                }
            },
            onCreateAndAttachTag = { name -> viewModel.createAndAttachTag(task.id, name) },
            onDismiss = { taggingTask = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("New task") }
                )
                Button(
                    onClick = {
                        viewModel.addTask(newTaskTitle)
                        newTaskTitle = ""
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Add")
                }
            }

            if (allTags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedTagId == null,
                            onClick = { viewModel.setTagFilter(null) },
                            label = { Text("All") }
                        )
                    }
                    items(allTags, key = { it.id }) { tag ->
                        FilterChip(
                            selected = selectedTagId == tag.id,
                            onClick = { viewModel.setTagFilter(tag.id) },
                            label = { Text("#${tag.name}") }
                        )
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggleCompleted = { viewModel.toggleCompleted(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        onEdit = { editingTask = task },
                        onTags = { taggingTask = task }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onTags: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleCompleted() })
        Text(
            text = task.title,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEdit),
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
        )
        Button(onClick = onTags) {
            Text("Tags")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete task")
        }
    }
}