package com.myhealth.app.ui.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.myhealth.app.health.RunSummary
import com.myhealth.app.ui.shell.AppHeader
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RunTrackerScreen(
    nav: NavController,
    viewModel: RunViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("live_run") }) {
                Icon(Icons.Default.DirectionsRun, contentDescription = "Start Run")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            AppHeader(title = "Run Tracker", nav = nav)

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            } else if (state.recentRuns.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.height(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No runs yet. Tap + to start!", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.recentRuns) { run ->
                        RunCard(run = run, onClick = { nav.navigate("run_detail/${run.sessionId}") })
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RunCard(run: RunSummary, onClick: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
    val date = run.startTime.atZone(ZoneId.systemDefault()).format(formatter)
    val duration = Duration.between(run.startTime, run.endTime)
    val minutes = duration.toMinutes()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(date, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Duration", style = MaterialTheme.typography.labelSmall)
                    Text("${minutes}m", fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Title", style = MaterialTheme.typography.labelSmall)
                    Text(run.title ?: "Run", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
