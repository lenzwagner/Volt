package com.lenz.tennisapp.ui.screens.airecommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenz.tennisapp.data.api.PredictionMatchDto
import com.lenz.tennisapp.data.repository.TennisRepository
import com.lenz.tennisapp.domain.model.TournamentCategory
import com.lenz.tennisapp.ui.screens.home.CategoryFilter
import com.lenz.tennisapp.ui.screens.home.FormatFilter
import com.lenz.tennisapp.ui.screens.home.TourFilter
import com.lenz.tennisapp.ui.screens.home.matchCategory
import com.lenz.tennisapp.ui.screens.home.matchFormat
import com.lenz.tennisapp.ui.screens.home.matchTour
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs

enum class AiSortMode(val label: String) {
    WEIGHTED("Gewichtet"),
    CONFIDENCE("Konfidenz"),
    ACCURACY("Wahrsch.")
}


data class EnrichedAiPrediction(
    val dto: PredictionMatchDto,
    val matchId: String? = null,
    val category: TournamentCategory? = null,
    val eventType: String = ""
) {
    val isAtp: Boolean get() = eventType.lowercase().let { it.contains("atp") || (it.contains("challenger") && !it.contains("women")) }
    val isWta: Boolean get() = eventType.lowercase().let { it.contains("wta") || it.contains("women") }
    val isDoubles: Boolean get() = eventType.lowercase().let { it.contains("doubles") || it.contains("mixed") }
}

data class AiRecommendationsUiState(
    val enriched: List<EnrichedAiPrediction> = emptyList(),
    val filtered: List<EnrichedAiPrediction> = emptyList(),
    val date: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val sortMode: AiSortMode = AiSortMode.WEIGHTED,
    val tourFilter: TourFilter = TourFilter.ALL,
    val formatFilter: FormatFilter = FormatFilter.ALL,
    val categoryFilter: CategoryFilter = CategoryFilter.ALL
) {
    // kept for backward compat with sortScore extension
    val matches: List<PredictionMatchDto> get() = enriched.map { it.dto }
    val allSorted: List<EnrichedAiPrediction> get() = filtered.sortedByDescending { it.dto.sortScore(sortMode) }
}

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
            // prefer cached matchId from enriched list
            val cached = _uiState.value.enriched.find { it.dto.p1Fullname == p1 && it.dto.p2Fullname == p2 }?.matchId
            val id = cached ?: runCatching { repository.findMatchIdByPlayers(p1, p2) }.getOrNull()
            if (id != null) onFound(id)
        }
    }

    fun setSortMode(mode: AiSortMode) {
        _uiState.value = _uiState.value.copy(sortMode = mode)
    }

    fun setTourFilter(f: TourFilter) {
        val cur = _uiState.value
        _uiState.value = cur.copy(tourFilter = f, filtered = applyFilters(cur.enriched, f, cur.formatFilter, cur.categoryFilter))
    }

    fun setFormatFilter(f: FormatFilter) {
        val cur = _uiState.value
        _uiState.value = cur.copy(formatFilter = f, filtered = applyFilters(cur.enriched, cur.tourFilter, f, cur.categoryFilter))
    }

    fun setCategoryFilter(f: CategoryFilter) {
        val cur = _uiState.value
        _uiState.value = cur.copy(categoryFilter = f, filtered = applyFilters(cur.enriched, cur.tourFilter, cur.formatFilter, f))
    }

    private fun applyFilters(
        list: List<EnrichedAiPrediction>,
        tour: TourFilter,
        format: FormatFilter,
        cat: CategoryFilter
    ): List<EnrichedAiPrediction> {
        return list.filter { e ->
            // reuse home-screen filter logic via a minimal TennisMatch-like proxy
            val eventType = e.eventType
            val tourOk = when (tour) {
                TourFilter.ALL -> true
                TourFilter.ATP -> eventType.lowercase().let { it.contains("atp") || (it.contains("challenger") && !it.contains("women")) }
                TourFilter.WTA -> eventType.lowercase().let { it.contains("wta") || it.contains("women") }
            }
            val fmtOk = when (format) {
                FormatFilter.ALL     -> true
                FormatFilter.SINGLES -> eventType.lowercase().let { !it.contains("doubles") && !it.contains("mixed") }
                FormatFilter.DOUBLES -> eventType.lowercase().let { it.contains("doubles") || it.contains("mixed") }
            }
            val catOk = matchCategory(e.category ?: TournamentCategory.OTHER, cat)
            tourOk && fmtOk && catOk
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = repository.getAiPredictions()
                val data = response?.data
                val dtos = data?.matches ?: emptyList()
                dtos.take(5).forEach { m ->
                    Timber.d("AI_SORT conf=%.3f p1=%.3f p2=%.3f absDiff=%.3f %s vs %s"
                        .format(m.confidence, m.p1Prob, m.p2Prob, abs(m.p1Prob - m.p2Prob), m.p1Fullname, m.p2Fullname))
                }
                val enriched = runCatching { repository.enrichAiPredictions(dtos) }.getOrElse { dtos.map { EnrichedAiPrediction(it) } }
                val cur = _uiState.value
                val filtered = applyFilters(enriched, cur.tourFilter, cur.formatFilter, cur.categoryFilter)
                _uiState.value = cur.copy(
                    enriched = enriched,
                    filtered = filtered,
                    date = data?.date ?: "",
                    isLoading = false
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
