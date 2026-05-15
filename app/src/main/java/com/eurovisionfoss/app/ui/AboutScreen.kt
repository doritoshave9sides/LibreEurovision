package com.eurovisionfoss.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    watchingCountry: String?,
    onResetOnboarding: () -> Unit,
    onChangeCountry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("About", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            Text("Libre Eurovision\uD83E\uDD29", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Version 0.0.2 — Vienna 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            InfoSection(
                title = "\uD83D\uDD13 Privacy",
                body = "This app collects nothing about you. No account, no email, no analytics, " +
                       "no tracking libraries. Your scores and notes are stored only on this device."
            )

            Spacer(Modifier.height(12.dp))
            InfoSection(
                title = "\u2696\uFE0F Licence",
                body = "Libre Eurovision is free and open source software, released under the " +
                       "GNU General Public Licence v3 (GPL v3). You are free to use, study, " +
                       "share, and improve it. Any modified version you distribute must also be " +
                       "open source under the same licence."
            )

            Spacer(Modifier.height(12.dp))
            InfoSection(
                title = "\uD83D\uDCCA Data sources",
                body = "Historical Eurovision statistics are bundled within the app. " +
                       "Data accurate as of 01 January 2026.\n\n" +
                       "Artist biographies and images are fetched from Wikipedia under the " +
                       "Creative Commons Attribution-ShareAlike licence and cached for 7 days."
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("\uD83C\uDF1F Your Settings", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (watchingCountry != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Watching country", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(watchingCountry, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedButton(onClick = onChangeCountry, modifier = Modifier.fillMaxWidth()) {
                Text("Change watching country")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onResetOnboarding, modifier = Modifier.fillMaxWidth()) {
                Text("Re-open welcome wizard")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoSection(title: String, body: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
