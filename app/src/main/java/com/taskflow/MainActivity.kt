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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
// Adjust this import to match whatever your project's default theme is named —
// check ui/theme/Theme.kt if unsure.
import com.taskflow.ui.theme.TaskFlowTheme
import com.taskflow.data.entity.Tag
import com.taskflow.data.entity.Task
import com.taskflow.data.entity.TaskList
import com.taskflow.data.repository.TagRepository
import com.taskflow.data.repository.TaskRepository
import com.taskflow.ui.components.EditTaskDialog
import com.taskflow.ui.components.TagPickerDialog
import com.taskflow.ui.screens.AnalyticsScreen
import com.taskflow.ui.screens.JournalScreen
import com.taskflow.ui.screens.ListDetailScreen
import com.taskflow.ui.screens.ListsScreen
import com.taskflow.ui.screens.TagsScreen
import com.taskflow.ui.viewmodel.AnalyticsViewModel
import com.taskflow.ui.viewmodel.InboxViewModel
import com.taskflow.ui.viewmodel.JournalViewModel
import com.taskflow.ui.viewmodel.ListDetailViewModel
import com.taskflow.ui.viewmodel.ListViewModel
import com.taskflow.ui.viewmodel.TagViewModel

/** Top-level destinations shown in the bottom NavigationBar. */
private sealed class Screen {
    object Inbox : Screen()
    object Lists : Screen()
    object Journal : Screen()
    object Tags : Screen()
    object Analytics : Screen()
}

private data class NavItem(val screen: Screen, val icon: ImageVector, val label: String)

private val navItems = listOf(
    NavItem(Screen.Inbox, Icons.Filled.Inbox, "Inbox"),
    NavItem(Screen.Lists, Icons.Filled.List, "Lists"),
    NavItem(Screen.Journal, Icons.AutoMirrored.Filled.MenuBook, "Journal"),
    NavItem(Screen.Tags, Icons.Filled.Sell, "Tags"),
    NavItem(Screen.Analytics, Icons.Filled.BarChart, "Analytics")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TaskFlowApplication

        setContent {
            TaskFlowTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf<Screen>(Screen.Inbox) }

                    val inboxViewModel: InboxViewModel = viewModel(
                        factory = InboxViewModel.provideFactory(app.taskRepository, app.tagRepository)
                    )
                    val listViewModel: ListViewModel = viewModel(
                        factory = ListViewModel.provideFactory(app.listRepository)
                    )
                    val journalViewModel: JournalViewModel = viewModel(
                        factory = JournalViewModel.provideFactory(app.taskRepository, app.tagRepository)
                    )
                    val tagViewModel: TagViewModel = viewModel(
                        factory = TagViewModel.provideFactory(app.tagRepository)
                    )
                    val analyticsViewModel: AnalyticsViewModel = viewModel(
                        factory = AnalyticsViewModel.provideFactory(app.taskRepository)
                    )
                    val lists by listViewModel.lists.collectAsStateWithLifecycle()

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                navItems.forEach { item ->
                                    NavigationBarItem(
                                        selected = screen == item.screen,
                                        onClick = { screen = item.screen },
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (screen) {
                                Screen.Inbox -> InboxScreen(viewModel = inboxViewModel, lists = lists)
                                Screen.Lists -> ListsSection(
                                    listViewModel = listViewModel,
                                    taskRepository = app.taskRepository,
                                    tagRepository = app.tagRepository
                                )
                                Screen.Journal -> JournalScreen(viewModel = journalViewModel)
                                Screen.Tags -> TagsScreen(viewModel = tagViewModel)
                                Screen.Analytics -> AnalyticsScreen(viewModel = analyticsViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Browser-style tab strip for Lists: "All Lists" is a permanent first tab; opening a list
 * from ListsScreen adds it as a closeable tab next to it. Tab state is local to this
 * composable (lost on process death/rotation for now — fine for v1, revisit with
 * rememberSaveable + a custom Saver if that turns out to matter).
 */
@Composable
private fun ListsSection(
    listViewModel: ListViewModel,
    taskRepository: TaskRepository,
    tagRepository: TagRepository
) {
    val lists by listViewModel.lists.collectAsStateWithLifecycle()
    var openTabs by remember { mutableStateOf(listOf<TaskList>()) }
    var selectedTabId by remember { mutableStateOf<Long?>(null) } // null = "All Lists"

    // If a list gets deleted while its tab is open, drop the tab instead of pointing at nothing.
    LaunchedEffect(lists) {
        val validIds = lists.map { it.id }.toSet()
        if (openTabs.any { it.id !in validIds }) {
            openTabs = openTabs.filter { it.id in validIds }
        }
        if (selectedTabId != null && selectedTabId !in validIds) {
            selectedTabId = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val selectedIndex = if (selectedTabId == null) {
            0
        } else {
            (openTabs.indexOfFirst { it.id == selectedTabId } + 1).coerceAtLeast(0)
        }

        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 12.dp,
            // The default indicator indexes into tab positions measured from the previous
            // layout pass; when a tab is added and selected in the same state update, that
            // list is briefly one short and it crashes (IndexOutOfBoundsException in
            // TabRowKt$ScrollableTabRow). Dropping the indicator avoids reading that list at all.
            indicator = {}
        ) {
            Tab(
                selected = selectedTabId == null,
                onClick = { selectedTabId = null },
                text = { Text("All Lists") }
            )
            openTabs.forEach { list ->
                Tab(
                    selected = selectedTabId == list.id,
                    onClick = { selectedTabId = list.id },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(list.name)
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close ${list.name}",
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(16.dp)
                                    .clickable {
                                        openTabs = openTabs.filterNot { it.id == list.id }
                                        if (selectedTabId == list.id) selectedTabId = null
                                    }
                            )
                        }
                    }
                )
            }
        }

        val currentList = openTabs.find { it.id == selectedTabId }
        if (currentList == null) {
            ListsScreen(
                viewModel = listViewModel,
                onOpenList = { list ->
                    if (openTabs.none { it.id == list.id }) openTabs = openTabs + list
                    selectedTabId = list.id
                }
            )
        } else {
            val listDetailViewModel: ListDetailViewModel = viewModel(
                key = "list_detail_${currentList.id}",
                factory = ListDetailViewModel.provideFactory(taskRepository, tagRepository, currentList.id)
            )
            ListDetailScreen(
                listName = currentList.name,
                viewModel = listDetailViewModel
            )
        }
    }
}

@Composable
fun InboxScreen(viewModel: InboxViewModel, lists: List<TaskList>) {
    val tasks by viewModel.inboxTasks.collectAsStateWithLifecycle()
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

        Text("Inbox (${tasks.size}):")

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tasks, key = { it.id }) { task ->
                InboxTaskRow(
                    task = task,
                    lists = lists,
                    onToggleCompleted = { viewModel.toggleCompleted(task) },
                    onDelete = { viewModel.deleteTask(task) },
                    onMoveToList = { listId -> viewModel.moveTaskToList(task, listId) },
                    onEdit = { editingTask = task },
                    onTags = { taggingTask = task }
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
    onEdit: () -> Unit,
    onTags: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
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

            IconButton(onClick = onTags) {
                Icon(Icons.Filled.Label, contentDescription = "Tags")
            }

            // "Move to list" — only meaningful once at least one list exists.
            if (lists.isNotEmpty()) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move to list")
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
}