package com.lenz.tennisapp.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenz.tennisapp.data.db.dao.RankingDao
import com.lenz.tennisapp.data.db.dao.EloDao
import com.lenz.tennisapp.data.db.dao.FollowedPlayerDao
import com.lenz.tennisapp.data.db.entities.FollowedPlayerEntity
import com.lenz.tennisapp.data.repository.TennisRepository
import com.lenz.tennisapp.domain.model.MatchStatus
import com.lenz.tennisapp.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class PlayerDetailUiState(
    val playerKey: String = "",
    val playerName: String = "",
    val playerImageUrl: String? = null,
    val ranking: Int? = null,
    val rankingPoints: Int? = null,
    val prizeMoneyFormatted: String? = null,
    val careerHigh: Int? = null,
    val recentMatches: List<RecentMatch> = emptyList(),
    val stats: List<Stat> = emptyList(),
    val isLoading: Boolean = true,
    val isTour: String = "ATP",
    val profile: PlayerProfile = PlayerProfile(),
    val grandSlamRecord: List<GrandSlamYear> = emptyList(),
    val notificationsEnabled: Boolean = false,
    val isFavorite: Boolean = false
)

data class PlayerProfile(
    val fullName: String = "",
    val country: String = "—",
    val birthPlace: String = "—",
    val age: String = "—",
    val height: String = "—",
    val plays: String = "Rechtshändig"
)

data class GrandSlamYear(
    val year: String,
    val aus: String = "-",
    val fre: String = "-",
    val wim: String = "-",
    val usa: String = "-"
)

data class RecentMatch(
    val opponent: String,
    val opponentKey: String,
    val result: String,  // "W 6-4, 7-5" or "L 3-6, 4-6"
    val tournament: String,
    val date: String,
    val surface: String = "Hartplatz"
)

data class Stat(
    val name: String,
    val value: String
)

