package com.taskflow.data.repository

import com.taskflow.data.dao.TaskDao
import com.taskflow.data.entity.Task
import com.taskflow.data.entity.TaskWithTags
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getInboxTasks(): Flow<List<Task>> = taskDao.getInboxTasks()

    fun getTasksForList(listId: Long): Flow<List<Task>> = taskDao.getTasksForList(listId)

    suspend fun getTaskById(taskId: Long): Task? = taskDao.getTaskById(taskId)

    suspend fun getTaskWithTags(taskId: Long): TaskWithTags? = taskDao.getTaskWithTags(taskId)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    /** Toggles completion, stamping/clearing completedAt to match. */
    suspend fun setTaskCompleted(task: Task, completed: Boolean) {
        val updated = task.copy(
            isCompleted = completed,
            completedAt = if (completed) System.currentTimeMillis() else null
        )
        taskDao.updateTask(updated)
    }

    /** Assigns a task to a list, or pass null to send it back to the Inbox. */
    suspend fun moveTaskToList(task: Task, listId: Long?) {
        taskDao.updateTask(task.copy(listId = listId))
    }

    /** Journaling Mode: add a task that's already done, with its tags, in one go. */
    suspend fun insertRetrospectiveEntry(task: Task, tagIds: List<Long>? = null): Long =
        taskDao.insertRetrospectiveEntry(task, tagIds)

    suspend fun getCompletedTasksWithTagsInRange(startMillis: Long, endMillis: Long): List<TaskWithTags> =
        taskDao.getCompletedTasksWithTagsInRange(startMillis, endMillis)
}