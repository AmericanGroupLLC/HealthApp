package com.myhealth.app.ui.train

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.myhealth.app.ui.shell.AppHeader

@Composable
fun ProgressReportScreen(
    nav: NavController,
    viewModel: ProgressReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column {
        AppHeader(title = "Progress Report", nav = nav)

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Range selector
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30, 90).forEach { days ->
                        FilterChip(
                            selected = state.selectedRange == days,
                            onClick = { viewModel.loadData(days) },
                            label = { Text("${days}d") },
                        )
                    }
                }
            }

            // Steps chart
            if (state.stepsHistory.isNotEmpty()) {
                item {
                    ChartCard(title = "Steps") {
                        val values = state.stepsHistory.map { it.second.toFloat() }
                        SimpleLineChart(values = values, color = Color(0xFF4CAF50))
                    }
                }
            }

            // Weight chart
            if (state.weightHistory.isNotEmpty()) {
                item {
                    ChartCard(title = "Weight (kg)") {
                        val values = state.weightHistory.map { it.second.toFloat() }
                        SimpleLineChart(values = values, color = Color(0xFF2196F3))
                    }
                }
            }

            // Summary stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryTile(
                        "Sleep", state.sleepHours?.let { "%.1fh".format(it) } ?: "—",
                        Modifier.weight(1f),
                    )
                    SummaryTile(
                        "RHR", state.restingHR?.let { "%.0f bpm".format(it) } ?: "—",
                        Modifier.weight(1f),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ChartCard(title: String, chart: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            chart()
        }
    }
}

@Composable
private fun SimpleLineChart(values: List<Float>, color: Color) {
    if (values.size < 2) return
    val minVal = values.min()
    val maxVal = values.max()
    val range = (maxVal - minVal).coerceAtLeast(1f)

    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - minVal) / range) * size.height * 0.8f - size.height * 0.1f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color, 4.dp.toPx(), Offset(x, y))
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
