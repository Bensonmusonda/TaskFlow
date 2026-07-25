package com.taskflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskflow.data.entity.Tag
import com.taskflow.data.entity.Task
import com.taskflow.data.entity.TaskList
import com.taskflow.data.repository.TagRepository
import com.taskflow.data.repository.TaskRepository
import com.taskflow.ui.components.EditTaskDialog
import com.taskflow.ui.components.TagPickerDialog
import com.taskflow.ui.components.TaskDueLabel
import com.taskflow.ui.screens.AddTaskScreen
import com.taskflow.ui.screens.AnalyticsScreen
import com.taskflow.ui.screens.CalendarScreen
import com.taskflow.ui.screens.HomeScreen
import com.taskflow.ui.screens.JournalScreen
import com.taskflow.ui.screens.ListDetailScreen
import com.taskflow.ui.screens.ListsScreen
import com.taskflow.ui.screens.NoteEditorScreen
import com.taskflow.ui.screens.NotesScreen
import com.taskflow.ui.screens.SettingsScreen
import com.taskflow.ui.viewmodel.AddTaskViewModel
import com.taskflow.ui.viewmodel.AnalyticsViewModel
import com.taskflow.ui.viewmodel.CalendarViewModel
import com.taskflow.ui.viewmodel.HomeViewModel
import com.taskflow.ui.viewmodel.InboxViewModel
import com.taskflow.ui.viewmodel.JournalViewModel
import com.taskflow.ui.viewmodel.ListDetailViewModel
import com.taskflow.ui.viewmodel.ListViewModel
import com.taskflow.ui.viewmodel.NoteViewModel
import com.taskflow.ui.viewmodel.TagViewModel
// Adjust this import to match whatever your project's default theme is named —
// check ui/theme/Theme.kt if unsure.
import com.taskflow.ui.theme.TaskFlowTheme

/** The 5 bottom-nav destinations. Lists/Journal/Add-task are reached as overlays instead —
 *  see [Overlay] — since they're "drill in from Home" flows, not persistent tabs. */
private sealed class Screen {
    object Home : Screen()
    object Calendar : Screen()
    object Analytics : Screen()
    object Notes : Screen()
    object Settings : Screen()
}

/** Full-screen destinations reached from Home (hamburger menu or preview-card buttons).
 *  Rendered on top of the tab content, with the bottom nav hidden while active. */
private sealed class Overlay {
    object Lists : Overlay()
    object Journal : Overlay()
    object AddTask : Overlay()
}

private data class NavItem(val screen: Screen, val icon: ImageVector, val label: String)

