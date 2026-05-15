package com.eurovisionfoss.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eurovisionfoss.app.data.ALL_COUNTRIES
import com.eurovisionfoss.app.ui.AboutScreen
import com.eurovisionfoss.app.ui.OnboardingScreen
import com.eurovisionfoss.app.ui.ResultsScreen
import com.eurovisionfoss.app.ui.VotingScreen
import com.eurovisionfoss.app.ui.theme.EurovisionFOSSTheme
import com.eurovisionfoss.app.util.BackupManager
import kotlinx.coroutines.launch

private const val PREF_FILE = "libre_eurovision"
private const val PREF_ONBOARDING_DONE = "onboarding_done"
private const val PREF_WATCHING_COUNTRY = "watching_country"

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE)

        setContent {
            EurovisionFOSSTheme {
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background) {

                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    var onboardingDone by remember {
                        mutableStateOf(prefs.getBoolean(PREF_ONBOARDING_DONE, false))
                    }
                    var watchingCountryId by remember {
                        mutableStateOf(prefs.getString(PREF_WATCHING_COUNTRY, null))
                    }
                    var showAbout by remember { mutableStateOf(false) }
                    var showOnboarding by remember { mutableStateOf(false) }
                    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

                    // File picker for import
                    val importLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument()
                    ) { uri -> if (uri != null) pendingImportUri = uri }

                    // Replace-warning dialog
                    pendingImportUri?.let { uri ->
                        AlertDialog(
                            onDismissRequest = { pendingImportUri = null },
                            title = { Text("Replace your scores?") },
                            text = {
                                Text(
                                    "Importing this backup will replace all your current scores and notes. " +
                                    "This cannot be undone.\n\n" +
                                    "Countries not recognised in this version of the app will appear as " +
                                    "\u2018Unknown Import\u2019 at the bottom of your lists so no data is ever lost."
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val capturedUri = uri
                                    pendingImportUri = null
                                    scope.launch {
                                        val knownIds = ALL_COUNTRIES.map { it.id }.toSet()
                                        val result = BackupManager.parseImport(
                                            context, capturedUri, knownIds, viewModel.worldIdToName
                                        )
                                        viewModel.applyImport(result, emptyList())
                                    }
                                }) { Text("Replace") }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
                            }
                        )
                    }

                    when {
                        !onboardingDone || showOnboarding -> {
                            OnboardingScreen(
                                context = context,
                                onComplete = { countryId ->
                                    prefs.edit()
                                        .putBoolean(PREF_ONBOARDING_DONE, true)
                                        .putString(PREF_WATCHING_COUNTRY, countryId)
                                        .apply()
                                    watchingCountryId = countryId
                                    onboardingDone = true
                                    showOnboarding = false
                                }
                            )
                        }

                        showAbout -> {
                            val countryName = ALL_COUNTRIES.find { it.id == watchingCountryId }?.name
                                ?: watchingCountryId
                            AboutScreen(
                                watchingCountry = countryName,
                                onResetOnboarding = { showAbout = false; showOnboarding = true },
                                onChangeCountry   = { showAbout = false; showOnboarding = true }
                            )
                        }

                        else -> {
                            val drawerState = rememberDrawerState(DrawerValue.Closed)
                            val pagerState = rememberPagerState(pageCount = { 2 })

                            ModalNavigationDrawer(
                                drawerState = drawerState,
                                drawerContent = {
                                    ModalDrawerSheet {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Libre Eurovision\uD83E\uDD29",
                                                fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Text("Vienna 2026 \u2022 v0.0.2",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        HorizontalDivider()

                                        NavigationDrawerItem(
                                            label = { Text("\uD83D\uDCE4  Share ranking") },
                                            selected = false,
                                            onClick = {
                                                scope.launch {
                                                    drawerState.close()
                                                    pagerState.animateScrollToPage(1)
                                                }
                                            }
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("\uD83D\uDCBE  Backup scores") },
                                            selected = false,
                                            onClick = {
                                                scope.launch {
                                                    drawerState.close()
                                                    val scores = viewModel.scores.value.values.toList()
                                                    val knownIds = ALL_COUNTRIES.map { it.id }.toSet()
                                                    val intent = BackupManager.export(
                                                        context, scores, knownIds)
                                                    context.startActivity(
                                                        Intent.createChooser(intent, "Save backup"))
                                                }
                                            }
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("\uD83D\uDCC2  Import backup") },
                                            selected = false,
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                importLauncher.launch(arrayOf("application/json"))
                                            }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        NavigationDrawerItem(
                                            label = { Text("\uD83D\uDD0D  Host city info") },
                                            selected = false,
                                            onClick = {
                                                scope.launch {
                                                    drawerState.close()
                                                    pagerState.animateScrollToPage(0)
                                                }
                                                viewModel.toggleHostCity()
                                            }
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("About") },
                                            selected = false,
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                showAbout = true
                                            }
                                        )
                                    }
                                }
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    val openMenu = { scope.launch { drawerState.open() } }
                                    when (page) {
                                        0 -> VotingScreen(viewModel, onMenuClick = { openMenu() })
                                        1 -> ResultsScreen(viewModel, onMenuClick = { openMenu() })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