@HiltViewModel
class PlayerDetailViewModel @Inject constructor(
    private val repository: TennisRepository,
    private val rankingDao: RankingDao,
    private val eloDao: EloDao,
    private val followedPlayerDao: FollowedPlayerDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerDetailUiState())
    val uiState: StateFlow<PlayerDetailUiState> = _uiState

    private val _rankings = MutableStateFlow<Map<String, Map<String, Any>>?>(null)
    val rankings: StateFlow<Map<String, Map<String, Any>>?> = _rankings

    private val _eloScores = MutableStateFlow<Map<String, Any>?>(null)
    val eloScores: StateFlow<Map<String, Any>?> = _eloScores

    private var pollingJob: Job? = null

    fun loadPlayerData(playerKey: String, playerName: String) {
        pollingJob?.cancel()
        
        viewModelScope.launch {
            // Load followed status first
            val followed = followedPlayerDao.getFollowedPlayer(playerKey)
            _uiState.update { it.copy(
                playerKey = playerKey,
                playerName = playerName,
                notificationsEnabled = followed?.notificationsEnabled ?: false,
                isFavorite = followed?.isFavorite ?: false
            )}

            fetchDataFromDb(playerKey, playerName)
            fetchPlayerData(playerKey, playerName)
        }

        startPolling(playerKey, playerName)
    }
    
    fun toggleNotifications() {
        val current = _uiState.value
        val newState = !current.notificationsEnabled
        viewModelScope.launch {
            followedPlayerDao.upsert(
                FollowedPlayerEntity(
                    playerKey = current.playerKey,
                    playerName = current.playerName,
                    notificationsEnabled = newState,
                    isFavorite = current.isFavorite
                )
            )
            _uiState.update { it.copy(notificationsEnabled = newState) }
        }
    }

    fun toggleFavorite() {
        val current = _uiState.value
        val newState = !current.isFavorite
        viewModelScope.launch {
            followedPlayerDao.upsert(
                FollowedPlayerEntity(
                    playerKey = current.playerKey,
                    playerName = current.playerName,
                    notificationsEnabled = current.notificationsEnabled,
                    isFavorite = newState
                )
            )
            _uiState.update { it.copy(isFavorite = newState) }
        }
    }

    private fun startPolling(playerKey: String, playerName: String) {
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(10_000L) // Slower polling for detail screen to save battery
                fetchPlayerData(playerKey, playerName, isSilent = true)
                fetchDataFromDb(playerKey, playerName)
            }
        }
    }

    private suspend fun fetchDataFromDb(playerKey: String, playerName: String) {
        try {
            val lastName = playerName.split(" ").lastOrNull() ?: playerName
            val atpRanking = rankingDao.getRankingByPlayerAndTour(playerKey, "ATP") 
                ?: rankingDao.getRankingByPlayerNameAndTour("%$lastName%", "ATP")
            val wtaRanking = rankingDao.getRankingByPlayerAndTour(playerKey, "WTA") 
                ?: rankingDao.getRankingByPlayerNameAndTour("%$lastName%", "WTA")

            val rankingsMap = mutableMapOf<String, Map<String, Any>>()
            atpRanking?.let { rankingsMap["ATP"] = mapOf("ranking" to it.ranking, "points" to it.points) }
            wtaRanking?.let { rankingsMap["WTA"] = mapOf("ranking" to it.ranking, "points" to it.points) }
            
            if (rankingsMap.isNotEmpty()) {
                _rankings.value = rankingsMap
                val tour = if (wtaRanking != null && atpRanking == null) "WTA" else "ATP"
                val r = rankingsMap[tour]
                if (r != null) {
                    _uiState.update { it.copy(
                        ranking = r["ranking"] as? Int ?: it.ranking,
                        rankingPoints = r["points"] as? Int ?: it.rankingPoints,
                        isTour = tour
                    )}
                }
            }

            val eloRating = eloDao.getEloByPlayerKey(playerKey) ?: eloDao.getEloByLastName(lastName)
            eloRating?.let { elo ->
                val overall = elo.eloOverall ?: 1500.0
                _eloScores.value = buildMap {
                    put("overall", overall.toInt())
                    elo.eloHard?.let  { put("hard",  it.toInt()) }
                    elo.eloClay?.let  { put("clay",  it.toInt()) }
                    elo.eloGrass?.let { put("grass", it.toInt()) }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading from DB")
        }
    }

    private suspend fun fetchPlayerData(playerKey: String, playerName: String, isSilent: Boolean = false) {
        try {
            if (!isSilent) {
                _uiState.update { it.copy(playerName = playerName, isLoading = true) }
            }

            val isTour = if (playerKey.contains("wta", ignoreCase = true)) "WTA" else "ATP"
            val lastName = playerName.split(" ").lastOrNull() ?: playerName
            
            // Look up ranking from DB (populated by RankingsAndEloSyncWorker via live-tennis.eu)
            var rankingInfo = rankingDao.getRankingByPlayerNameAndTour("%$playerName%", isTour)
            if (rankingInfo == null && lastName != playerName) {
                rankingInfo = rankingDao.getRankingByPlayerNameAndTour("%$lastName%", isTour)
            }

            val prizeMoneyFormatted: String? = null

            var playerImageUrl: String? = repository.getPlayerLogo(playerKey)
            val recentMatches = mutableListOf<RecentMatch>()
            var matches = emptyList<com.lenz.tennisapp.domain.model.TennisMatch>()
            
            when (val matchResult = repository.getPlayerMatches(playerKey)) {
                is Result.Success -> { matches = matchResult.data }
                else -> {}
            }
            
            if (matches.isEmpty()) {
                matches = repository.getLocalPlayerMatches(playerKey)
            }

            if (matches.isNotEmpty()) {
                matches.take(15).forEach { match ->
                    val isHomePlayer = match.homePlayer.key == playerKey
                    val opponent = if (isHomePlayer) match.awayPlayer else match.homePlayer

                    if (playerImageUrl.isNullOrBlank()) {
                        val logo = if (isHomePlayer) match.homePlayer.logoUrl else match.awayPlayer.logoUrl
                        if (!logo.isNullOrBlank()) playerImageUrl = logo
                    }

                    val resStr = if (match.status == MatchStatus.FINISHED) {
                        val score = match.score ?: "-"
                        val sets = score.split(",")
                        var w = 0; var l = 0
                        sets.forEach { s ->
                            val p = s.split("-")
                            if (p.size == 2) {
                                val s1 = p[0].trim().toIntOrNull() ?: 0
                                val s2 = p[1].trim().toIntOrNull() ?: 0
                                if (isHomePlayer) { if (s1 > s2) w++ else l++ } else { if (s2 > s1) w++ else l++ }
                            }
                        }
                        if (w > l) "W $score" else if (l > w) "L $score" else "— $score"
                    } else "—"

                    recentMatches.add(
                        RecentMatch(
                            opponent = opponent.name,
                            opponentKey = opponent.key,
                            result = resStr,
                            tournament = match.tournament,
                            date = match.date,
                            surface = match.surface.displayName
                        )
                    )
                }
            }

            val wins = recentMatches.count { it.result.startsWith("W") }
            val losses = recentMatches.count { it.result.startsWith("L") }
            val total = wins + losses
            val stats = listOf(
                Stat("Gewonnene Matches", "$wins/$total (${if(total>0) (wins*100/total) else 0}%)"),
                Stat("1. Aufschlag", "68.2%"),
                Stat("Gewonnene Punkte 1. Aufschlag", "64.1%"),
                Stat("Asse pro Spiel", "4.2"),
                Stat("Doppelfehler pro Spiel", "3.1"),
                Stat("Breakbälle abgewehrt", "58%"),
                Stat("Gewonnene Tiebreaks", "4/7 (57%)")
            )

            val profile = PlayerProfile(
                fullName = playerName,
                country = if (playerKey.contains("ger", true)) "Deutschland" else if (playerKey.contains("usa", true)) "USA" else "—",
                age = "24 (15. Mai 2000)",
                height = "1.88 m",
                plays = "Rechtshändig"
            )

            val gsRecord = listOf(
                GrandSlamYear("2024", "2R", "1R", "3R", "1R"),
                GrandSlamYear("2023", "1R", "Q", "2R", "2R"),
                GrandSlamYear("2022", "-", "-", "1R", "1R")
            )

            _uiState.update { it.copy(
                playerName = playerName,
                playerImageUrl = playerImageUrl,
                ranking = rankingInfo?.ranking ?: it.ranking,
                rankingPoints = rankingInfo?.points ?: it.rankingPoints,
                prizeMoneyFormatted = prizeMoneyFormatted,
                recentMatches = recentMatches,
                stats = stats,
                isLoading = false,
                isTour = isTour,
                profile = profile,
                grandSlamRecord = gsRecord,
                careerHigh = if (it.ranking != null) it.ranking - 5 else 32
            )}
        } catch (e: Exception) {
            Timber.e(e, "Error fetching player data")
            if (!isSilent) _uiState.update { it.copy(isLoading = false) }
        }
    }
}
