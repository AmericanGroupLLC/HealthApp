package com.myhealth.app.ui.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhealth.app.data.room.ExerciseLogDao
import com.myhealth.core.models.LoggedSet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class WorkoutLoggerState(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val sets: List<LoggedSet> = listOf(LoggedSet(reps = 10, weight = 20.0)),
    val notes: String = "",
    val restTimerSeconds: Int = 90,
    val isRestTimerRunning: Boolean = false,
    val restTimeRemaining: Int = 0,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class WorkoutLoggerViewModel @Inject constructor(
    private val exerciseLogDao: ExerciseLogDao,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutLoggerState())
    val state: StateFlow<WorkoutLoggerState> = _state

    fun setExercise(id: String, name: String) {
        _state.value = _state.value.copy(exerciseId = id, exerciseName = name)
        viewModelScope.launch {
            // Try to pre-fill from last session
            try {
                val logs = exerciseLogDao.observeFor(id, 1).first()
                val latest = logs.firstOrNull()
                if (latest != null) {
                    val previousSets = Json.decodeFromString<List<LoggedSet>>(latest.setsJSON)
                    if (previousSets.isNotEmpty()) {
                        _state.value = _state.value.copy(sets = previousSets)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun updateSet(index: Int, set: LoggedSet) {
        val updated = _state.value.sets.toMutableList()
        if (index in updated.indices) {
            updated[index] = set
            _state.value = _state.value.copy(sets = updated)
        }
    }

    fun addSet() {
        val last = _state.value.sets.lastOrNull() ?: LoggedSet(reps = 10, weight = 20.0)
        _state.value = _state.value.copy(sets = _state.value.sets + last.copy())
    }

    fun removeSet(index: Int) {
        if (_state.value.sets.size > 1) {
            _state.value = _state.value.copy(sets = _state.value.sets.toMutableList().apply { removeAt(index) })
        }
    }

    fun updateNotes(notes: String) {
        _state.value = _state.value.copy(notes = notes)
    }

    fun setRestTimer(seconds: Int) {
        _state.value = _state.value.copy(restTimerSeconds = seconds)
    }

    fun save() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            val s = _state.value
            exerciseLogDao.insert(
                com.myhealth.app.data.room.ExerciseLogEntity(
                    exerciseId = s.exerciseId,
                    setsJSON = Json.encodeToString(s.sets),
                    notes = s.notes,
                    performedAt = System.currentTimeMillis(),
                )
            )
            _state.value = _state.value.copy(isSaving = false, savedSuccessfully = true)
        }
    }
}
