package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HomeworkEntity::class], version = 3, exportSchema = false)
abstract class HomeworkDatabase : RoomDatabase() {
    abstract fun homeworkDao(): HomeworkDao

    companion object {
        @Volatile
        private var INSTANCE: HomeworkDatabase? = null
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE homework_solutions ADD COLUMN folderName TEXT")
            }
        }

        fun getDatabase(context: Context): HomeworkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HomeworkDatabase::class.java,
                    "homework_database"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
