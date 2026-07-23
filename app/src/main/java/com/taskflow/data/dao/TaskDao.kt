package com.taskflow.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.taskflow.data.entity.Task
import com.taskflow.data.entity.TaskTagCrossRef
import com.taskflow.data.entity.TaskWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?

    @Query("SELECT * FROM tasks WHERE listId IS NULL ORDER BY createdAt DESC")
    fun getInboxTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY createdAt DESC")
    fun getTasksForList(listId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskTagCrossRef(crossRef: TaskTagCrossRef)

    @Query("DELETE FROM task_tags WHERE taskId = :taskId AND tagId = :tagId")
    suspend fun deleteTaskTagCrossRef(taskId: Long, tagId: Long)

    @Query(
        """
        SELECT tasks.* FROM tasks
        INNER JOIN task_tags ON tasks.id = task_tags.taskId
        WHERE task_tags.tagId = :tagId
        ORDER BY tasks.createdAt DESC
        """
    )
    fun getTasksByTag(tagId: Long): Flow<List<Task>>

    /**
     * Journaling Mode / "Retrospective Entry": persists a task that is already done,
     * bypassing the normal open -> complete flow, plus its tag associations, in one
     * transaction. `isCompleted` and `completedAt` are enforced here rather than trusted
     * from the caller, so a retrospective entry can never accidentally land as an open task.
     */
    @Transaction
    suspend fun insertRetrospectiveEntry(task: Task, tagIds: List<Long>? = null): Long {
        val completedTask = task.copy(
            isCompleted = true,
            completedAt = task.completedAt ?: System.currentTimeMillis()
        )
        val taskId = insertTask(completedTask)
        tagIds?.forEach { tagId ->
            insertTaskTagCrossRef(TaskTagCrossRef(taskId = taskId, tagId = tagId))
        }
        return taskId
    }

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskWithTags(taskId: Long): TaskWithTags?

    @Transaction
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND completedAt BETWEEN :startMillis AND :endMillis")
    suspend fun getCompletedTasksWithTagsInRange(startMillis: Long, endMillis: Long): List<TaskWithTags>

    /**
     * Reactive version for the Analytics heatmap — recomposes automatically when tasks
     * complete/uncomplete or their tag associations change (Room tracks the junction
     * table too, since it's part of the @Transaction query).
     */
    @Transaction
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND completedAt IS NOT NULL")
    fun observeCompletedTasksWithTags(): Flow<List<TaskWithTags>>

    /** Backs alarm rescheduling after a device reboot (AlarmManager alarms don't survive it). */
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate > :nowMillis")
    suspend fun getTasksWithFutureDueDate(nowMillis: Long): List<Task>
}