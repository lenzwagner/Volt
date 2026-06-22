package com.lenz.tennisapp.ui.screens.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenz.tennisapp.data.repository.TennisRepository
import com.lenz.tennisapp.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TournamentDetailUiState(
    val tournamentName: String = "",
    val rounds: List<TournamentRound> = emptyList(),
    val tournamentInfo: TournamentInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TournamentInfo(
    val points: String,
    val surface: Surface,
    val category: TournamentCategory,
    val location: String? = null,
    val lastWinner: String? = null
)

data class TournamentRound(
    val name: String,
    val matches: List<TennisMatch>
)

@HiltViewModel
class TournamentDetailViewModel @Inject constructor(
    private val repository: TennisRepository
) : ViewModel() {

    private val _leagueId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)

    private val _lastYearWinner = MutableStateFlow<String?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TournamentDetailUiState> = _leagueId
        .filterNotNull()
        .flatMapLatest { leagueId ->
            repository.getTournamentMatches(leagueId)
                .combine(combine(_isLoading, _isRefreshing, _lastYearWinner) { l, r, w -> Triple(l || r, w, null) }) { matches, extra ->
                    val loading = extra.first
                    val lastWinner = extra.second as? String
                    
                    val rounds = groupMatchesByRound(matches)
                    val info = if (matches.isNotEmpty()) {
                        val first = matches.first()
                        TournamentInfo(
                            points = getPoints(first.tournamentCategory),
                            surface = first.surface,
                            category = first.tournamentCategory,
                            lastWinner = lastWinner ?: "Wird geladen..."
                        )
                    } else null
                    
                    TournamentDetailUiState(
                        tournamentName = matches.firstOrNull()?.tournament ?: "",
                        rounds = rounds,
                        tournamentInfo = info,
                        isLoading = loading && matches.isEmpty()
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TournamentDetailUiState(isLoading = true))

    fun loadTournament(leagueId: String, name: String) {
        if (_leagueId.value == leagueId) return

        _leagueId.value = leagueId
        _lastYearWinner.value = null
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshTournamentMatches(leagueId)
            
            // Try to fetch last year winner
            val winner = repository.getLastYearWinner(leagueId)
            _lastYearWinner.value = winner ?: "Nicht verfügbar"

            _isLoading.value = false
        }
    }

    private fun groupMatchesByRound(matches: List<TennisMatch>): List<TournamentRound> {
        return matches
            .groupBy { it.round ?: "Andere" }
            .map { (roundName, roundMatches) ->
                TournamentRound(
                    name = translateRound(roundName),
                    matches = roundMatches.sortedBy { it.date + " " + it.time }
                )
            }
            .sortedBy { getRoundOrder(it.name) }
    }

    private fun translateRound(round: String): String {
        return when {
            round.contains("Final", ignoreCase = true) && !round.contains("Semi", ignoreCase = true) && !round.contains("Quarter", ignoreCase = true) -> "Finale"
            round.contains("Semi", ignoreCase = true) -> "Halbfinale"
            round.contains("Quarter", ignoreCase = true) -> "Viertelfinale"
            round.contains("Round of 16", ignoreCase = true) -> "Achtelfinale"
            round.contains("Round of 32", ignoreCase = true) -> "2. Runde"
            round.contains("Round of 64", ignoreCase = true) -> "1. Runde"
            round.contains("Qualification", ignoreCase = true) -> "Qualifikation"
            else -> round
        }
    }

    private fun getRoundOrder(roundName: String): Int {
        return when (roundName) {
            "Finale" -> 0
            "Halbfinale" -> 1
            "Viertelfinale" -> 2
            "Achtelfinale" -> 3
            "2. Runde" -> 4
            "1. Runde" -> 5
            "Qualifikation" -> 6
            else -> 10
        }
    }

    private fun getPoints(category: TournamentCategory): String {
        return when (category) {
            TournamentCategory.GRAND_SLAM      -> "2000"
            TournamentCategory.ATP_MASTERS_1000, 
            TournamentCategory.WTA_1000        -> "1000"
            TournamentCategory.ATP_500, 
            TournamentCategory.WTA_500         -> "500"
            TournamentCategory.ATP_250, 
            TournamentCategory.WTA_250         -> "250"
            TournamentCategory.WTA_125         -> "125"
            TournamentCategory.CHALLENGER_175  -> "175"
            TournamentCategory.CHALLENGER_125  -> "125"
            TournamentCategory.CHALLENGER_100  -> "100"
            TournamentCategory.CHALLENGER_75   -> "75"
            TournamentCategory.CHALLENGER_50   -> "50"
            TournamentCategory.CHALLENGER      -> "125"
            else                               -> "—"
        }
    }
}
