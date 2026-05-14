package com.myhealth.app.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhealth.app.data.social.LeaderboardEntry
import com.myhealth.app.data.social.SocialRepository
import com.myhealth.core.models.Challenge
import com.myhealth.core.models.Friend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialState(
    val isLoading: Boolean = true,
    val friends: List<Friend> = emptyList(),
    val challenges: List<Challenge> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repo: SocialRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SocialState())
    val state: StateFlow<SocialState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val friends = repo.getFriends()
                val challenges = repo.getChallenges()
                val lb = if (challenges.isNotEmpty()) {
                    repo.getLeaderboard(challenges.first().id.toInt())
                } else emptyList()
                _state.value = SocialState(
                    isLoading = false,
                    friends = friends,
                    challenges = challenges,
                    leaderboard = lb,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun addFriend(name: String, handle: String) {
        viewModelScope.launch {
            try {
                repo.addFriend(name, handle)
                refresh()
            } catch (_: Exception) {}
        }
    }

    fun removeFriend(id: Int) {
        viewModelScope.launch {
            try {
                repo.removeFriend(id)
                refresh()
            } catch (_: Exception) {}
        }
    }

    fun createChallenge(title: String, kind: String, days: Int, target: Double) {
        viewModelScope.launch {
            try {
                repo.createChallenge(title, kind, days, target)
                refresh()
            } catch (_: Exception) {}
        }
    }

    fun joinChallenge(id: Int) {
        viewModelScope.launch {
            try {
                repo.joinChallenge(id)
                refresh()
            } catch (_: Exception) {}
        }
    }

    fun loadLeaderboard(challengeId: Int) {
        viewModelScope.launch {
            try {
                val lb = repo.getLeaderboard(challengeId)
                _state.value = _state.value.copy(leaderboard = lb)
            } catch (_: Exception) {}
        }
    }

    fun submitScore(challengeId: Int, score: Double) {
        viewModelScope.launch {
            try {
                repo.submitScore(challengeId, score)
                loadLeaderboard(challengeId)
            } catch (_: Exception) {}
        }
    }
}
