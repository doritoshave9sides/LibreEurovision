package com.eurovisionfoss.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eurovisionfoss.app.CardSection
import com.eurovisionfoss.app.MainViewModel
import com.eurovisionfoss.app.SortMode
import com.eurovisionfoss.app.WikiState
import com.eurovisionfoss.app.data.Country
import com.eurovisionfoss.app.data.CountryHistory
import com.eurovisionfoss.app.data.Score
import kotlin.math.roundToInt

private val NeonBlue = Color(0xFF00B4FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(viewModel: MainViewModel, onMenuClick: () -> Unit) {
    val sortedCountries by viewModel.sortedCountries.collectAsState()
    val scores by viewModel.scores.collectAsState()
    val expandedCountryId by viewModel.expandedCountryId.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val cardSections by viewModel.cardSections.collectAsState()
    val hostCityVisible by viewModel.hostCityVisible.collectAsState()
    val wikiStates by viewModel.wikiStates.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Libre Eurovision\uD83E\uDD29", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.toggleHostCity() }) {
                        Text("\uD83D\uDD0D", fontSize = 20.sp)
                    }
                    TextButton(onClick = { viewModel.cycleSort() }) {
                        Text(
                            text = when (sortMode) {
                                SortMode.ALPHABETICAL  -> "A-Z"
                                SortMode.RUNNING_ORDER -> "#"
                                SortMode.TOTAL_WINS    -> "\uD83C\uDFC6"
                            },
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                HostCityPanel(visible = hostCityVisible)
                if (hostCityVisible) Spacer(Modifier.height(4.dp))
            }

            items(sortedCountries, key = { it.id }) { country ->
                CountryRow(
                    country = country,
                    score = scores[country.id],
                    isExpanded = expandedCountryId == country.id,
                    activeSection = cardSections[country.id] ?: CardSection.NONE,
                    wikiState = wikiStates[country.id],
                    history = viewModel.history[country.id],
                    onToggle = { viewModel.toggleExpanded(country.id) },
                    onToggleSection = { sec -> viewModel.toggleCardSection(country.id, sec) },
                    onScoreChange = { perf, song, sound ->
                        viewModel.updateScore(country.id, perf, song, sound)
                    },
                    onNotesChange = { notes -> viewModel.updateNotes(country.id, notes) }
                )
            }
        }
    }
}

