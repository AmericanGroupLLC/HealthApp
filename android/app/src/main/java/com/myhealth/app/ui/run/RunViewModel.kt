package com.myhealth.app.ui.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhealth.app.health.HealthConnectGateway
import com.myhealth.app.health.RunSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

enum class RunState { IDLE, RUNNING, PAUSED, COMPLETED }

data class RunUiState(
    val recentRuns: List<RunSummary> = emptyList(),
    val isLoading: Boolean = true,
    val runState: RunState = RunState.IDLE,
    val elapsedSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val currentPace: String = "--:--",
    val routePoints: List<Pair<Double, Double>> = emptyList(),
    val startTime: Instant? = null,
)

@HiltViewModel
class RunViewModel @Inject constructor(
    private val healthConnect: HealthConnectGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(RunUiState())
    val state: StateFlow<RunUiState> = _state

    init { loadRecentRuns() }

    fun loadRecentRuns() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val runs = healthConnect.readRecentRuns()
            _state.value = _state.value.copy(recentRuns = runs, isLoading = false)
        }
    }

    fun startRun() {
        _state.value = _state.value.copy(
            runState = RunState.RUNNING,
            startTime = Instant.now(),
            elapsedSeconds = 0,
            distanceMeters = 0.0,
            routePoints = emptyList(),
        )
    }

    fun pauseRun() {
        _state.value = _state.value.copy(runState = RunState.PAUSED)
    }

    fun resumeRun() {
        _state.value = _state.value.copy(runState = RunState.RUNNING)
    }

    fun stopRun() {
        viewModelScope.launch {
            val s = _state.value
            if (s.startTime != null) {
                healthConnect.writeExerciseSession(
                    exerciseType = 56, // RUNNING
                    startTime = s.startTime,
                    endTime = Instant.now(),
                    title = "Run",
                )
            }
            _state.value = _state.value.copy(runState = RunState.COMPLETED)
            loadRecentRuns()
        }
    }

    fun updateLocation(lat: Double, lng: Double) {
        val s = _state.value
        val newPoints = s.routePoints + (lat to lng)
        var distance = s.distanceMeters
        if (newPoints.size >= 2) {
            val (prevLat, prevLng) = newPoints[newPoints.size - 2]
            distance += haversine(prevLat, prevLng, lat, lng)
        }
        val elapsed = if (s.startTime != null) Duration.between(s.startTime, Instant.now()).seconds else 0
        val pace = if (distance > 0) {
            val minPerKm = (elapsed / 60.0) / (distance / 1000.0)
            "%d:%02d".format(minPerKm.toInt(), ((minPerKm % 1) * 60).toInt())
        } else "--:--"
        _state.value = s.copy(
            routePoints = newPoints,
            distanceMeters = distance,
            elapsedSeconds = elapsed,
            currentPace = pace,
        )
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
