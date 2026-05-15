package com.eurovisionfoss.app.util

import android.content.Context
import com.eurovisionfoss.app.data.CountryHistory
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

object HistoryLoader {
    private var cache: Map<String, CountryHistory>? = null

    fun load(context: Context): Map<String, CountryHistory> {
        cache?.let { return it }
        val json = context.assets.open("history.json")
            .bufferedReader().use { it.readText() }
        val root = Gson().fromJson(json, JsonObject::class.java)
        val result = mutableMapOf<String, CountryHistory>()
        for ((key, value) in root.entrySet()) {
            if (key.startsWith("_")) continue
            val obj = value.asJsonObject
            val recentMap = mutableMapOf<String, Int?>()
            obj.getAsJsonObject("recent")?.entrySet()?.forEach { (year, pos) ->
                recentMap[year] = if (pos.isJsonNull) null else pos.asInt
            }
            val nonPart = mutableListOf<Int>()
            obj.getAsJsonArray("nonParticipatingYears")?.forEach { nonPart.add(it.asInt) }
            result[key] = CountryHistory(
                wins = obj.get("wins").asInt,
                participations = obj.get("participations").asInt,
                bestPosition = obj.get("bestPosition").asInt,
                allTimeAvgFinish = obj.get("allTimeAvgFinish").asInt,
                recent = recentMap,
                nonParticipatingYears = nonPart
            )
        }
        cache = result
        return result
    }
}
