package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HomeworkEntity::class], version = 1, exportSchema = false)
abstract class HomeworkDatabase : RoomDatabase() {
    abstract fun homeworkDao(): HomeworkDao

    companion object {
        @Volatile
        private var INSTANCE: HomeworkDatabase? = null

        fun getDatabase(context: Context): HomeworkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HomeworkDatabase::class.java,
                    "homework_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
