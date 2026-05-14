package com.myhealth.app.ui.care

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhealth.app.data.room.SymptomLogDao
import com.myhealth.app.data.room.SymptomLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SymptomsLogState(
    val symptoms: List<SymptomLogEntity> = emptyList(),
)

@HiltViewModel
class SymptomsLogViewModel @Inject constructor(
    private val dao: SymptomLogDao,
) : ViewModel() {

    private val _state = MutableStateFlow(SymptomsLogState())
    val state: StateFlow<SymptomsLogState> = _state

    init {
        viewModelScope.launch {
            dao.getAll().collect { list ->
                _state.value = SymptomsLogState(symptoms = list)
            }
        }
    }

    fun logSymptom(bodyLocation: String, painScale: Int, durationHours: Double, notes: String) {
        viewModelScope.launch {
            dao.insert(
                SymptomLogEntity(
                    bodyLocation = bodyLocation,
                    painScale = painScale,
                    durationHours = durationHours,
                    notes = notes.ifBlank { null },
                )
            )
        }
    }

    fun deleteSymptom(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}
