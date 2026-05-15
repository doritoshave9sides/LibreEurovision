package com.eurovisionfoss.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM scores")
    fun getAllScores(): Flow<List<Score>>

    @Upsert
    suspend fun upsert(score: Score)

    @Query("DELETE FROM scores")
    suspend fun deleteAll()

    @Query("UPDATE scores SET notes = :notes WHERE countryId = :countryId")
    suspend fun updateNotes(countryId: String, notes: String)
}
