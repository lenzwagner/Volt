package com.lenz.tennisapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenz.tennisapp.data.repository.PlayerRepository
import com.lenz.tennisapp.data.repository.PredictionRepository
import com.lenz.tennisapp.data.repository.TennisRepository
import com.lenz.tennisapp.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HomeUiState(
    val tournaments: List<Tournament>? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val networkFetchDone: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TennisRepository,
    private val playerRepository: PlayerRepository,
    private val predictionRepository: PredictionRepository
) : ViewModel() {

    private val _tourFilter = MutableStateFlow(TourFilter.ALL)
    private val _formatFilter = MutableStateFlow(FormatFilter.ALL)
    private val _categoryFilter = MutableStateFlow(CategoryFilter.ALL)
    private val _liveFilter = MutableStateFlow(false)
    private val _finishedFilter = MutableStateFlow(false)

    val tourFilter: StateFlow<TourFilter> = _tourFilter.asStateFlow()
    val formatFilter: StateFlow<FormatFilter> = _formatFilter.asStateFlow()
    val categoryFilter: StateFlow<CategoryFilter> = _categoryFilter.asStateFlow()
    val liveFilter: StateFlow<Boolean> = _liveFilter.asStateFlow()
    val finishedFilter: StateFlow<Boolean> = _finishedFilter.asStateFlow()

    private val _rawToday = MutableStateFlow<List<Tournament>?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val _lastError = MutableStateFlow<String?>(null)
    private val _networkFetchDone = MutableStateFlow(false)

    val liveCount: StateFlow<Int> = _rawToday.map { tournaments ->
        tournaments?.sumOf { t -> t.matches.count { it.status == MatchStatus.LIVE } } ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isDataReady: StateFlow<Boolean> = _rawToday
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val todayUiState: StateFlow<HomeUiState> = combine(
        _rawToday,
        _tourFilter,
        _formatFilter,
        _categoryFilter,
        _liveFilter,
        _finishedFilter,
        _isRefreshing,
        _lastError,
        _networkFetchDone
    ) { args ->
        val tournaments = args[0] as List<Tournament>?
        val tour = args[1] as TourFilter
        val format = args[2] as FormatFilter
        val category = args[3] as CategoryFilter
        val liveOnly = args[4] as Boolean
        val finishedOnly = args[5] as Boolean
        val refreshing = args[6] as Boolean
        val lastErr = args[7] as String?
        val networkDone = args[8] as Boolean

        if (tournaments == null && !refreshing) {
            HomeUiState(isLoading = true, error = lastErr, networkFetchDone = networkDone)
        } else {
            var filtered = filterTournaments(tournaments ?: emptyList(), tour, format, category)

            if (finishedOnly) {
                // FT view: only finished matches
                filtered = filtered.map { t ->
                    t.copy(matches = t.matches.filter { it.status == MatchStatus.FINISHED })
                }.filter { it.matches.isNotEmpty() }
            } else {
                // Main view: live + open only (no finished)
                filtered = filtered.map { t ->
                    t.copy(matches = t.matches.filter { it.status != MatchStatus.FINISHED })
                }.filter { it.matches.isNotEmpty() }
                if (liveOnly) {
                    filtered = filtered.map { t ->
                        t.copy(matches = t.matches.filter { it.status == MatchStatus.LIVE })
                    }.filter { it.matches.isNotEmpty() }
                }
            }

            if (filtered.isNotEmpty() || networkDone) {
                HomeUiState(
                    tournaments = sortedForToday(filtered),
                    isRefreshing = refreshing,
                    error = lastErr,
                    networkFetchDone = networkDone
                )
            } else {
                HomeUiState(isLoading = true, error = lastErr, networkFetchDone = networkDone)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState(isLoading = true))

    init {
        refresh()
        startPolling()
        startPrefetch()
        viewModelScope.launch { predictionRepository.resolvePending() }
        viewModelScope.launch { runCatching { repository.getAiPredictions() } }
    }

    // After the main page has data, warm match-detail caches (H2H + AI predictions,
    // no odds API) in the background so opening a match is instant.
    private fun startPrefetch() {
        viewModelScope.launch {
            val tournaments = _rawToday.filterNotNull().first()
            // Order by likelihood of being tapped: live → upcoming → finished
            val ids = tournaments
                .flatMap { it.matches }
                .sortedBy { m ->
                    when (m.status) {
                        MatchStatus.LIVE -> 0
                        MatchStatus.NOT_STARTED, MatchStatus.TBD -> 1
                        else -> 2
                    }
                }
                .map { it.id }
            if (ids.isNotEmpty()) {
                try {
                    repository.prefetchMatchDetails(ids)
                } catch (e: Exception) {
                    Timber.w(e, "Prefetch batch failed")
                }
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(10_000)
                try {
                    val result = repository.fetchOnce()
                    _rawToday.value = result
                } catch (e: Exception) {
                    Timber.w(e, "Poll failed")
                }
            }
        }
    }

    fun setTourFilter(filter: TourFilter) { _tourFilter.value = filter }
    fun setFormatFilter(filter: FormatFilter) { _formatFilter.value = filter }
    fun setCategoryFilter(filter: CategoryFilter) { _categoryFilter.value = filter }
    fun toggleLiveFilter() {
        _liveFilter.value = !_liveFilter.value
        if (_liveFilter.value) _finishedFilter.value = false
    }

    fun toggleFinishedFilter() {
        _finishedFilter.value = !_finishedFilter.value
        if (_finishedFilter.value) _liveFilter.value = false
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _lastError.value = null
            try {
                val dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                Timber.d("Refreshing matches for date: $dateStr")
                val result = repository.fetchOnce()
                Timber.d("Refresh fetchOnce returned ${result.size} tournaments")
                _rawToday.value = result
                if (result.isEmpty()) {
                    _lastError.value = "Keine Turniere für heute gefunden."
                }
                // Sync live rankings in background so ranking badges appear without manual sync
                launch {
                    try { playerRepository.syncLiveRankings() } catch (_: Exception) {}
                    // Re-read tournaments after rankings are written
                    val enriched = repository.fetchOnce()
                    if (enriched.isNotEmpty()) _rawToday.value = enriched
                }
            } catch (e: Exception) {
                Timber.e(e, "Refresh failed")
                _lastError.value = buildDetailedError(e)
            } finally {
                _isRefreshing.value = false
                _networkFetchDone.value = true
            }
        }
    }

    private fun buildDetailedError(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("401") || msg.contains("403") -> "🔑 API-Key abgelaufen."
            msg.contains("404") -> "⚠️ API-Endpunkt nicht gefunden."
            msg.contains("429") -> "⏱ API-Limit erreicht."
            msg.contains("UnknownHost") -> "📡 Kein Internet."
            else -> "❌ Fehler: ${e.localizedMessage}"
        }
    }

    private fun sortedForToday(list: List<Tournament>): List<Tournament> {
        return list.sortedWith(
            compareByDescending<Tournament> { it.category.points }
                .thenBy {
                    when {
                        it.type == "Doubles" -> 2
                        it.category.name.startsWith("WTA") -> 1
                        else -> 0
                    }
                }
                .thenBy { it.name }
        ).map { t ->
            t.copy(matches = t.matches.sortedWith(
                compareBy<TennisMatch> { matchSortOrder(it.status) }
                    .thenBy { it.time }
            ))
        }
    }

    private fun matchSortOrder(status: MatchStatus) = when (status) {
        MatchStatus.LIVE -> 0
        MatchStatus.NOT_STARTED, MatchStatus.TBD -> 1
        MatchStatus.FINISHED -> 2
        else -> 3
    }
}
