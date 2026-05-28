package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homework_solutions ORDER BY timestamp DESC")
    fun getAllSolutions(): Flow<List<HomeworkEntity>>

    @Query("SELECT * FROM homework_solutions WHERE id = :id LIMIT 1")
    suspend fun getSolutionById(id: Int): HomeworkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSolution(entity: HomeworkEntity): Long

    @Update
    suspend fun updateSolution(entity: HomeworkEntity)

    @Delete
    suspend fun deleteSolution(entity: HomeworkEntity)

    @Query("DELETE FROM homework_solutions WHERE id = :id")
    suspend fun deleteSolutionById(id: Int)
}
