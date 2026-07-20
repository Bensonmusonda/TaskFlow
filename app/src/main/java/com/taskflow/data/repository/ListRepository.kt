package com.taskflow.data.repository

import com.taskflow.data.dao.ListDao
import com.taskflow.data.entity.TaskList
import kotlinx.coroutines.flow.Flow

class ListRepository(private val listDao: ListDao) {

    fun getAllLists(): Flow<List<TaskList>> = listDao.getAllLists()

    suspend fun getListById(listId: Long): TaskList? = listDao.getListById(listId)

    suspend fun insertList(list: TaskList): Long = listDao.insertList(list)

    suspend fun updateList(list: TaskList) = listDao.updateList(list)

    suspend fun deleteList(list: TaskList) = listDao.deleteList(list)
}