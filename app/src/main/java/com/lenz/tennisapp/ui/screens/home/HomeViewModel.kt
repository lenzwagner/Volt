package com.lenz.tennisapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenz.tennisapp.data.datastore.ApiKeyStore
import com.lenz.tennisapp.data.db.dao.RankingDao
import com.lenz.tennisapp.data.repository.PredictionRepository
import com.lenz.tennisapp.data.repository.TennisRepository
import com.lenz.tennisapp.domain.model.*
import com.lenz.tennisapp.domain.prediction.MatchPredictor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val tournaments: List<Tournament> = emptyList(),
    val predictions: Map<String, UserPrediction> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isKeyExpired: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val liveFilter: LiveFilterState = LiveFilterState(),
    val tipsOfTheDay: List<TipOfTheDay> = emptyList(),
    val tipsDismissed: Boolean = false,
    val tipThreshold: Float = 0.75f,
    val tipCount: Int = 5,
    val hasAnyTips: Boolean = false
)

data class TipOfTheDay(
    val match: TennisMatch,
    val aiProbHome: Float,  // 0.0 to 1.0
    val confidence: Float = maxOf(aiProbHome, 1f - aiProbHome)  // Always >0.5, only >0.75 shown
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TennisRepository,
    private val predictionRepository: PredictionRepository,
    private val keyStore: ApiKeyStore,
    private val predictor: MatchPredictor,
    private val rankingDao: RankingDao
) : ViewModel() {

    private val _selectedDate  = MutableStateFlow(LocalDate.now())
    private val _isLoading     = MutableStateFlow(false)
    private val _error         = MutableStateFlow<String?>(null)
    private val _isKeyExpired  = MutableStateFlow(false)
    private val _liveFilter    = MutableStateFlow(LiveFilterState())
    private val _tipDismissed  = MutableStateFlow(false)
    private val _tipsOfTheDay  = MutableStateFlow<List<TipOfTheDay>>(emptyList())
    private val _tipThreshold  = MutableStateFlow(0.75f)
    private val _tipCount      = MutableStateFlow(5)
    private val _liveOnly      = MutableStateFlow(false)

    val liveOnly: StateFlow<Boolean> = _liveOnly

    val liveMatchCount: StateFlow<Int> = repository.getLiveMatches()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun toggleLiveOnly() { _liveOnly.value = !_liveOnly.value }

    private val _allRankings = rankingDao.getAllRankings()
        .map { list -> 
            // Create a map where key is a normalized version of the name
            list.associateBy({ it.playerName.lowercase().trim() }, { it.ranking }) 
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // Live-specific state using getLiveMatches() (all matches with isLive=1, date-independent)
    val liveUiState: StateFlow<HomeUiState> = combine(
        repository.getLiveMatches().map { matches ->
            matches.map { match ->
                val leagueId = match.leagueId
                Tournament(
                    id = leagueId,
                    name = match.tournament,
                    category = match.tournamentCategory,
                    surface = match.surface,
                    matches = listOf(match)
                )
            }.groupBy { it.id }.map { (_, ts) ->
                ts.first().copy(matches = ts.flatMap { it.matches })
            }
        },
        predictionRepository.getPredictionMap(),
        _isLoading,
        _error,
        _isKeyExpired,
        _liveFilter,
        keyStore.tennisKeyExpired
    ) { args ->
        val tournaments = args[0] as List<Tournament>
        val preds       = args[1] as Map<String, UserPrediction>
        val loading     = args[2] as Boolean
        val error       = args[3] as? String
        val keyLocal    = args[4] as Boolean
        val filter      = args[5] as LiveFilterState
        val keyExpired  = args[6] as Boolean

        HomeUiState(
            tournaments  = tournaments,
            predictions  = preds,
            isLoading    = loading,
            error        = error,
            isKeyExpired = keyLocal || keyExpired,
            selectedDate = LocalDate.now(),
            liveFilter   = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedDate.flatMapLatest { date -> repository.getMatchesForDate(date) },
        predictionRepository.getPredictionMap(),
        _isLoading,
        _error,
        _isKeyExpired,
        _liveFilter,
        _tipDismissed,
        _tipsOfTheDay,
        _allRankings,
        keyStore.tennisKeyExpired,
        _tipThreshold,
        _tipCount
    ) { args ->
        val tournaments  = args[0] as List<Tournament>
        val preds        = args[1] as Map<String, UserPrediction>
        val loading      = args[2] as Boolean
        val error        = args[3] as? String
        val keyLocal     = args[4] as Boolean
        val filter       = args[5] as LiveFilterState
        val tipDismissed = args[6] as Boolean
        val tips         = args[7] as List<TipOfTheDay>
        val rankings     = args[8] as Map<String, Int>
        val keyExpired   = args[9] as Boolean
        val threshold    = args[10] as Float
        val count        = args[11] as Int
        val date         = _selectedDate.value

        // Helper to find ranking by name - aggressive fuzzy matching
        fun getRank(name: String): Int? {
            val normalized = name.lowercase().trim()
                .replace(".", "")
                .replace("-", " ")
            
            val nameParts = normalized.split(" ").filter { it.length > 0 } // Allow single chars
            if (nameParts.isEmpty()) return null
            
            val lastName = nameParts.last()
            
            // 1. Try exact match on normalized name
            rankings[normalized]?.let { return it }
            
            // 2. Try matching by last name + any other part (initial or first name)
            rankings.entries.find { (rankName, _) ->
                val rName = rankName.lowercase().replace("-", " ")
                rName.contains(lastName) && nameParts.any { part -> 
                    part != lastName && rName.contains(part) 
                }
            }?.value?.let { return it }

            // 3. Last name only fallback (if name is unique enough)
            if (lastName.length > 3) {
                val matches = rankings.entries.filter { it.key.lowercase().contains(lastName) }
                if (matches.size == 1) return matches[0].value
            }

            return null
        }

        // Enrich tournaments with rankings
        val enrichedTournaments = tournaments.map { t ->
            t.copy(matches = t.matches.map { m ->
                m.copy(
                    homePlayer = m.homePlayer.copy(ranking = getRank(m.homePlayer.name) ?: m.homePlayer.ranking),
                    awayPlayer = m.awayPlayer.copy(ranking = getRank(m.awayPlayer.name) ?: m.awayPlayer.ranking)
                )
            })
        }

        HomeUiState(
            tournaments  = enrichedTournaments,
            predictions  = preds,
            isLoading    = loading,
            error        = error,
            isKeyExpired = keyLocal || keyExpired,
            selectedDate = date,
            liveFilter   = filter,
            tipsOfTheDay = if (tipDismissed) emptyList() else tips.filter { it.confidence >= threshold }.take(count),
            tipsDismissed = tipDismissed,
            tipThreshold = threshold,
            tipCount = count,
            hasAnyTips = tips.isNotEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            keyStore.setTennisKeyExpired(false)
        }
        startPolling()
        
        // Update tips when tournaments change
        viewModelScope.launch {
            _selectedDate.flatMapLatest { date -> repository.getMatchesForDate(date) }
                .collect { tournaments ->
                    updateTips(tournaments)
                }
        }
    }

    /** Polls livescores every 5 seconds while the app is in the foreground. */
    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(5_000L)
                repository.refreshLivescores()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = repository.refreshMatches(_selectedDate.value)
            if (result is Result.Error) {
                _error.value = result.message
                _isKeyExpired.value = result.isKeyExpired
            }

            // Also refresh live matches
            repository.refreshLivescores()

            // Wait for Room to emit updated values
            delay(1000)
            _isLoading.value = false
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        refresh()
    }

    fun dismissKeyExpiredBanner() { _isKeyExpired.value = false }

    fun toggleTourFilter(tour: TourType) {
        _liveFilter.update { s ->
            s.copy(tours = if (tour in s.tours) s.tours - tour else s.tours + tour)
        }
    }

    fun toggleCategoryFilter(cat: FilterCategory) {
        _liveFilter.update { s ->
            s.copy(categories = if (cat in s.categories) s.categories - cat else s.categories + cat)
        }
    }

    fun toggleMatchTypeFilter(matchType: MatchType) {
        _liveFilter.update { s ->
            s.copy(matchTypes = if (matchType in s.matchTypes) s.matchTypes - matchType else s.matchTypes + matchType)
        }
    }

    fun clearLiveFilters() { _liveFilter.value = LiveFilterState() }

    fun dismissTipsOfTheDay() { _tipDismissed.value = true }

    fun setTipThreshold(threshold: Float) {
        _tipThreshold.value = threshold
    }

    fun setTipCount(count: Int) {
        _tipCount.value = count
    }

    private suspend fun updateTips(tournaments: List<Tournament>) {
        val allMatches = tournaments.flatMap { it.matches }
            .filter { it.status != MatchStatus.FINISHED }

        val allTips = allMatches.map { match ->
            val prediction = predictor.predict(match, null)
            val homeProb = prediction.player1WinProbability
            
            TipOfTheDay(
                match = match,
                aiProbHome = homeProb,
                confidence = maxOf(homeProb, 1f - homeProb)
            )
        }.sortedByDescending { it.confidence }
            .take(10) // Take more, filter in UI
            .filter { it.confidence > 0.55f } // Lower base threshold

        _tipsOfTheDay.value = allTips
    }

    fun predict(
        matchId: String, matchDate: String, tournamentName: String,
        homePlayerKey: String, homePlayerName: String,
        awayPlayerKey: String, awayPlayerName: String,
        winnerKey: String, winnerName: String
    ) {
        viewModelScope.launch {
            // Note: winnerKey is the Player.key from TennisMatch (e.g., "zverev-a")
            // Save both for matching against match results
            predictionRepository.savePrediction(
                matchId             = matchId,
                predictedWinnerKey  = winnerKey,  // Stored player key for display
                predictedWinnerName = winnerName,
                homePlayerKey       = homePlayerKey,
                homePlayerName      = homePlayerName,
                awayPlayerKey       = awayPlayerKey,
                awayPlayerName      = awayPlayerName,
                matchDate           = matchDate,
                tournamentName      = tournamentName
            )
        }
    }
}
