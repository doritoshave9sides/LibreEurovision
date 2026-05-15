package com.eurovisionfoss.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eurovisionfoss.app.data.ALL_COUNTRIES
import com.eurovisionfoss.app.data.COMPETING_COUNTRIES
import com.eurovisionfoss.app.data.AppDatabase
import com.eurovisionfoss.app.data.Country
import com.eurovisionfoss.app.data.CountryHistory
import com.eurovisionfoss.app.data.Score
import com.eurovisionfoss.app.util.HistoryLoader
import com.eurovisionfoss.app.util.ImportResult
import com.eurovisionfoss.app.util.UnknownImport
import com.eurovisionfoss.app.util.WikiResult
import com.eurovisionfoss.app.util.WikipediaService
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Cycles: ALPHABETICAL -> RUNNING_ORDER -> TOTAL_WINS -> ALPHABETICAL
enum class SortMode { ALPHABETICAL, RUNNING_ORDER, TOTAL_WINS }

data class RankedCountry(
    val rank: Int?,
    val country: Country,
    val rawTotal: Int,
    val eurovisionPoints: Int
)

enum class CardSection { NONE, NOTES, STATS }

data class WikiState(
    val loading: Boolean = false,
    val result: WikiResult? = null
)

private val EUROVISION_POINTS = listOf(12, 10, 8, 7, 6, 5, 4, 3, 2, 1)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.scoreDao()
    private val wikiService = WikipediaService(db.wikiCacheDao())

    val history: Map<String, CountryHistory> by lazy { HistoryLoader.load(application) }
    val worldIdToName: Map<String, String> by lazy { loadWorldCountries(application) }

    private val _sortMode = MutableStateFlow(SortMode.ALPHABETICAL)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _expandedCountryId = MutableStateFlow<String?>(null)
    val expandedCountryId: StateFlow<String?> = _expandedCountryId.asStateFlow()

    private val _cardSections = MutableStateFlow<Map<String, CardSection>>(emptyMap())
    val cardSections: StateFlow<Map<String, CardSection>> = _cardSections.asStateFlow()

    private val _hostCityVisible = MutableStateFlow(false)
    val hostCityVisible: StateFlow<Boolean> = _hostCityVisible.asStateFlow()

    private val _wikiStates = MutableStateFlow<Map<String, WikiState>>(emptyMap())
    val wikiStates: StateFlow<Map<String, WikiState>> = _wikiStates.asStateFlow()

    private val _unknownImports = MutableStateFlow<List<UnknownImport>>(emptyList())
    val unknownImports: StateFlow<List<UnknownImport>> = _unknownImports.asStateFlow()

    val scores: StateFlow<Map<String, Score>> = dao.getAllScores()
        .map { list -> list.associateBy { it.countryId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val sortedCountries: StateFlow<List<Country>> = _sortMode
        .map { mode ->
            when (mode) {
                SortMode.ALPHABETICAL  -> COMPETING_COUNTRIES.sortedBy { it.name }
                SortMode.RUNNING_ORDER -> COMPETING_COUNTRIES.sortedBy { it.runningOrder }
                SortMode.TOTAL_WINS    -> COMPETING_COUNTRIES.sortedByDescending { history[it.id]?.wins ?: 0 }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            COMPETING_COUNTRIES.sortedBy { it.name }
        )

    val rankedCountries: StateFlow<List<RankedCountry>> = scores
        .map { computeRankings(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cycleSort() {
        _sortMode.value = when (_sortMode.value) {
            SortMode.ALPHABETICAL  -> SortMode.RUNNING_ORDER
            SortMode.RUNNING_ORDER -> SortMode.TOTAL_WINS
            SortMode.TOTAL_WINS    -> SortMode.ALPHABETICAL
        }
    }

    fun toggleExpanded(countryId: String) {
        val current = _expandedCountryId.value
        _expandedCountryId.value = if (current == countryId) null else countryId
        if (_expandedCountryId.value == countryId) fetchWiki(countryId)
    }

    fun toggleCardSection(countryId: String, section: CardSection) {
        val current = _cardSections.value[countryId] ?: CardSection.NONE
        _cardSections.value = _cardSections.value + (countryId to if (current == section) CardSection.NONE else section)
    }

    fun toggleHostCity() {
        _hostCityVisible.value = !_hostCityVisible.value
    }

    fun updateScore(countryId: String, performance: Int, song: Int, sound: Int) {
        viewModelScope.launch {
            val notes = scores.value[countryId]?.notes ?: ""
            dao.upsert(Score(countryId, performance, song, sound, notes))
        }
    }

    fun updateNotes(countryId: String, notes: String) {
        viewModelScope.launch {
            val existing = scores.value[countryId]
            if (existing != null) {
                dao.updateNotes(countryId, notes.take(250))
            } else {
                dao.upsert(Score(countryId, 5, 5, 5, notes.take(250)))
            }
        }
    }

    fun applyImport(result: ImportResult, allScores: List<Score>) {
        viewModelScope.launch {
            dao.deleteAll()
            allScores.forEach { dao.upsert(it) }
            _unknownImports.value = result.unknown
        }
    }

    fun clearUnknownImports() { _unknownImports.value = emptyList() }

    private fun fetchWiki(countryId: String) {
        val country = ALL_COUNTRIES.find { it.id == countryId } ?: return
        // Skip fetch for boycotting/withdrawn countries — no artist to look up
        if (country.artist == "Withdrew" || country.artist == "TBA") return
        if (_wikiStates.value[countryId]?.loading == true) return
        _wikiStates.value = _wikiStates.value + (countryId to WikiState(loading = true))
        viewModelScope.launch {
            val result = wikiService.fetch(countryId, country.artist)
            _wikiStates.value = _wikiStates.value + (countryId to WikiState(loading = false, result = result))
        }
    }

    private fun loadWorldCountries(context: Context): Map<String, String> {
        return runCatching {
            val json = context.assets.open("world_countries.json").bufferedReader().use { it.readText() }
            val arr = JsonParser.parseString(json).asJsonObject.getAsJsonArray("countries")
            arr.associate { el ->
                val obj = el.asJsonObject
                obj.get("id").asString to obj.get("name").asString
            }
        }.getOrDefault(emptyMap())
    }

    private fun computeRankings(scoresMap: Map<String, Score>): List<RankedCountry> {
        val scored = COMPETING_COUNTRIES
            .mapNotNull { country -> scoresMap[country.id]?.let { Pair(country, it.total) } }
            .sortedByDescending { it.second }
        val unscored = COMPETING_COUNTRIES.filter { it.id !in scoresMap }.sortedBy { it.name }

        val result = mutableListOf<RankedCountry>()
        var currentRank = 1; var pointsIndex = 0; var i = 0
        while (i < scored.size) {
            val currentTotal = scored[i].second
            var j = i
            while (j < scored.size && scored[j].second == currentTotal) j++
            val groupSize = j - i
            val pts = if (pointsIndex < EUROVISION_POINTS.size) EUROVISION_POINTS[pointsIndex] else 0
            for (k in i until j) result.add(RankedCountry(currentRank, scored[k].first, scored[k].second, pts))
            currentRank += groupSize; pointsIndex += groupSize; i = j
        }
        unscored.forEach { result.add(RankedCountry(null, it, 0, 0)) }
        return result
    }
}
