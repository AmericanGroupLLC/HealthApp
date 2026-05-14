package com.myhealth.app.ui.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.myhealth.app.ui.shell.AppHeader
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RunDetailScreen(
    sessionId: String,
    nav: NavController,
    viewModel: RunViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val run = state.recentRuns.find { it.sessionId == sessionId }

    Column {
        AppHeader(title = "Run Detail", nav = nav)

        if (run == null) {
            Text("Run not found", modifier = Modifier.padding(16.dp))
            return
        }

        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy h:mm a")
        val date = run.startTime.atZone(ZoneId.systemDefault()).format(formatter)
        val duration = Duration.between(run.startTime, run.endTime)

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(run.title ?: "Run", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(date, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    DetailStat("Duration", "${duration.toMinutes()}m")
                    DetailStat("Time", formatDuration(duration))
                }
            }

            item {
                val routePoints = state.routePoints
                val cameraPositionState = rememberCameraPositionState()

                LaunchedEffect(routePoints) {
                    if (routePoints.size >= 2) {
                        val bounds = LatLngBounds.builder().apply {
                            routePoints.forEach { include(LatLng(it.first, it.second)) }
                        }.build()
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 32))
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                ) {
                    if (routePoints.size >= 2) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                        ) {
                            Polyline(
                                points = routePoints.map { LatLng(it.first, it.second) },
                                color = Color(0xFF4285F4),
                                width = 8f,
                            )
                        }
                    } else {
                        androidx.compose.foundation.layout.Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text("No route data", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatDuration(duration: Duration): String {
    val h = duration.toHours()
    val m = duration.toMinutes() % 60
    val s = duration.seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
