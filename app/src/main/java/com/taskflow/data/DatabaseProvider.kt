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
            )
                // Dev-stage only: wipes local data on a version bump instead of crashing
                // when no Migration is defined. Remove this and write real Migrations
                // before this app has real user data to preserve.
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}