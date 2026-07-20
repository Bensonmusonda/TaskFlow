package com.taskflow.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.taskflow.data.dao.ListDao
import com.taskflow.data.dao.TagDao
import com.taskflow.data.dao.TaskDao
import com.taskflow.data.entity.Tag
import com.taskflow.data.entity.Task
import com.taskflow.data.entity.TaskList
import com.taskflow.data.entity.TaskTagCrossRef

@Database(
    entities = [Task::class, TaskList::class, Tag::class, TaskTagCrossRef::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun listDao(): ListDao
    abstract fun tagDao(): TagDao

    companion object {
        const val DATABASE_NAME = "taskflow.db"
    }
}
