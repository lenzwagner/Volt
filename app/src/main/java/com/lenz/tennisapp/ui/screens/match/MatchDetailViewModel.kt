package com.lenz.tennisapp.ui.screens.match

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenz.tennisapp.data.repository.PredictionRepository
import com.lenz.tennisapp.data.repository.TennisRepository
import com.lenz.tennisapp.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

sealed class MatchDetailUiState {
    data class Loaded(val detail: MatchDetail, val userPrediction: UserPrediction? = null) : MatchDetailUiState()
    data class Error(val message: String) : MatchDetailUiState()
}

@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val repository: TennisRepository,
    private val predictionRepository: PredictionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val matchId: String = checkNotNull(savedStateHandle["matchId"])

    private val _uiState = MutableStateFlow<MatchDetailUiState>(
        MatchDetailUiState.Error("Lade...")  // Start with error state, will load immediately
    )
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadDetail()
        startPolling()
        
        // Observe prediction changes to keep UI in sync
        viewModelScope.launch {
            predictionRepository.getPredictionMap().collect { preds ->
                val current = _uiState.value
                if (current is MatchDetailUiState.Loaded) {
                    _uiState.value = current.copy(userPrediction = preds[matchId])
                }
            }
        }
    }

    fun loadDetail() {
        viewModelScope.launch {
            fetchDetail(isSilent = false)
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(2_000L)
                
                // Get the date of the match to refresh correctly even for past matches
                val matchDate = (_uiState.value as? MatchDetailUiState.Loaded)?.detail?.match?.date
                val date = matchDate?.let { 
                    try { LocalDate.parse(it) } catch(e: Exception) { LocalDate.now() }
                } ?: LocalDate.now()
                
                // First update the database with latest scores from API
                repository.refreshSilent(date)

                // Then reload the UI state from the database
                fetchDetail(isSilent = true)
            }
        }
    }

    private suspend fun fetchDetail(isSilent: Boolean) {
        try {
            if (!isSilent) _isRefreshing.value = true
            Timber.d("Loading match detail for ID: $matchId (silent=$isSilent)")
            val predMap = predictionRepository.getPredictionMap().first()

            // Show loaded state immediately with basic match info
            val result = repository.getMatchDetail(matchId)

            _uiState.value = when (result) {
                is Result.Success -> {
                    MatchDetailUiState.Loaded(
                        detail = result.data,
                        userPrediction = predMap[matchId]
                    )
                }
                is Result.Error -> {
                    if (isSilent) _uiState.value else MatchDetailUiState.Error(result.message)
                }
                else -> {
                    if (isSilent) _uiState.value else MatchDetailUiState.Error("Unbekannter Fehler")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception in fetchDetail")
            if (!isSilent) {
                _uiState.value = MatchDetailUiState.Error("Fehler: ${e.message}")
            }
        } finally {
            if (!isSilent) _isRefreshing.value = false
        }
    }

    fun predict(winnerKey: String, winnerName: String) {
        val state = _uiState.value as? MatchDetailUiState.Loaded ?: return
        val match = state.detail.match
        viewModelScope.launch {
            predictionRepository.savePrediction(
                matchId             = matchId,
                predictedWinnerKey  = winnerKey,
                predictedWinnerName = winnerName,
                homePlayerKey       = match.homePlayer.key,
                homePlayerName      = match.homePlayer.name,
                awayPlayerKey       = match.awayPlayer.key,
                awayPlayerName      = match.awayPlayer.name,
                matchDate           = match.date,
                tournamentName      = match.tournament
            )
        }
    }
}
