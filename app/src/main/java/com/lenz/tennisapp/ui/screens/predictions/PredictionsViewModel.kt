package com.lenz.tennisapp.ui.screens.predictions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenz.tennisapp.data.repository.PredictionRepository
import com.lenz.tennisapp.domain.model.PredictionStats
import com.lenz.tennisapp.domain.model.UserPrediction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class PredictionTimeRange(val label: String, val days: Int? = null) {
    ALL("Alle", null),
    TODAY("Heute", 0),
    WEEK("Woche", 7),
    MONTH("Monat", 30)
}

data class PredictionsUiState(
    val predictions: List<UserPrediction> = emptyList(),
    val stats: PredictionStats = PredictionStats(0, 0, 0, 0, 0, 0, 0),
    val timeRange: PredictionTimeRange = PredictionTimeRange.ALL,
    val filteredStats: PredictionStats = PredictionStats(0, 0, 0, 0, 0, 0, 0)
)

@HiltViewModel
class PredictionsViewModel @Inject constructor(
    private val repository: PredictionRepository
) : ViewModel() {

    private val _timeRange = MutableStateFlow(PredictionTimeRange.ALL)

    val uiState: StateFlow<PredictionsUiState> = combine(
        repository.getAllPredictions(),
        repository.getStats(),
        _timeRange
    ) { predictions, stats, range ->
        val filtered = calculateStatsForRange(predictions, range)
        PredictionsUiState(
            predictions = predictions,
            stats = stats,
            timeRange = range,
            filteredStats = filtered
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PredictionsUiState())

    fun setTimeRange(range: PredictionTimeRange) {
        _timeRange.value = range
    }

    fun clearAllPredictions() {
        viewModelScope.launch {
            repository.deleteAllPredictions()
        }
    }

    private fun calculateStatsForRange(predictions: List<UserPrediction>, range: PredictionTimeRange): PredictionStats {
        val cutoffDate = when (range.days) {
            null -> null  // All time
            0 -> LocalDate.now()  // Today
            else -> LocalDate.now().minusDays(range.days.toLong())
        }

        val filtered = if (cutoffDate == null) {
            predictions
        } else {
            predictions.filter { pred ->
                runCatching { LocalDate.parse(pred.matchDate) >= cutoffDate }.getOrDefault(false)
            }
        }

        val totalResolved = filtered.count { !it.isPending }
        val correct = filtered.count { it.isCorrect == true }

        return PredictionStats(
            totalResolved = totalResolved,
            correct = correct,
            pending = filtered.count { it.isPending },
            weeklyResolved = 0,
            weeklyCorrect = 0,
            monthlyResolved = 0,
            monthlyCorrect = 0
        )
    }
}
