package com.taskflow.data.repository

import com.taskflow.data.dao.TaskDao
import com.taskflow.data.entity.Task
import com.taskflow.data.entity.TaskTagCrossRef
import com.taskflow.data.entity.TaskWithTags
import com.taskflow.notifications.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val alarmScheduler: AlarmScheduler
) {

    fun getInboxTasks(): Flow<List<Task>> = taskDao.getInboxTasks()

    fun getTasksForList(listId: Long): Flow<List<Task>> = taskDao.getTasksForList(listId)

    fun getCompletedTasks(): Flow<List<Task>> = taskDao.getCompletedTasks()

    fun getTasksByTag(tagId: Long): Flow<List<Task>> = taskDao.getTasksByTag(tagId)

    suspend fun getTaskById(taskId: Long): Task? = taskDao.getTaskById(taskId)

    suspend fun getTaskWithTags(taskId: Long): TaskWithTags? = taskDao.getTaskWithTags(taskId)

    suspend fun attachTag(taskId: Long, tagId: Long) =
        taskDao.insertTaskTagCrossRef(TaskTagCrossRef(taskId = taskId, tagId = tagId))

    suspend fun detachTag(taskId: Long, tagId: Long) =
        taskDao.deleteTaskTagCrossRef(taskId, tagId)

    suspend fun insertTask(task: Task): Long {
        val id = taskDao.insertTask(task)
        if (!task.isCompleted && task.dueDate != null) {
            alarmScheduler.schedule(task.copy(id = id))
        }
        return id
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
        // Cancel any existing alarm for this task first — covers due date changing,
        // being cleared, or the task now being marked complete.
        alarmScheduler.cancel(task.id)
        if (!task.isCompleted && task.dueDate != null) {
            alarmScheduler.schedule(task)
        }
    }

    suspend fun deleteTask(task: Task) {
        alarmScheduler.cancel(task.id)
        taskDao.deleteTask(task)
    }

    /** Toggles completion, stamping/clearing completedAt to match, and (un)scheduling the reminder. */
    suspend fun setTaskCompleted(task: Task, completed: Boolean) {
        val updated = task.copy(
            isCompleted = completed,
            completedAt = if (completed) System.currentTimeMillis() else null
        )
        taskDao.updateTask(updated)
        if (completed) {
            alarmScheduler.cancel(task.id)
        } else if (updated.dueDate != null) {
            alarmScheduler.schedule(updated)
        }
    }

    /** Assigns a task to a list, or pass null to send it back to the Inbox. */
    suspend fun moveTaskToList(task: Task, listId: Long?) {
        taskDao.updateTask(task.copy(listId = listId))
        // Completion/due-date are unaffected by this — no alarm change needed.
    }

    /** Journaling Mode: add a task that's already done, with its tags, in one go. Never
     *  schedules a reminder — the task is already complete. */
    suspend fun insertRetrospectiveEntry(task: Task, tagIds: List<Long>? = null): Long =
        taskDao.insertRetrospectiveEntry(task, tagIds)

    suspend fun getCompletedTasksWithTagsInRange(startMillis: Long, endMillis: Long): List<TaskWithTags> =
        taskDao.getCompletedTasksWithTagsInRange(startMillis, endMillis)

    fun observeCompletedTasksWithTags(): Flow<List<TaskWithTags>> = taskDao.observeCompletedTasksWithTags()

    /** Every incomplete task with a future due date — used to re-arm alarms after a reboot. */
    suspend fun getTasksWithUpcomingReminders(): List<Task> =
        taskDao.getTasksWithFutureDueDate(System.currentTimeMillis())
}