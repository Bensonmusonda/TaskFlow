package com.taskflow.data.repository

import com.taskflow.data.dao.TagDao
import com.taskflow.data.entity.Tag
import kotlinx.coroutines.flow.Flow

class TagRepository(private val tagDao: TagDao) {

    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun getTagByName(name: String): Tag? = tagDao.getTagByName(name)

    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)
}