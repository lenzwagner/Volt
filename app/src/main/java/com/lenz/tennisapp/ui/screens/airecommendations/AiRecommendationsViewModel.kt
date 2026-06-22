package com.lenz.tennisapp.ui.screens.airecommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenz.tennisapp.data.api.PredictionMatchDto
import com.lenz.tennisapp.data.repository.TennisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

enum class AiSortMode(val label: String) {
    WEIGHTED("Gewichtet"),
    CONFIDENCE("Konfidenz"),
    ACCURACY("Wahrsch.")
}

data class AiRecommendationsUiState(
    val matches: List<PredictionMatchDto> = emptyList(),
    val date: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val sortMode: AiSortMode = AiSortMode.WEIGHTED,
    val allSorted: List<PredictionMatchDto> = emptyList()
)

@HiltViewModel
class AiRecommendationsViewModel @Inject constructor(
    private val repository: TennisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiRecommendationsUiState())
    val uiState: StateFlow<AiRecommendationsUiState> = _uiState

    init { load() }

    fun refresh() { load() }

    fun findAndNavigate(p1: String, p2: String, onFound: (String) -> Unit) {
        viewModelScope.launch {
            val id = runCatching { repository.findMatchIdByPlayers(p1, p2) }.getOrNull()
            if (id != null) onFound(id)
        }
    }

    fun setSortMode(mode: AiSortMode) {
        val sorted = _uiState.value.matches.sortedByDescending { it.sortScore(mode) }
        _uiState.value = _uiState.value.copy(sortMode = mode, allSorted = sorted)
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = repository.getAiPredictions()
                val data = response?.data
                val matches = data?.matches ?: emptyList()
                val mode = _uiState.value.sortMode
                _uiState.value = _uiState.value.copy(
                    matches = matches,
                    date = data?.date ?: "",
                    isLoading = false,
                    allSorted = matches.sortedByDescending { it.sortScore(mode) }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Fehler beim Laden")
            }
        }
    }
}

fun PredictionMatchDto.sortScore(mode: AiSortMode): Float = when (mode) {
    AiSortMode.WEIGHTED   -> 0.45f * confidence + 0.55f * abs(p1Prob - p2Prob)
    AiSortMode.CONFIDENCE -> confidence
    AiSortMode.ACCURACY   -> abs(p1Prob - p2Prob)
}
