package com.taskflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
// Adjust this import to match whatever your project's default theme is named —
// check ui/theme/Theme.kt if unsure.
import com.taskflow.ui.theme.TaskFlowTheme
import com.taskflow.data.entity.Task
import com.taskflow.data.entity.TaskList
import com.taskflow.ui.components.EditTaskDialog
import com.taskflow.ui.screens.JournalScreen
import com.taskflow.ui.screens.ListDetailScreen
import com.taskflow.ui.screens.ListsScreen
import com.taskflow.ui.viewmodel.InboxViewModel
import com.taskflow.ui.viewmodel.JournalViewModel
import com.taskflow.ui.viewmodel.ListDetailViewModel
import com.taskflow.ui.viewmodel.ListViewModel

// Placeholder routing — this becomes the real tab-nav shell (tracker section 10) later.
private sealed class Screen {
    object Inbox : Screen()
    object Lists : Screen()
    object Journal : Screen()
    data class ListDetail(val list: TaskList) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TaskFlowApplication

        setContent {
            TaskFlowTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf<Screen>(Screen.Inbox) }

                    val inboxViewModel: InboxViewModel = viewModel(
                        factory = InboxViewModel.provideFactory(app.taskRepository)
                    )
                    val listViewModel: ListViewModel = viewModel(
                        factory = ListViewModel.provideFactory(app.listRepository)
                    )
                    val journalViewModel: JournalViewModel = viewModel(
                        factory = JournalViewModel.provideFactory(app.taskRepository, app.tagRepository)
                    )
                    val lists by listViewModel.lists.collectAsStateWithLifecycle()

                    Column(modifier = Modifier.fillMaxSize()) {
                        when (val current = screen) {
                            is Screen.ListDetail -> {
                                val listDetailViewModel: ListDetailViewModel = viewModel(
                                    key = "list_detail_${current.list.id}",
                                    factory = ListDetailViewModel.provideFactory(app.taskRepository, current.list.id)
                                )
                                ListDetailScreen(
                                    listName = current.list.name,
                                    viewModel = listDetailViewModel,
                                    onBack = { screen = Screen.Lists }
                                )
                            }
                            else -> {
                                RootTabs(
                                    current = current,
                                    onSelect = { screen = it }
                                )
                                when (current) {
                                    Screen.Inbox -> InboxScreen(
                                        viewModel = inboxViewModel,
                                        lists = lists
                                    )
                                    Screen.Lists -> ListsScreen(
                                        viewModel = listViewModel,
                                        onOpenList = { screen = Screen.ListDetail(it) }
                                    )
                                    Screen.Journal -> JournalScreen(viewModel = journalViewModel)
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RootTabs(current: Screen, onSelect: (Screen) -> Unit) {
    val tabs = listOf("Inbox" to Screen.Inbox, "Lists" to Screen.Lists, "Journal" to Screen.Journal)
    val selectedIndex = tabs.indexOfFirst { it.second == current }.coerceAtLeast(0)
    TabRow(selectedTabIndex = selectedIndex) {
        tabs.forEachIndexed { index, (label, target) ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(target) },
                text = { Text(label) }
            )
        }
    }
}

@Composable
fun InboxScreen(viewModel: InboxViewModel, lists: List<TaskList>) {
    val tasks by viewModel.inboxTasks.collectAsStateWithLifecycle()
    var newTaskTitle by remember { mutableStateOf("") }
    var editingTask by remember { mutableStateOf<Task?>(null) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
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

        Text("Inbox (${tasks.size}):")

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(tasks, key = { it.id }) { task ->
                InboxTaskRow(
                    task = task,
                    lists = lists,
                    onToggleCompleted = { viewModel.toggleCompleted(task) },
                    onDelete = { viewModel.deleteTask(task) },
                    onMoveToList = { listId -> viewModel.moveTaskToList(task, listId) },
                    onEdit = { editingTask = task }
                )
            }
        }
    }
}

@Composable
private fun InboxTaskRow(
    task: Task,
    lists: List<TaskList>,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
    onMoveToList: (Long) -> Unit,
    onEdit: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

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

        // "Move to list" — only meaningful once at least one list exists.
        if (lists.isNotEmpty()) {
            Box {
                Button(onClick = { menuExpanded = true }) {
                    Text("Move")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    lists.forEach { list ->
                        DropdownMenuItem(
                            text = { Text(list.name) },
                            onClick = {
                                onMoveToList(list.id)
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete task")
        }
    }
}