private val navItems = listOf(
    NavItem(Screen.Home, Icons.Filled.Home, "Home"),
    NavItem(Screen.Calendar, Icons.Filled.CalendarMonth, "Calendar"),
    NavItem(Screen.Analytics, Icons.Filled.BarChart, "Analytics"),
    NavItem(Screen.Notes, Icons.Filled.Notes, "Notes"),
    NavItem(Screen.Settings, Icons.Filled.Settings, "Settings")
)

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val app = application as TaskFlowApplication

        setContent {
            TaskFlowTheme {
                val view = androidx.compose.ui.platform.LocalView.current
                val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                if (!view.isInEditMode) {
                    androidx.compose.runtime.SideEffect {
                        val window = this@MainActivity.window
                        androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                            // Dark icons/text on light backgrounds, light on dark — without this,
                            // the status bar icons default to one appearance regardless of theme,
                            // which is exactly the poor-contrast-in-light-mode symptom.
                            isAppearanceLightStatusBars = !darkTheme
                            isAppearanceLightNavigationBars = !darkTheme
                        }
                    }
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                    var overlay by remember { mutableStateOf<Overlay?>(null) }

                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.provideFactory(app.taskRepository)
                    )
                    val calendarViewModel: CalendarViewModel = viewModel(
                        factory = CalendarViewModel.provideFactory(app.taskRepository)
                    )
                    val addTaskViewModel: AddTaskViewModel = viewModel(
                        factory = AddTaskViewModel.provideFactory(app.taskRepository)
                    )
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
                    val noteViewModel: NoteViewModel = viewModel(
                        factory = NoteViewModel.provideFactory(app.noteRepository)
                    )
                    val dailyActivity by analyticsViewModel.dailyActivity.collectAsStateWithLifecycle()

                    Scaffold(
                        bottomBar = {
                            if (overlay == null) {
                                TaskFlowBottomNav(current = screen, onSelect = { screen = it })
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (overlay) {
                                Overlay.Lists -> OverlayScaffold(title = "Lists", onBack = { overlay = null }) {
                                    ListsSection(
                                        inboxViewModel = inboxViewModel,
                                        listViewModel = listViewModel,
                                        taskRepository = app.taskRepository,
                                        tagRepository = app.tagRepository
                                    )
                                }
                                Overlay.Journal -> OverlayScaffold(title = "Journal", onBack = { overlay = null }) {
                                    JournalScreen(viewModel = journalViewModel)
                                }
                                Overlay.AddTask -> AddTaskScreen(
                                    viewModel = addTaskViewModel,
                                    onBack = { overlay = null }
                                )
                                null -> when (screen) {
                                    Screen.Home -> HomeScreen(
                                        homeViewModel = homeViewModel,
                                        listViewModel = listViewModel,
                                        journalViewModel = journalViewModel,
                                        dailyActivity = dailyActivity,
                                        onAddTask = { overlay = Overlay.AddTask },
                                        onOpenLists = { overlay = Overlay.Lists },
                                        onOpenJournal = { overlay = Overlay.Journal },
                                        onOpenAnalytics = { screen = Screen.Analytics }
                                    )
                                    Screen.Calendar -> CalendarScreen(viewModel = calendarViewModel)
                                    Screen.Analytics -> AnalyticsScreen(viewModel = analyticsViewModel)
                                    Screen.Notes -> NotesSection(viewModel = noteViewModel)
                                    Screen.Settings -> SettingsScreen(tagViewModel = tagViewModel)
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
private fun TaskFlowBottomNav(current: Screen, onSelect: (Screen) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val selected = current == item.screen
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = if (selected) com.taskflow.ui.components.TaskFlowAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onSelect(item.screen) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

/** What's selected inside the Lists overlay: the pinned Inbox tab, the pinned "All Lists"
 *  tab, or one of the closeable per-list detail tabs. */
private sealed class ListsTab {
    object Inbox : ListsTab()
    object AllLists : ListsTab()
    data class Detail(val list: TaskList) : ListsTab()
}

@Composable
private fun ListsSection(
    inboxViewModel: InboxViewModel,
    listViewModel: ListViewModel,
    taskRepository: TaskRepository,
    tagRepository: TagRepository
) {
    val lists by listViewModel.lists.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf<ListsTab>(ListsTab.AllLists) }
    var openTabs by remember { mutableStateOf(listOf<TaskList>()) }

    val currentSelected = selectedTab
    if (currentSelected is ListsTab.Detail && lists.none { it.id == currentSelected.list.id }) {
        // The open list was deleted elsewhere — fall back rather than point at nothing.
        selectedTab = ListsTab.AllLists
    }
    if (openTabs.any { openTab -> lists.none { it.id == openTab.id } }) {
        openTabs = openTabs.filter { openTab -> lists.any { it.id == openTab.id } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val allTabs: List<ListsTab> = listOf(ListsTab.Inbox, ListsTab.AllLists) + openTabs.map { ListsTab.Detail(it) }
        val selectedIndex = allTabs.indexOfFirst {
            when (it) {
                is ListsTab.Detail -> (selectedTab as? ListsTab.Detail)?.list?.id == it.list.id
                else -> it == selectedTab
            }
        }.coerceAtLeast(0)

        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 12.dp,
            // See note in a previous fix: the default indicator can index out of bounds
            // when a tab is added and selected in the same state update.
            indicator = {}
        ) {
            Tab(
                selected = selectedTab == ListsTab.Inbox,
                onClick = { selectedTab = ListsTab.Inbox },
                text = { Text("Inbox") }
            )
            Tab(
                selected = selectedTab == ListsTab.AllLists,
                onClick = { selectedTab = ListsTab.AllLists },
                text = { Text("All Lists") }
            )
            openTabs.forEach { list ->
                Tab(
                    selected = (selectedTab as? ListsTab.Detail)?.list?.id == list.id,
                    onClick = { selectedTab = ListsTab.Detail(list) },
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
                                        if ((selectedTab as? ListsTab.Detail)?.list?.id == list.id) {
                                            selectedTab = ListsTab.AllLists
                                        }
                                    }
                            )
                        }
                    }
                )
            }
        }

        when (val tab = selectedTab) {
            ListsTab.Inbox -> InboxScreen(viewModel = inboxViewModel, lists = lists)
            ListsTab.AllLists -> ListsScreen(
                viewModel = listViewModel,
                onOpenList = { list ->
                    if (openTabs.none { it.id == list.id }) openTabs = openTabs + list
                    selectedTab = ListsTab.Detail(list)
                }
            )
            is ListsTab.Detail -> {
                val listDetailViewModel: ListDetailViewModel = viewModel(
                    key = "list_detail_${tab.list.id}",
                    factory = ListDetailViewModel.provideFactory(taskRepository, tagRepository, tab.list.id)
                )
                ListDetailScreen(listName = tab.list.name, viewModel = listDetailViewModel)
            }
        }
    }
}

/** Push/pop between the notes list and a single open editor. */
@Composable
private fun NotesSection(viewModel: NoteViewModel) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    var selectedNoteId by remember { mutableStateOf<Long?>(null) }

    val selectedNote = notes.find { it.id == selectedNoteId }
    if (selectedNote == null) {
        NotesScreen(
            viewModel = viewModel,
            onCreateNote = { viewModel.createNote(onCreated = { id -> selectedNoteId = id }) },
            onOpenNote = { note -> selectedNoteId = note.id }
        )
    } else {
        NoteEditorScreen(
            note = selectedNote,
            viewModel = viewModel,
            onBack = { selectedNoteId = null }
        )
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
            onConfirm = { title, description, dueDate ->
                viewModel.updateTaskDetails(task, title, description, dueDate)
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEdit)
            ) {
                Text(
                    text = task.title,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
                TaskDueLabel(dueDate = task.dueDate, isCompleted = task.isCompleted)
            }

            IconButton(onClick = onTags) {
                Icon(Icons.Filled.Label, contentDescription = "Tags")
            }

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