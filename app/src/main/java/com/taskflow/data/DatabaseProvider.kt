package com.taskflow.data

import android.content.Context
import androidx.room.Room

/**
 * Simple manual singleton for the Room database. No DI framework assumed —
 * if you add Hilt later, this becomes a one-line @Provides function instead.
 */
object DatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            ).build().also { instance = it }
        }
    }
}
