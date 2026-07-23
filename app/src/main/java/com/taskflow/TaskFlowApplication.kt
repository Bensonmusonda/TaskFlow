package com.taskflow

import android.app.Application
import com.taskflow.data.DatabaseProvider
import com.taskflow.data.repository.ListRepository
import com.taskflow.data.repository.NoteRepository
import com.taskflow.data.repository.TagRepository
import com.taskflow.data.repository.TaskRepository
import com.taskflow.notifications.AlarmScheduler
import com.taskflow.notifications.NotificationHelper

/**
 * Holds the single AppDatabase instance and the repositories built on top of it.
 * No DI framework yet — if you add Hilt later, this class goes away and these
 * become @Provides/@Singleton bindings instead.
 */
class TaskFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }

    private val database by lazy { DatabaseProvider.getDatabase(this) }
    private val alarmScheduler by lazy { AlarmScheduler(this) }

    val taskRepository by lazy { TaskRepository(database.taskDao(), alarmScheduler) }
    val listRepository by lazy { ListRepository(database.listDao()) }
    val tagRepository by lazy { TagRepository(database.tagDao()) }
    val noteRepository by lazy { NoteRepository(database.noteDao()) }

    /** Re-arms alarms for every incomplete task with a future due date — called after boot. */
    suspend fun rescheduleAllPendingReminders() {
        taskRepository.getTasksWithUpcomingReminders().forEach { alarmScheduler.schedule(it) }
    }
}