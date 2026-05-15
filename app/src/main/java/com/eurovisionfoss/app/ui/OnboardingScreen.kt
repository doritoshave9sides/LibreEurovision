package com.eurovisionfoss.app.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eurovisionfoss.app.data.ALL_COUNTRIES
import com.google.gson.JsonParser
import kotlinx.coroutines.launch

private val WATCHING_COUNTRIES = ALL_COUNTRIES.map { it.id to it.name }.sortedBy { it.second }
private const val FINALS_DATE = "Saturday 16 May 2026"
private const val FINALS_TIME = "21:00 CET (20:00 UTC)"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    context: Context,
    onComplete: (watchingCountryId: String?) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var selectedCountryId by remember { mutableStateOf<String?>(null) }
    var showAustriaDialog by remember { mutableStateOf(false) }

    if (showAustriaDialog) {
        AlertDialog(
            onDismissRequest = { showAustriaDialog = false },
            title = { Text("Watching from Austria?") },
            text = {
                Text(
                    "As Austria is the host country of Eurovision 2026, Austrian viewers are " +
                    "still welcome to cast a televote — you just cannot vote for Austria itself, " +
                    "as EBU rules prevent a country from awarding points to their own entry. " +
                    "You can vote for any of the other finalists using the standard phone number " +
                    "or the official Eurovision app, and your vote will count as part of the " +
                    "Austrian televote result."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showAustriaDialog = false
                    scope.launch { pagerState.animateScrollToPage(2) }
                }) { Text("Got it") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> OnboardingPage1(
                    onNext = { scope.launch { pagerState.animateScrollToPage(1) } }
                )
                1 -> OnboardingPage2(
                    context = context,
                    selectedId = selectedCountryId,
                    onSelect = { id ->
                        selectedCountryId = id
                        if (id == "AT") {
                            showAustriaDialog = true
                        } else {
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }
                    },
                    onSkip = {
                        selectedCountryId = null
                        scope.launch { pagerState.animateScrollToPage(2) }
                    },
                    onBack = { scope.launch { pagerState.animateScrollToPage(0) } }
                )
                2 -> OnboardingPage3(
                    context = context,
                    countryId = selectedCountryId,
                    onDone = { onComplete(selectedCountryId) },
                    onBack = { scope.launch { pagerState.animateScrollToPage(1) } }
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage1(onNext: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Libre Eurovision\uD83E\uDD29", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Vienna 2026", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))

            Text("\uD83D\uDD12 What we DON'T do", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            val rows = listOf(
                "Official App" to "Libre Eurovision\uD83E\uDD29",
                "Account + email required" to "No account. Ever.",
                "Shares data with 3rd parties" to "Zero sharing",
                "Collects personal info" to "Collects nothing",
                "~20 device permissions" to "Only what's needed",
                "10 tracking libraries" to "No trackers",
                "Scores on their servers" to "Your scores stay on your phone"
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Official App", fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error)
                        Text("Libre Eurovision", fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF34C759))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    rows.drop(1).forEach { (bad, good) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Text(bad, modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(good, modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Everything you do stays on your device. No servers, no profiles, no ads.\n" +
                "You can read every line of code — it's open source under GPL v3.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("Next")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingPage2(
    context: Context,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = WATCHING_COUNTRIES.find { it.first == selectedId }?.second ?: ""

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("\uD83D\uDCFA Where are you watching from?",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("This helps us show you the right televote details for your country.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Watching country") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    WATCHING_COUNTRIES.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                expanded = false
                                onSelect(id)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onBack) { Text("Back") }
                TextButton(onClick = onSkip) { Text("Skip / Not voting by phone") }
            }
        }
    }
}

@Composable
private fun OnboardingPage3(
    context: Context,
    countryId: String?,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val votingInfo = remember(countryId) { loadVotingInfo(context, countryId) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("\uD83D\uDCDE Your Televote Info",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (countryId == null) {
                        Text("You skipped country selection.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("You can set your watching country later in the About section.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    } else {
                        Text(votingInfo.countryName, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))

                        if (votingInfo.number == "TBC") {
                            Text(
                                "Voting numbers for Eurovision 2026 are released by the EBU close to the Grand Final.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Check back after $FINALS_DATE at $FINALS_TIME when voting opens.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text("Call to vote:", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(votingInfo.number, fontSize = 32.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFFBB86FC))
                            if (votingInfo.note.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(votingInfo.note, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center)
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Grand Final: $FINALS_DATE\nVoting opens: $FINALS_TIME",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Start scoring!")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack) { Text("Back") }
        }
    }
}

private data class VotingInfo(
    val countryName: String,
    val number: String,
    val note: String
)

private fun loadVotingInfo(context: Context, countryId: String?): VotingInfo {
    if (countryId == null) return VotingInfo("", "TBC", "")
    return runCatching {
        val json = context.assets.open("voting_numbers.json")
            .bufferedReader().use { it.readText() }
        val root = JsonParser.parseString(json).asJsonObject
        val entry = root.getAsJsonObject(countryId) ?: return VotingInfo(countryId, "TBC", "")
        val name = com.eurovisionfoss.app.data.ALL_COUNTRIES.find { it.id == countryId }?.name ?: countryId
        VotingInfo(
            countryName = name,
            number = entry.get("number")?.asString ?: "TBC",
            note = entry.get("note")?.asString ?: ""
        )
    }.getOrDefault(VotingInfo(countryId, "TBC", ""))
}
