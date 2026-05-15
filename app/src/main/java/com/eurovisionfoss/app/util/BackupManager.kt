package com.eurovisionfoss.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.eurovisionfoss.app.data.Score
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportResult(
    val imported: Int,
    val unknown: List<UnknownImport>
)

data class UnknownImport(
    val countryId: String,
    val countryName: String,
    val performance: Int,
    val song: Int,
    val sound: Int,
    val notes: String
)

object BackupManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    // Returns an Intent ready to pass to startActivity / share sheet
    suspend fun export(
        context: Context,
        scores: List<Score>,
        knownIds: Set<String>
    ): Intent = withContext(Dispatchers.IO) {
        val arr = com.google.gson.JsonArray()
        scores.forEach { s ->
            val obj = JsonObject()
            obj.addProperty("countryId", s.countryId)
            obj.addProperty("performance", s.performance)
            obj.addProperty("song", s.song)
            obj.addProperty("sound", s.sound)
            obj.addProperty("notes", s.notes)
            arr.add(obj)
        }
        val root = JsonObject()
        root.addProperty("version", "0.0.2")
        root.addProperty("app", "Libre Eurovision")
        root.addProperty("exported", isoFmt.format(Date()))
        root.add("scores", arr)

        val file = File(context.cacheDir, "libre_eurovision_backup.json")
        file.writeText(gson.toJson(root))

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Libre Eurovision Backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // knownIds = all current Eurovision country IDs in the app
    // worldIds = all world country IDs from world_countries.json (for forward compat)
    // Returns list of known scores to upsert + unknown imports to surface in UI
    suspend fun parseImport(
        context: Context,
        uri: android.net.Uri,
        knownIds: Set<String>,
        worldIdToName: Map<String, String>
    ): ImportResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.use { it.readText() }
            ?: return@withContext ImportResult(0, emptyList())

        val root = runCatching { JsonParser.parseString(text).asJsonObject }
            .getOrNull() ?: return@withContext ImportResult(0, emptyList())

        val scoresArr = root.getAsJsonArray("scores")
            ?: return@withContext ImportResult(0, emptyList())

        val knownScores = mutableListOf<Score>()
        val unknowns = mutableListOf<UnknownImport>()

        scoresArr.forEach { el ->
            val obj = el.asJsonObject
            val id = obj.get("countryId")?.asString ?: return@forEach
            val perf = obj.get("performance")?.asInt ?: 5
            val song = obj.get("song")?.asInt ?: 5
            val sound = obj.get("sound")?.asInt ?: 5
            val notes = obj.get("notes")?.asString ?: ""

            if (id in knownIds) {
                knownScores.add(Score(id, perf, song, sound, notes))
            } else {
                // Unknown: could be a future country, a typo, or a new entrant
                val name = worldIdToName[id] ?: id
                unknowns.add(UnknownImport(id, name, perf, song, sound, notes))
            }
        }

        ImportResult(knownScores.size, unknowns)
    }
}
