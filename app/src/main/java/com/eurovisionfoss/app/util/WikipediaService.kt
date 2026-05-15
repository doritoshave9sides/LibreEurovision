package com.eurovisionfoss.app.util

import com.eurovisionfoss.app.data.WikiCache
import com.eurovisionfoss.app.data.WikiCacheDao
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

// Image cache TTL: 7 days in milliseconds
private const val IMAGE_TTL_MS = 7L * 24 * 60 * 60 * 1000

// Stats/bio cache TTL: expires roughly July 2027
private val STATS_TTL_EXPIRY_MS: Long by lazy {
    java.util.Calendar.getInstance().apply {
        set(2027, java.util.Calendar.JULY, 1, 0, 0, 0)
    }.timeInMillis
}

data class WikiResult(
    val bio: String,
    val imageUrl: String,
    val fromCache: Boolean,
    val offline: Boolean = false
)

class WikipediaService(private val dao: WikiCacheDao) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "LibreEurovision/1.0 (Android; contact via app store)")
                    .build()
            )
        }
        .build()

    suspend fun fetch(countryId: String, artistName: String): WikiResult =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()

            // 1. Check cache
            val cached = dao.get(countryId)
            if (cached != null) {
                val imageExpired = (now - cached.fetchedAt) > IMAGE_TTL_MS
                val statsExpired = now > STATS_TTL_EXPIRY_MS
                if (!imageExpired && !statsExpired) {
                    return@withContext WikiResult(cached.bio, cached.imageUrl, fromCache = true)
                }
                // Image expired but stats still valid — keep bio, refresh image
                if (statsExpired) {
                    dao.deleteAll()
                } else if (imageExpired) {
                    // fall through to refresh
                }
            }

            // 2. Try network
            try {
                dao.deleteExpired(now - IMAGE_TTL_MS)

                // Step 1: direct title lookup
                var summaryBody: String? = client.newCall(
                    Request.Builder()
                        .url("https://en.wikipedia.org/api/rest_v1/page/summary/${
                            artistName.replace(" ", "_")
                        }")
                        .build()
                ).execute().use { response ->
                    when {
                        response.code == 404   -> null   // fall through to search
                        response.code == 403   -> null   // blocked without UA — fall through to search
                        !response.isSuccessful -> {
                            android.util.Log.e("WikiService", "HTTP ${response.code} for direct lookup of '$artistName'")
                            return@withContext offlineFallback(cached)
                        }
                        else                   -> response.body?.string()
                    }
                }

                // Treat disambiguation/set-index pages as not found — fall through to OpenSearch
                if (summaryBody != null) {
                    val testJson = JsonParser.parseString(summaryBody).asJsonObject
                    val extract = testJson.get("extract")?.asString ?: ""
                    val isDisambig = testJson.get("type")?.asString == "disambiguation" ||
                        extract.take(300).contains("may refer to:", ignoreCase = true)
                    if (isDisambig) summaryBody = null
                }

                // Step 2: OpenSearch fallback — finds the correct Wikipedia title
                if (summaryBody == null) {
                    val query = URLEncoder.encode(artistName, "UTF-8")
                    val foundTitle: String? = client.newCall(
                        Request.Builder()
                            .url("https://en.wikipedia.org/w/api.php" +
                                "?action=opensearch&search=$query&limit=1&namespace=0&format=json")
                            .build()
                    ).execute().use { response ->
                        if (!response.isSuccessful) {
                            android.util.Log.e("WikiService", "OpenSearch HTTP ${response.code} for '$artistName'")
                            return@use null
                        }
                        val searchBody = response.body?.string() ?: return@use null
                        // Response shape: ["query", ["Title1",...], ["Desc1",...], ["URL1",...]]
                        val arr = JsonParser.parseString(searchBody).asJsonArray
                        val titles = if (arr.size() > 1) arr.get(1).asJsonArray else null
                        if (titles != null && titles.size() > 0) titles.get(0).asString else null
                    }

                    if (foundTitle != null) {
                        summaryBody = client.newCall(
                            Request.Builder()
                                .url("https://en.wikipedia.org/api/rest_v1/page/summary/${
                                    foundTitle.replace(" ", "_")
                                }")
                                .build()
                        ).execute().use { resp ->
                            if (resp.isSuccessful) resp.body?.string() else null
                        }
                    }
                }

                // No article found via either route
                if (summaryBody == null)
                    return@withContext WikiResult("", "", fromCache = false, offline = false)

                val json = JsonParser.parseString(summaryBody).asJsonObject
                val rawBio = json.get("extract")?.asString ?: ""

                // OpenSearch may also land on a disambiguation or set-index page
                if (json.get("type")?.asString == "disambiguation" ||
                    rawBio.take(300).contains("may refer to:", ignoreCase = true))
                    return@withContext WikiResult("", "", fromCache = false, offline = false)
                val bio = if (rawBio.length <= 500) rawBio else {
                    val cut = rawBio.take(500)
                    val lastPunct = cut.lastIndexOfAny(charArrayOf('.', '!', '?'))
                    if (lastPunct > 0) cut.substring(0, lastPunct + 1) else cut
                }
                val imageUrl = json.getAsJsonObject("thumbnail")
                    ?.get("source")?.asString ?: ""

                val entry = WikiCache(countryId, bio, imageUrl, now)
                dao.upsert(entry)
                WikiResult(bio, imageUrl, fromCache = false)
            } catch (e: Exception) {
                android.util.Log.e("WikiService", "fetch failed for '$artistName': ${e::class.simpleName}: ${e.message}")
                offlineFallback(cached)
            }
        }

    private fun offlineFallback(cached: WikiCache?): WikiResult {
        return if (cached != null) {
            WikiResult(cached.bio, cached.imageUrl, fromCache = true, offline = false)
        } else {
            WikiResult("", "", fromCache = false, offline = true)
        }
    }
}
