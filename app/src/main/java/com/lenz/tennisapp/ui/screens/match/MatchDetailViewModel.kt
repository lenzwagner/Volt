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
    object Loading : MatchDetailUiState()
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
        repository.getCachedMatchDetail(matchId)
            ?.let { MatchDetailUiState.Loaded(it) }
            ?: MatchDetailUiState.Loading
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
            fetchDetail(isSilent = false, forceRefreshOdds = false, showIndicator = false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            Timber.d("Manual refresh triggered for match $matchId")
            fetchDetail(isSilent = false, forceRefreshOdds = true, showIndicator = true)
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

    private suspend fun fetchDetail(isSilent: Boolean, forceRefreshOdds: Boolean = false, showIndicator: Boolean = false) {
        try {
            if (showIndicator) _isRefreshing.value = true
            Timber.d("Loading match detail for ID: $matchId (silent=$isSilent, forceOdds=$forceRefreshOdds)")
            val predMap = predictionRepository.getPredictionMap().first()

            // Silent poll (every 2s): only patch the live score from DB. Don't
            // re-run the network (H2H/odds/prediction) — keep the loaded values.
            val current = _uiState.value
            if (isSilent && current is MatchDetailUiState.Loaded) {
                val fresh = repository.getFreshMatch(matchId)
                if (fresh != null) {
                    // Keep enriched players; only update score/status/game fields.
                    val merged = current.detail.match.copy(
                        score = fresh.score,
                        gameScore = fresh.gameScore,
                        status = fresh.status,
                        isHomeServing = fresh.isHomeServing,
                        setScores = fresh.setScores,
                        finalResult = fresh.finalResult,
                        winnerKey = fresh.winnerKey
                    )
                    _uiState.value = current.copy(
                        detail = current.detail.copy(match = merged),
                        userPrediction = predMap[matchId]
                    )
                    // Match just finished with a pending pick → resolve it.
                    if (merged.status == MatchStatus.FINISHED && predMap[matchId]?.isPending == true) {
                        predictionRepository.resolvePending()
                    }
                }
                return
            }

            // Instant render with DB-only data while the full version loads.
            if (!isSilent && current !is MatchDetailUiState.Loaded) {
                (repository.getMatchDetailBase(matchId) as? Result.Success)?.let {
                    _uiState.value = MatchDetailUiState.Loaded(it.data, predMap[matchId])
                }
            }

            // Full detail (H2H, odds, prediction) — patches the view when ready
            val result = repository.getMatchDetail(matchId, forceRefreshOdds)

            _uiState.value = when (result) {
                is Result.Success -> {
                    MatchDetailUiState.Loaded(
                        detail = result.data,
                        userPrediction = predMap[matchId]
                    )
                }
                is Result.Error -> {
                    val current = _uiState.value
                    if (isSilent || current is MatchDetailUiState.Loaded) current
                    else MatchDetailUiState.Error(result.message)
                }
                else -> {
                    val current = _uiState.value
                    if (isSilent || current is MatchDetailUiState.Loaded) current
                    else MatchDetailUiState.Error("Unbekannter Fehler")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception in fetchDetail")
            if (!isSilent && _uiState.value !is MatchDetailUiState.Loaded) {
                _uiState.value = MatchDetailUiState.Error("Fehler: ${e.message}")
            }
        } finally {
            if (showIndicator) _isRefreshing.value = false
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
