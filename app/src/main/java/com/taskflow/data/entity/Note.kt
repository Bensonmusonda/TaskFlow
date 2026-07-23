package com.taskflow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A standalone markdown note — the "secondary module" mentioned in the tech spec's
 *  Future-Proofing section. Deliberately not tied to Task/List; it's its own thing. */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val contentMarkdown: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)