package com.taskflow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A "Growth Domain" tag, e.g. #Coding, #3DModeling. */
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)
