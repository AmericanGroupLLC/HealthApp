package com.myhealth.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.myhealth.app.health.HealthConnectGateway
import com.myhealth.app.ui.common.StateWrapper
import com.myhealth.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun WellnessInsightsScreen(nav: NavController, vm: WellnessInsightsViewModel = hiltViewModel()) {
    val insightsState by vm.insights.collectAsState()

    Column {
        Text(
            "Wellness Insights",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        StateWrapper(state = insightsState, onRetry = { vm.refresh() }) { insights ->
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
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Weekly Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Your readiness score is looking good", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                items(insights) { insight ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = insight.color)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(insight.title, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(insight.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Text(
                        "This is not medical advice. Consult your healthcare provider before making changes to your health routine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

data class InsightCard(val title: String, val description: String, val color: Color)

@HiltViewModel
class WellnessInsightsViewModel @Inject constructor(
    private val hc: HealthConnectGateway,
) : ViewModel() {
    private val _insights = MutableStateFlow<UiState<List<InsightCard>>>(UiState.Loading)
    val insights = _insights.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _insights.value = UiState.Loading
        try {
            val cards = mutableListOf<InsightCard>()

            val sleepHrs = hc.lastNightSleepHours()
            if (sleepHrs != null) {
                val desc = if (sleepHrs >= 7.0)
                    "Your sleep was %.1fh last night — on track with your 8h goal.".format(sleepHrs)
                else
                    "Your sleep was only %.1fh last night — below the 8h goal.".format(sleepHrs)
                cards += InsightCard("Sleep Quality", desc, Color(0xFF7E57C2))
            }

            val hrv = hc.readHrvAverage()
            if (hrv != null) {
                cards += InsightCard("HRV Trend", "7-day HRV average: %.0f ms.".format(hrv), Color(0xFF4CAF50))
            }

            val stepsHistory = hc.readStepsHistory(7)
            if (stepsHistory.isNotEmpty()) {
                val avg = stepsHistory.sumOf { it.second } / stepsHistory.size
                cards += InsightCard("Activity", "7-day step average: $avg steps/day.", Color(0xFFF57C00))
            }

            val rhr = hc.latestRestingHR()
            if (rhr != null) {
                cards += InsightCard("Resting HR", "Latest resting heart rate: ${rhr.toInt()} bpm.", Color(0xFF42A5F5))
            }

            if (cards.isEmpty()) {
                cards += InsightCard("No Data", "Connect Health Connect to see personalized insights.", Color(0xFF9E9E9E))
            }

            _insights.value = UiState.Success(cards)
        } catch (e: Exception) {
            _insights.value = UiState.Error(e.message ?: "Failed to load insights")
        }
    }
}
