package com.myhealth.app.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhealth.app.health.HealthConnectGateway
import com.myhealth.app.health.SleepStage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SleepRecoveryState(
    val isLoading: Boolean = true,
    val sleepHours: Double? = null,
    val sleepStages: List<SleepStage> = emptyList(),
    val hrvAverage: Double? = null,
    val restingHR: Double? = null,
    val recoveryScore: Int = 0,
    val recoverySuggestion: String = "",
)

@HiltViewModel
class SleepRecoveryViewModel @Inject constructor(
    internal val healthConnect: HealthConnectGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(SleepRecoveryState())
    val state: StateFlow<SleepRecoveryState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val sleepHours = healthConnect.lastNightSleepHours()
            val stages = healthConnect.readSleepStages()
            val hrv = healthConnect.readHrvAverage()
            val rhr = healthConnect.latestRestingHR()
            val (score, suggestion) = computeRecoveryScore(hrv, rhr, sleepHours)
            _state.value = SleepRecoveryState(
                isLoading = false,
                sleepHours = sleepHours,
                sleepStages = stages,
                hrvAverage = hrv,
                restingHR = rhr,
                recoveryScore = score,
                recoverySuggestion = suggestion,
            )
        }
    }

    private fun computeRecoveryScore(hrv: Double?, rhr: Double?, sleepHrs: Double?): Pair<Int, String> {
        var score = 50.0
        if (hrv != null) {
            val normalized = ((hrv - 30.0) / 50.0).coerceIn(0.0, 1.0)
            score += normalized * 25.0
        }
        if (rhr != null) {
            val normalized = ((80.0 - rhr) / 35.0).coerceIn(0.0, 1.0)
            score += normalized * 15.0
        }
        if (sleepHrs != null) {
            val penalty = (Math.abs(sleepHrs - 8.0) / 4.0).coerceIn(0.0, 1.0)
            score += (1.0 - penalty) * 15.0
        }
        val clamped = score.toInt().coerceIn(0, 100)
        val suggestion = when {
            clamped >= 80 -> "Green light — go push it 💪"
            clamped >= 60 -> "Solid day — moderate effort recommended"
            clamped >= 40 -> "Mixed signals — keep it easy today"
            else -> "Recovery day — prioritize sleep & gentle movement 🧘"
        }
        return clamped to suggestion
    }
}
