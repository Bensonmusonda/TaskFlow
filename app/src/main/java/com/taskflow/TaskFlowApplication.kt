package com.taskflow

import android.app.Application
import com.taskflow.data.DatabaseProvider
import com.taskflow.data.repository.ListRepository
import com.taskflow.data.repository.TagRepository
import com.taskflow.data.repository.TaskRepository

/**
 * Holds the single AppDatabase instance and the repositories built on top of it.
 * No DI framework yet — if you add Hilt later, this class goes away and these
 * become @Provides/@Singleton bindings instead.
 */
class TaskFlowApplication : Application() {

    private val database by lazy { DatabaseProvider.getDatabase(this) }

    val taskRepository by lazy { TaskRepository(database.taskDao()) }
    val listRepository by lazy { ListRepository(database.listDao()) }
    val tagRepository by lazy { TagRepository(database.tagDao()) }
}