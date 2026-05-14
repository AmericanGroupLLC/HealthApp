package com.myhealth.app.ui.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhealth.app.health.HealthConnectGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class ProgressReportState(
    val isLoading: Boolean = true,
    val selectedRange: Int = 7,
    val weightHistory: List<Pair<Instant, Double>> = emptyList(),
    val stepsHistory: List<Pair<Instant, Long>> = emptyList(),
    val sleepHours: Double? = null,
    val restingHR: Double? = null,
)

@HiltViewModel
class ProgressReportViewModel @Inject constructor(
    private val healthConnect: HealthConnectGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressReportState())
    val state: StateFlow<ProgressReportState> = _state

    init { loadData(7) }

    fun loadData(days: Int) {
        _state.value = _state.value.copy(isLoading = true, selectedRange = days)
        viewModelScope.launch {
            val weight = healthConnect.readWeightHistory(days)
            val steps = healthConnect.readStepsHistory(days)
            val sleep = healthConnect.lastNightSleepHours()
            val rhr = healthConnect.latestRestingHR()
            _state.value = ProgressReportState(
                isLoading = false,
                selectedRange = days,
                weightHistory = weight,
                stepsHistory = steps,
                sleepHours = sleep,
                restingHR = rhr,
            )
        }
    }
}
