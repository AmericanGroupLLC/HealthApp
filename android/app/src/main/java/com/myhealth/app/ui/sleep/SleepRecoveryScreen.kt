package com.myhealth.app.ui.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.myhealth.app.health.SleepStage
import com.myhealth.app.ui.shell.AppHeader

private val STAGE_COLORS = mapOf(
    1 to Color(0xFF1A237E), // Deep
    2 to Color(0xFF4A148C), // REM
    3 to Color(0xFF0D47A1), // Core/Light
    4 to Color(0xFFE65100), // Awake
)

private fun stageName(type: Int) = when (type) {
    1 -> "Deep"
    2 -> "REM"
    3 -> "Light"
    4 -> "Awake"
    else -> "Unknown"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepRecoveryScreen(
    nav: NavController,
    viewModel: SleepRecoveryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showWindDown by remember { mutableStateOf(false) }

    if (showWindDown) {
        ModalBottomSheet(
            onDismissRequest = { showWindDown = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            WindDownScreen(
                healthConnect = viewModel.healthConnect,
                onDismiss = { showWindDown = false },
            )
        }
    }

    Column {
        AppHeader(title = "Sleep & Recovery", nav = nav)

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Recovery Score Ring
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Recovery Score", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        RecoveryScoreRing(score = state.recoveryScore)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            state.recoverySuggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Sleep Stages Chart
            if (state.sleepStages.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Sleep Stages", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            SleepStagesChart(stages = state.sleepStages)
                            Spacer(modifier = Modifier.height(8.dp))
                            SleepStageLegend()
                        }
                    }
                }
            }

            // Stats Tiles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatTile(
                        label = "Sleep",
                        value = state.sleepHours?.let { "%.1fh".format(it) } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "HRV",
                        value = state.hrvAverage?.let { "%.0fms".format(it) } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "RHR",
                        value = state.restingHR?.let { "%.0f bpm".format(it) } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Wind Down Button
            item {
                Button(
                    onClick = { showWindDown = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.NightsStay, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wind Down")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RecoveryScoreRing(score: Int) {
    val scoreColor = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFFA726)
        score >= 40 -> Color(0xFFFF7043)
        else -> Color(0xFFEF5350)
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 12.dp.toPx()
            drawArc(
                color = Color.Gray.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
            )
            drawArc(
                color = scoreColor,
                startAngle = -90f,
                sweepAngle = 360f * score / 100f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$score",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor,
            )
            Text("/ 100", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SleepStagesChart(stages: List<SleepStage>) {
    if (stages.isEmpty()) return
    val earliest = stages.minOf { it.startTime }
    val latest = stages.maxOf { it.endTime }
    val totalDuration = java.time.Duration.between(earliest, latest).toMinutes().toFloat().coerceAtLeast(1f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
    ) {
        val barHeight = size.height / 4
        stages.forEach { stage ->
            val startFrac = java.time.Duration.between(earliest, stage.startTime).toMinutes() / totalDuration
            val endFrac = java.time.Duration.between(earliest, stage.endTime).toMinutes() / totalDuration
            val yIndex = when (stage.type) {
                4 -> 0; 2 -> 1; 3 -> 2; 1 -> 3; else -> 2
            }
            drawRect(
                color = STAGE_COLORS[stage.type] ?: Color.Gray,
                topLeft = Offset(startFrac * size.width, yIndex * barHeight),
                size = Size((endFrac - startFrac) * size.width, barHeight - 2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun SleepStageLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        STAGE_COLORS.forEach { (type, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(10.dp)) { drawCircle(color) }
                Spacer(modifier = Modifier.width(4.dp))
                Text(stageName(type), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
