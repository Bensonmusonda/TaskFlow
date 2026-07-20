package com.taskflow.data.repository

import com.taskflow.data.dao.TagDao
import com.taskflow.data.entity.Tag
import kotlinx.coroutines.flow.Flow

class TagRepository(private val tagDao: TagDao) {

    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun getTagByName(name: String): Tag? = tagDao.getTagByName(name)

    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)

    /**
     * Looks up a tag by name, creating it if it doesn't exist yet. Relies on the unique
     * index on Tag.name + insertTag's IGNORE conflict strategy: if the insert is ignored
     * (name already exists), it returns -1 and we fall back to a lookup.
     */
    suspend fun getOrCreateTag(name: String): Tag {
        val trimmed = name.trim()
        val insertedId = tagDao.insertTag(Tag(name = trimmed))
        return if (insertedId != -1L) {
            Tag(id = insertedId, name = trimmed)
        } else {
            tagDao.getTagByName(trimmed)
                ?: throw IllegalStateException("Tag insert ignored but no existing tag found for '$trimmed'")
        }
    }

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)
}