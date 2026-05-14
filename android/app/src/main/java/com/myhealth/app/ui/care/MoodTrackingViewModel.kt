package com.myhealth.app.ui.care

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhealth.app.data.room.MoodDao
import com.myhealth.app.data.room.MoodEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MoodTrackingViewModel @Inject constructor(
    private val moodDao: MoodDao,
) : ViewModel() {

    val moods: StateFlow<List<MoodEntity>> = moodDao.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveMood(scale: Int, notes: String) {
        viewModelScope.launch {
            moodDao.insert(
                MoodEntity(
                    value = scale,
                    note = notes.ifBlank { null },
                )
            )
        }
    }
}
