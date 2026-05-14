package com.myhealth.app.ui.train

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.myhealth.app.data.prefs.SettingsRepository
import com.myhealth.app.ui.shell.AppHeader
import com.myhealth.app.ui.theme.CareTab
import com.myhealth.app.ui.Routes
import com.myhealth.core.exercises.Exercise
import com.myhealth.core.exercises.ExerciseLibrary
import com.myhealth.core.exercises.ProgramDay
import com.myhealth.core.exercises.WorkoutPrograms
import com.myhealth.core.health.ExerciseMedia
import com.myhealth.core.health.ExerciseMedicalMap
import com.myhealth.core.health.HealthCondition
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PlanExercise(
    val exercise: Exercise,
    val sets: Int,
    val repRange: String,
    val gifUrl: String,
    val isBeneficial: Boolean,
)

data class TodayPlanState(
    val dayName: String = "",
    val exercises: List<PlanExercise> = emptyList(),
    val estimatedMinutes: Int = 0,
)

@HiltViewModel
class TodayPlanViewModel @Inject constructor(
    settings: SettingsRepository,
) : ViewModel() {

    private val conditions: StateFlow<Set<HealthCondition>> = settings.healthConditions
        .map { names -> names.mapNotNull { n -> HealthCondition.values().firstOrNull { it.name == n } }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _state = MutableStateFlow(TodayPlanState())
    val state: StateFlow<TodayPlanState> = _state.asStateFlow()

    init { generatePlan() }

    private fun generatePlan() {
        val program = WorkoutPrograms.all.first()
        val dayIndex = LocalDate.now().dayOfWeek.ordinal % program.days.size
        val programDay: ProgramDay = program.days[dayIndex]
        val conds = conditions.value

        val exercises = programDay.exerciseIds
            .mapNotNull { id -> ExerciseLibrary.byId(id) }
            .filter { ExerciseMedicalMap.isSafe(it.id, conds) }
            .map { exercise ->
                PlanExercise(
                    exercise = exercise,
                    sets = programDay.sets,
                    repRange = programDay.repRange,
                    gifUrl = ExerciseMedia.gifUrl(exercise.id),
                    isBeneficial = ExerciseMedicalMap.benefitsFor(exercise.id, conds).isNotEmpty(),
                )
            }

        val estMinutes = exercises.size * 4 + 5 // ~4 min per exercise + warm-up

        _state.value = TodayPlanState(
            dayName = programDay.name,
            exercises = exercises,
            estimatedMinutes = estMinutes,
        )
    }
}

@Composable
fun TodayPlanScreen(nav: NavController, vm: TodayPlanViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column {
        AppHeader(
            tab = CareTab.Train,
            onProfile = { nav.navigate(Routes.PROFILE) },
            onBell = { nav.navigate(Routes.NEWS_DRAWER) },
        )

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    state.dayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${state.exercises.size} exercises • ~${state.estimatedMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(state.exercises) { plan ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = plan.gifUrl,
                            contentDescription = plan.exercise.name,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plan.exercise.name, fontWeight = FontWeight.Bold)
                            Text(
                                "${plan.sets} × ${plan.repRange}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                plan.exercise.primaryMuscles.joinToString { it.label },
                                style = MaterialTheme.typography.labelSmall,
                            )
                            if (plan.isBeneficial) {
                                Text(
                                    "✓ Beneficial for your conditions",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { nav.navigate("workout_logger") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Workout")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
