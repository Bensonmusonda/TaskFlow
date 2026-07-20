package com.taskflow.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Not a persisted entity — a query-time projection joining a Task to its Tags
 * through the cross-ref table. This is what the Daily Activity Score calculation
 * (Section 4 of the spec) will query against later, without touching the core schema.
 */
data class TaskWithTags(
    @Embedded val task: Task,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TaskTagCrossRef::class,
            parentColumn = "taskId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>
)
