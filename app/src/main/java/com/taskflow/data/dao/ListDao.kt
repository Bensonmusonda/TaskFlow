package com.taskflow.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.taskflow.data.entity.TaskList
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertList(list: TaskList): Long

    @Update
    suspend fun updateList(list: TaskList)

    @Delete
    suspend fun deleteList(list: TaskList)

    @Query("SELECT * FROM lists ORDER BY name ASC")
    fun getAllLists(): Flow<List<TaskList>>

    @Query("SELECT * FROM lists WHERE id = :listId")
    suspend fun getListById(listId: Long): TaskList?
}
