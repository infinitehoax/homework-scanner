package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "homework_solutions")
data class HomeworkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val questionText: String,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val solutionText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val historyJson: String = "[]" // To store subsequent follow-up chats if any
)
