package com.myhealth.app.ui.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhealth.app.health.HealthConnectGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WaterTrackerState(
    val todayMl: Double = 0.0,
    val goalMl: Double = 2000.0,
    val history: List<Pair<String, Double>> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class WaterTrackerViewModel @Inject constructor(
    private val healthConnect: HealthConnectGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(WaterTrackerState())
    val state: StateFlow<WaterTrackerState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val today = healthConnect.readHydrationToday()
            _state.value = _state.value.copy(todayMl = today, isLoading = false)
        }
    }

    fun addWater(ml: Double) {
        viewModelScope.launch {
            healthConnect.writeHydration(ml)
            _state.value = _state.value.copy(todayMl = _state.value.todayMl + ml)
        }
    }

    fun setGoal(ml: Double) {
        _state.value = _state.value.copy(goalMl = ml)
    }
}
