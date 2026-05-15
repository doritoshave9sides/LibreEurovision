package com.eurovisionfoss.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface WikiCacheDao {
    @Query("SELECT * FROM wiki_cache WHERE countryId = :countryId")
    suspend fun get(countryId: String): WikiCache?

    @Upsert
    suspend fun upsert(entry: WikiCache)

    // Delete entries older than the given timestamp (for 7-day TTL)
    @Query("DELETE FROM wiki_cache WHERE fetchedAt < :cutoff")
    suspend fun deleteExpired(cutoff: Long)

    // Clear all — called when stats cache TTL (July 2027) expires
    @Query("DELETE FROM wiki_cache")
    suspend fun deleteAll()
}
