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

data class AiRecommendationsUiState(
    val matches: List<PredictionMatchDto> = emptyList(),
    val date: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AiRecommendationsViewModel @Inject constructor(
    private val repository: TennisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiRecommendationsUiState())
    val uiState: StateFlow<AiRecommendationsUiState> = _uiState

    init { load() }

    fun refresh() { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = repository.getAiPredictions()
                val data = response?.data
                _uiState.value = AiRecommendationsUiState(
                    matches = data?.matches?.sortedByDescending { maxOf(it.p1Prob, it.p2Prob) } ?: emptyList(),
                    date = data?.date ?: "",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = AiRecommendationsUiState(isLoading = false, error = "Fehler beim Laden")
            }
        }
    }
}
