package com.taskflow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Spec calls this entity "List", but that name collides with kotlin.collections.List
 * everywhere it'd be imported alongside standard collections. Named `TaskList` instead;
 * the underlying table is still "lists" so the spec's data model isn't renamed, just the class.
 */
@Entity(tableName = "lists")
data class TaskList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String
)
