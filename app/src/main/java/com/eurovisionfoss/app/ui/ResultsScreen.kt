package com.eurovisionfoss.app.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eurovisionfoss.app.CardSection
import com.eurovisionfoss.app.MainViewModel
import com.eurovisionfoss.app.RankedCountry
import com.eurovisionfoss.app.data.CountryHistory
import com.eurovisionfoss.app.util.ImageExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(viewModel: MainViewModel, onMenuClick: () -> Unit) {
    val ranked by viewModel.rankedCountries.collectAsState()
    val cardSections by viewModel.cardSections.collectAsState()
    val scored = ranked.filter { it.rank != null }
    val unscored = ranked.filter { it.rank == null }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSharePrompt by remember { mutableStateOf(false) }

    if (showSharePrompt) {
        SharePromptDialog(
            onTop12 = {
                showSharePrompt = false
                scope.launch {
                    val intent = ImageExporter.exportTop12(context, scored)
                    context.startActivity(android.content.Intent.createChooser(intent, "Share your Top 12"))
                }
            },
            onTop5 = {
                showSharePrompt = false
                scope.launch {
                    val intent = ImageExporter.exportTop5(context, scored)
                    context.startActivity(android.content.Intent.createChooser(intent, "Share your Top 5"))
                }
            },
            onDismiss = { showSharePrompt = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Points", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                },
                actions = {
                    if (scored.isNotEmpty()) {
                        TextButton(onClick = { showSharePrompt = true }) {
                            Text("\uD83D\uDCE4 Share", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (scored.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start scoring countries\non the voting screen",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(scored, key = { it.country.id }) { item ->
                    ScoredCountryRow(
                        item = item,
                        activeSection = cardSections[item.country.id] ?: CardSection.NONE,
                        history = viewModel.history[item.country.id],
                        onToggleSection = { sec -> viewModel.toggleCardSection(item.country.id, sec) }
                    )
                }

                if (unscored.isNotEmpty()) {
                    item {
                        Text(
                            text = "Not yet scored",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                    items(unscored, key = { it.country.id }) { item ->
                        UnscoredCountryRow(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun SharePromptDialog(
    onTop12: () -> Unit,
    onTop5: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share your ranking") },
        text = {
            Text("Choose a format to share on social media.\n\n" +
                 "Portrait (9:16) — Top 12 with Eurovision points, great for Stories.\n" +
                 "Square (1:1) — Top 5 snapshot, great for feeds.")
        },
        confirmButton = {
            Button(onClick = onTop12) { Text("Top 12 — Portrait") }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onTop5) { Text("Top 5 — Square") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun ScoredCountryRow(
    item: RankedCountry,
    activeSection: CardSection,
    history: CountryHistory?,
    onToggleSection: (CardSection) -> Unit
) {
    val badgeColor = pointsBadgeColor(item.eurovisionPoints)
    val hasNotes = item.country.let { c ->
        false // notes visible here are read-only; actual note check via score in ViewModel
    }
    val notes = "" // read from ranked country — would need to pass score separately; handled below

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = item.country.flag, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.country.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${item.rawTotal} / 30",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(badgeColor, shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item.eurovisionPoints > 0) "${item.eurovisionPoints} pts" else "0 pts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.eurovisionPoints >= 10) Color.Black else Color.White
                    )
                }
            }

            // Notes (read-only) / Stats toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = { onToggleSection(CardSection.NOTES) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "\uD83D\uDCDD Notes",
                        color = if (activeSection == CardSection.NOTES)
                            Color(0xFF00B4FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (activeSection == CardSection.NOTES) FontWeight.Bold else FontWeight.Normal
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
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (activeSection == CardSection.STATS) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            AnimatedVisibility(
                visible = activeSection == CardSection.NOTES,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                // Read-only notes view in Slist
                val noteText = item.country.id // placeholder; actual note passed via extra param below
                Text(
                    text = "Notes are editable from the country list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            AnimatedVisibility(
                visible = activeSection == CardSection.STATS,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ResultsStatsPanel(history = history)
            }
        }
    }
}

@Composable
private fun ResultsStatsPanel(history: CountryHistory?) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (history == null) {
            Text("No historical data.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        Surface(shape = MaterialTheme.shapes.small, tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCell2("Wins", "${history.wins}")
                StatCell2("Best", ordinal(history.bestPosition))
                StatCell2("Avg", ordinal(history.allTimeAvgFinish))
                StatCell2("Entries", "${history.participations}")
            }
        }
        val nonPartYears = mutableListOf<Int>()
        listOf("2021","2022","2023","2024","2025").forEach { year ->
            val pos = history.recent[year]
            if (pos == null) nonPartYears.add(year.toInt())
        }
        if (nonPartYears.isNotEmpty()) {
            Spacer(androidx.compose.ui.Modifier.padding(top = 4.dp))
            Text("* Did not participate in ${nonPartYears.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun StatCell2(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UnscoredCountryRow(item: RankedCountry) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).padding(start = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = item.country.flag, fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = item.country.name, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.weight(1f))
            Text(text = "\u2014", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
        }
    }
}

private fun pointsBadgeColor(points: Int): Color = when (points) {
    12   -> Color(0xFFFFD700)
    10   -> Color(0xFFB0BEC5)
    8    -> Color(0xFFCD7F32)
    7    -> Color(0xFF34C759)
    6    -> Color(0xFF30B0C7)
    5    -> Color(0xFF007AFF)
    4    -> Color(0xFF5856D6)
    3    -> Color(0xFFAF52DE)
    2    -> Color(0xFFFF9500)
    1    -> Color(0xFFFF3B30)
    else -> Color(0xFF636366)
}
