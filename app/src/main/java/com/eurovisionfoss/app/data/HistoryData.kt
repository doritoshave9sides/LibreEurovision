package com.eurovisionfoss.app.data

data class CountryHistory(
    val wins: Int,
    val participations: Int,
    val bestPosition: Int,
    val allTimeAvgFinish: Int,
    val recent: Map<String, Int?>,          // year -> position (null = did not participate)
    val nonParticipatingYears: List<Int>
)
