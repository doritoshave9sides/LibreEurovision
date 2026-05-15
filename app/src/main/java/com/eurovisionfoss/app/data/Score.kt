package com.eurovisionfoss.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class Score(
    @PrimaryKey val countryId: String,
    val performance: Int,
    val song: Int,
    val sound: Int,
    val notes: String = ""
) {
    val total: Int get() = performance + song + sound
}
