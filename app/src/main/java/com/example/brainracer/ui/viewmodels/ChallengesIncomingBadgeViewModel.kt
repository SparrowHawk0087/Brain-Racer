package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.ChallengeRepositoryImpl
import com.example.brainracer.domain.entities.ChallengeStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Одна подписка на входящие вызовы (PENDING) для индикатора на вкладке «Вызовы» в BottomBar на всех табах.
 */
class ChallengesIncomingBadgeViewModel : ViewModel() {

    private val challengeRepository = ChallengeRepositoryImpl()

    private val _hasIncomingPending = MutableStateFlow(false)
    val hasIncomingPending: StateFlow<Boolean> = _hasIncomingPending.asStateFlow()

    private var observeJob: Job? = null

    fun bindToUser(uid: String?) {
        observeJob?.cancel()
        observeJob = null
        if (uid.isNullOrBlank()) {
            _hasIncomingPending.value = false
            return
        }
        observeJob = viewModelScope.launch {
            challengeRepository.observeUserChallengeSides(uid)
                .catch {
                    _hasIncomingPending.value = false
                }
                .collect { sides ->
                    _hasIncomingPending.value =
                        sides.asChallenged.any { it.status == ChallengeStatus.PENDING }
                }
        }
    }

    override fun onCleared() {
        observeJob?.cancel()
        super.onCleared()
    }
}
