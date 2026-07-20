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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskTagCrossRef(crossRef: TaskTagCrossRef)

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
}
