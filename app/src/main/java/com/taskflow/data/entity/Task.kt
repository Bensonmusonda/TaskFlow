package com.taskflow.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Core task entity. Backs both "list-based" tasks and Inbox (unscheduled) tasks,
 * where `listId == null` signals Inbox membership.
 *
 * Timestamps are stored as epoch millis (Long) to keep the schema simple and
 * avoid pulling in a date/time TypeConverter for v1.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("listId")],
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val listId: Long? = null,
)