@Composable
private fun CountryRow(
    country: Country,
    score: Score?,
    isExpanded: Boolean,
    activeSection: CardSection,
    wikiState: WikiState?,
    history: CountryHistory?,
    onToggle: () -> Unit,
    onToggleSection: (CardSection) -> Unit,
    onScoreChange: (Int, Int, Int) -> Unit,
    onNotesChange: (String) -> Unit
) {
    val hasNotes = !score?.notes.isNullOrBlank()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = country.flag, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = country.name, style = MaterialTheme.typography.titleMedium)
                    when {
                        country.artist == "Withdrew" -> Text(
                            text = "Withdrew \u2014 Boycott 2026",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9500)
                        )
                        country.artist != "TBA" -> Text(
                            text = "${country.artist} \u2014 ${country.song}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (score != null) {
                    Text(
                        text = "${score.total}/30",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    WikiBioSection(country, wikiState)
                    Spacer(Modifier.height(12.dp))

                    ScoreSliders(initialScore = score, onScoreChange = onScoreChange)

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))

                    // Notes / Stats toggle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { onToggleSection(CardSection.NOTES) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "\uD83D\uDCDD Notes",
                                color = if (activeSection == CardSection.NOTES || hasNotes) NeonBlue
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (activeSection == CardSection.NOTES) FontWeight.Bold
                                             else FontWeight.Normal
                            )
                        }
                        TextButton(
                            onClick = { onToggleSection(CardSection.STATS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "\uD83D\uDCCA Stats",
                                color = if (activeSection == CardSection.STATS)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (activeSection == CardSection.STATS) FontWeight.Bold
                                             else FontWeight.Normal
                            )
                        }
                    }

                    AnimatedVisibility(visible = activeSection == CardSection.NOTES) {
                        NotesEditPanel(notes = score?.notes ?: "", onNotesChange = onNotesChange)
                    }
                    AnimatedVisibility(visible = activeSection == CardSection.STATS) {
                        StatsPanel(history = history)
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun WikiBioSection(country: Country, wikiState: WikiState?) {
    val isWithdrew = country.artist == "Withdrew"
    val artistText = if (country.artist == "TBA" || isWithdrew) country.name else country.artist
    val imageUrl = wikiState?.result?.imageUrl

    Column {
        // Artist name + song row (spinner while loading)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(artistText, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium)
                if (!isWithdrew && country.song != "TBA") {
                    Text("\u201c${country.song}\u201d",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (wikiState?.loading == true) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Bio / status text
        if (isWithdrew) {
            Text(
                "${country.name} withdrew from Eurovision 2026 in protest. No artist or song was selected.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF9500)
            )
        } else {
            val bio = wikiState?.result?.bio
            when {
                wikiState == null || wikiState.loading -> { /* spinner shown above; nothing here */ }
                wikiState.result?.offline == true && bio.isNullOrBlank() ->
                    Text("Could not load bio \u2014 check your connection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                bio.isNullOrBlank() ->
                    Text("No bio available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    var bioExpanded by remember { mutableStateOf(false) }
                    var overflowed by remember(bioExpanded) { mutableStateOf(false) }
                    val boxColor = MaterialTheme.colorScheme.surfaceVariant

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { bioExpanded = !bioExpanded },
                        shape = MaterialTheme.shapes.small,
                        color = boxColor
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Box {
                                Text(
                                    text = bio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (bioExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Clip,
                                    onTextLayout = { overflowed = it.hasVisualOverflow }
                                )
                                if (!bioExpanded && overflowed) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    0.4f to Color.Transparent,
                                                    1f to boxColor
                                                )
                                            )
                                    )
                                }
                            }
                            if (bioExpanded && !imageUrl.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageUrl)
                                        .addHeader("User-Agent", "LibreEurovision/1.0 (Android; contact via app store)")
                                        .build(),
                                    contentDescription = "$artistText photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesEditPanel(notes: String, onNotesChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        OutlinedTextField(
            value = notes,
            onValueChange = { if (it.length <= 250) onNotesChange(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your notes") },
            placeholder = { Text("Amazing staging, total banger...") },
            supportingText = { Text("${notes.length} / 250") },
            maxLines = 4
        )
    }
}

@Composable
private fun StatsPanel(history: CountryHistory?) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        if (history == null) {
            Text("No historical data available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }

        Surface(shape = MaterialTheme.shapes.small, tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCell("Wins", "${history.wins}")
                StatCell("Best", ordinal(history.bestPosition))
                StatCell("Avg", ordinal(history.allTimeAvgFinish))
                StatCell("Entries", "${history.participations}")
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("Last 5 years", fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))

        val nonPartYears = mutableListOf<Int>()
        listOf("2021", "2022", "2023", "2024", "2025").forEach { year ->
            val pos = history.recent[year]
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(year, modifier = Modifier.width(44.dp),
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                if (pos == null) {
                    nonPartYears.add(year.toInt())
                    Text("*Did not participate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                } else {
                    Text(ordinal(pos), style = MaterialTheme.typography.bodySmall,
                        color = when {
                            pos == 1  -> Color(0xFFFFD700)
                            pos <= 3  -> Color(0xFF34C759)
                            pos <= 10 -> MaterialTheme.colorScheme.onSurface
                            else      -> MaterialTheme.colorScheme.onSurfaceVariant
                        })
                }
            }
        }

        if (nonPartYears.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("* Country did not participate in ${nonPartYears.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ScoreSliders(initialScore: Score?, onScoreChange: (Int, Int, Int) -> Unit) {
    var performance by remember { mutableStateOf((initialScore?.performance ?: 5).toFloat()) }
    var song by remember { mutableStateOf((initialScore?.song ?: 5).toFloat()) }
    var sound by remember { mutableStateOf((initialScore?.sound ?: 5).toFloat()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        ScoreSlider("Performance", performance, { performance = it }) {
            onScoreChange(performance.roundToInt(), song.roundToInt(), sound.roundToInt())
        }
        ScoreSlider("Song", song, { song = it }) {
            onScoreChange(performance.roundToInt(), song.roundToInt(), sound.roundToInt())
        }
        ScoreSlider("Sound", sound, { sound = it }) {
            onScoreChange(performance.roundToInt(), song.roundToInt(), sound.roundToInt())
        }
    }
}

@Composable
private fun ScoreSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val intValue = value.roundToInt()
    val color = scoreColor(intValue)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp))
        Slider(
            value = value, onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 1f..10f, steps = 8,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color, activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.24f)
            )
        )
        Text(
            text = if (intValue == 10) "\u2605 10" else "$intValue",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (intValue == 10) FontWeight.Bold else FontWeight.Normal,
            color = color, modifier = Modifier.width(40.dp), textAlign = TextAlign.End
        )
    }
}

fun scoreColor(value: Int): Color {
    val red    = Color(0xFFFF3B30)
    val orange = Color(0xFFFF9500)
    val green  = Color(0xFF34C759)
    val gold   = Color(0xFFFFD700)
    return when {
        value <= 1 -> red
        value < 5  -> lerp(red, orange, (value - 1f) / 4f)
        value == 5 -> orange
        value < 9  -> lerp(orange, green, (value - 5f) / 4f)
        value == 9 -> green
        else       -> gold
    }
}

fun ordinal(n: Int): String {
    if (n <= 0) return "\u2014"
    val suffix = when {
        n in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else         -> "th"
    }
    return "$n$suffix"
}
