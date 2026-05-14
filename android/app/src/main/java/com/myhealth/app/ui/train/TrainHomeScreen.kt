package com.myhealth.app.ui.train

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirlineSeatReclineExtra
import androidx.compose.material.icons.filled.SelfImprovement
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.myhealth.app.data.prefs.SettingsRepository
import com.myhealth.app.health.HealthConnectGateway
import com.myhealth.app.ui.Routes
import com.myhealth.app.ui.shell.AppHeader
import com.myhealth.app.ui.theme.CarePlusColor
import com.myhealth.app.ui.theme.CareTab
import com.myhealth.app.ui.common.StateWrapper
import com.myhealth.app.ui.common.UiState
import com.myhealth.core.exercises.ExerciseLibrary
import com.myhealth.core.health.ExerciseMedia
import com.myhealth.core.health.ExerciseMedicalMap
import com.myhealth.core.health.HealthCondition
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Train tab home — adds the design-spec "Moderate workout for you"
 * recommendation card above the existing programs list. Mirrors iOS
 * `TrainHomeView`.
 */
data class RecoveryData(
    val sleepHours: Double? = null,
    val hrvAvg: Double? = null,
) {
    val recommendation: String get() {
        val sleep = sleepHours ?: return "Loading recovery data…"
        val hrv = hrvAvg
        return when {
            sleep < 6.0 || (hrv != null && hrv < 30) -> "Light recovery flow"
            sleep < 7.0 || (hrv != null && hrv < 50) -> "Moderate workout"
            else -> "Full intensity workout"
        }
    }
    val durationMin: Int get() {
        val sleep = sleepHours ?: return 30
        return when {
            sleep < 6.0 -> 20
            sleep < 7.0 -> 35
            else -> 45
        }
    }
    val subtitle: String get() {
        val parts = mutableListOf<String>()
        sleepHours?.let { parts += "Sleep %.1fh".format(it) }
        hrvAvg?.let { parts += if (it < 40) "HRV low" else "HRV %.0f".format(it) }
        return parts.joinToString(" · ").ifEmpty { "No health data" }
    }
}

@HiltViewModel
class TrainHomeViewModel @Inject constructor(
    settings: SettingsRepository,
    private val healthConnect: HealthConnectGateway,
) : ViewModel() {
    val conditions: StateFlow<Set<HealthCondition>> = settings.healthConditions
        .map { names -> names.mapNotNull { n -> HealthCondition.values().firstOrNull { it.name == n } }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _recovery = MutableStateFlow<UiState<RecoveryData>>(UiState.Loading)
    val recovery: StateFlow<UiState<RecoveryData>> = _recovery.asStateFlow()

    init { loadRecoveryData() }

    fun loadRecoveryData() = viewModelScope.launch {
        _recovery.value = UiState.Loading
        try {
            val sleep = healthConnect.lastNightSleepHours()
            val hrv = healthConnect.readHrvAverage()
            _recovery.value = UiState.Success(RecoveryData(sleepHours = sleep, hrvAvg = hrv))
        } catch (e: Exception) {
            _recovery.value = UiState.Error(e.message ?: "Failed to load recovery data")
        }
    }
}

@Composable
fun TrainHomeScreen(nav: NavController, vm: TrainHomeViewModel = hiltViewModel()) {
    val tint = CarePlusColor.TrainGreen
    val conditions by vm.conditions.collectAsState()
    val recoveryState by vm.recovery.collectAsState()
    val recommended = ExerciseLibrary.recommended(conditions).take(6)

    Column(Modifier.fillMaxSize()) {
        AppHeader(
            tab = CareTab.Train,
            onProfile = { nav.navigate(Routes.PROFILE) },
            onBell = { nav.navigate(Routes.NEWS_DRAWER) },
        )
        LazyColumn(Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Recommendation card
            item {
                StateWrapper(state = recoveryState, onRetry = { vm.loadRecoveryData() }) { recovery ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(tint.copy(alpha = 0.10f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(recovery.recommendation + " for you",
                                color = tint, fontWeight = FontWeight.SemiBold)
                            Text("${recovery.durationMin} min ${recovery.recommendation.lowercase()}",
                                fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(recovery.subtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)) {
                                listOf("rate","run","care","diet").forEach { tag ->
                                    Box(
                                        Modifier.background(MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(99.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) { Text(tag, fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp) }
                                }
                            }
                        }
                    }
                }
            }

            // ── Recommended for you (condition-based) ────────────
            if (recommended.isNotEmpty()) {
                item {
                    Text("Recommended for you", fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp))
                    if (conditions.isNotEmpty() && conditions != setOf(HealthCondition.none)) {
                        Text("Based on your declared conditions · safe exercises only",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(recommended) { exercise ->
                    val benefits = ExerciseMedicalMap.benefitsFor(exercise.id, conditions)
                    val isBeneficial = benefits.isNotEmpty()
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { nav.navigate("${Routes.EXERCISE_DETAIL}/${exercise.id}") },
                        colors = CardDefaults.cardColors(
                            if (isBeneficial) Color(0xFF34C759).copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = ExerciseMedia.thumbnailUrl(exercise.id),
                                contentDescription = exercise.name,
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(exercise.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    exercise.primaryMuscles.joinToString { it.label },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isBeneficial) {
                                    Text(
                                        "✓ Beneficial for: ${benefits.joinToString { it.label }}",
                                        fontSize = 11.sp, color = Color(0xFF34C759),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Today's plan", fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp))
            }
            item { PlanRow(Icons.Filled.SelfImprovement, "Warm-up", "5 min · 4 moves") }
            item { PlanRow(Icons.Filled.SelfImprovement, "Strength block",
                "20 min · 6 moves") }
            item { PlanRow(Icons.Filled.SelfImprovement, "Cooldown",
                "10 min · breathwork") }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { nav.navigate(Routes.STANDUP_TIMER) },
                    colors = CardDefaults.cardColors(
                        CarePlusColor.Warning.copy(alpha = 0.10f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AirlineSeatReclineExtra, null,
                            tint = CarePlusColor.Warning,
                            modifier = Modifier.padding(end = 12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Sedentary 52 min", fontWeight = FontWeight.SemiBold)
                            Text("Time to stand up",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { nav.navigate(Routes.WORKOUT_LIBRARY) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp)) {
                        Text("Open program library",
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Suggested fitness vendors ────────────────────
            item {
                Text("Suggested fitness vendors", fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp))
            }
            items(com.myhealth.app.data.seed.VendorSuggestions.fitnessEquipment) { v ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.background(tint.copy(alpha = 0.12f),
                                RoundedCornerShape(9.dp)).padding(8.dp)
                        ) { Icon(Icons.Filled.SelfImprovement, null, tint = tint) }
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(v.name, fontWeight = FontWeight.SemiBold)
                            Text(v.tagline, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Text(
                    "This is not medical advice. Consult your healthcare provider before making changes to your health routine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PlanRow(icon: androidx.compose.ui.graphics.vector.ImageVector,
                    title: String, subtitle: String) {
    val tint = CarePlusColor.TrainGreen
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.background(tint.copy(alpha = 0.12f),
                    RoundedCornerShape(9.dp)).padding(8.dp)
            ) { Icon(icon, null, tint = tint) }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp)
            }
        }
    }
}
