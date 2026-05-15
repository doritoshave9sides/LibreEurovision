package com.eurovisionfoss.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wiki_cache")
data class WikiCache(
    @PrimaryKey val countryId: String,
    val bio: String,
    val imageUrl: String,       // empty string if none found
    val fetchedAt: Long         // System.currentTimeMillis()
)
