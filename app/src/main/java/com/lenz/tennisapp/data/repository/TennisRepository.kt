package com.lenz.tennisapp.data.repository

import com.lenz.tennisapp.data.api.OddsApiService
import com.lenz.tennisapp.data.api.TennisApiService
import com.lenz.tennisapp.data.api.dto.*
import com.lenz.tennisapp.data.datastore.ApiKeyStore
import com.lenz.tennisapp.data.db.dao.*
import com.lenz.tennisapp.data.scraper.PlayerMatcher
import com.lenz.tennisapp.data.db.entities.EloRatingEntity
import com.lenz.tennisapp.data.db.entities.MatchEntity
import com.lenz.tennisapp.data.db.entities.NotifiedMatchEntity
import com.lenz.tennisapp.domain.model.*
import com.lenz.tennisapp.domain.prediction.MatchPredictor
import com.lenz.tennisapp.notification.NotificationHelper
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TennisRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tennisApi: TennisApiService,
    private val oddsApi: OddsApiService,
    private val matchDao: MatchDao,
    private val eloDao: EloDao,
    private val rankingDao: RankingDao,
    private val predictionDao: PredictionDao,
    private val followedPlayerDao: FollowedPlayerDao,
    private val notifiedMatchDao: NotifiedMatchDao,
    private val playerDao: PlayerDao,
    private val playerMatcher: PlayerMatcher,
    private val keyStore: ApiKeyStore,
    private val predictor: MatchPredictor
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getMatchesForDate(date: LocalDate): Flow<List<Tournament>> =
        matchDao.getMatchesForDate(date.format(dateFormatter))
            .map { entities -> groupIntoTournaments(entities) }

    fun getLiveMatches(): Flow<List<TennisMatch>> =
        matchDao.getLiveMatches().map { entities ->
            val now = System.currentTimeMillis()
            entities
                .filter { entity ->
                    // Keep matches that:
                    // 1. Are marked as live AND updated recently (< 3 hours)
                    // 2. Have Set status (Set 1, Set 2, etc) - actively playing
                    val threeHoursMs = 3 * 60 * 60 * 1000L
                    val recentlyUpdated = (now - entity.cachedAt) < threeHoursMs
                    val isPlayingSet = entity.status.contains("Set", ignoreCase = true) ||
                                     entity.status.contains("Love", ignoreCase = true)

                    entity.isLive && (recentlyUpdated || isPlayingSet)
                }
                .map { it.toDomain() }
                .sortedBy { it.time }
        }

    fun getTournamentMatches(leagueId: String): Flow<List<TennisMatch>> =
        matchDao.getMatchesByLeagueId(leagueId).map { entities -> entities.map { it.toDomain() } }

    // Suspend version of get all matches
    suspend fun getAllMatches(): List<TennisMatch> {
        return try {
            matchDao.getAllMatches().first().map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAllMatchesFlow(): Flow<List<TennisMatch>> =
        matchDao.getAllMatches().map { entities -> entities.map { it.toDomain() } }

    suspend fun refreshMatches(date: LocalDate): Result<Unit> {
        val key = keyStore.tennisApiKey.first()
        if (key.isBlank()) return Result.Error("Kein API-Tennis Key gesetzt", isKeyExpired = true)

        return try {
            val dateStr = date.format(dateFormatter)

            // First load livescores to get all finished matches
            val liveResponse = tennisApi.getLivescores(apiKey = key)
            if (liveResponse.success == 1) {
                val entities = liveResponse.result?.mapNotNull { it.toEntity() } ?: emptyList()
                matchDao.upsertMatches(entities)
            }

            // Then load fixtures for the specific date
            val response = tennisApi.getFixtures(dateStart = dateStr, dateStop = dateStr, apiKey = key)

            if (response.success == 0) {
                val errorMsg = response.error ?: "Unbekannter Fehler"
                val isKeyError = errorMsg.contains("key", ignoreCase = true) ||
                        errorMsg.contains("limit", ignoreCase = true) ||
                        errorMsg.contains("expired", ignoreCase = true) ||
                        errorMsg.contains("invalid", ignoreCase = true)
                if (isKeyError) keyStore.setTennisKeyExpired(true)
                return Result.Error(errorMsg, isKeyExpired = isKeyError)
            }

            keyStore.setTennisKeyExpired(false)
            val entities = response.result?.mapNotNull { it.toEntity() } ?: emptyList()
            matchDao.upsertMatches(entities)

            // Register new players in the central player registry
            val tourType = response.result?.firstOrNull()?.eventTypeType ?: ""
            val tour = when {
                "Atp" in tourType || "Challenger Men" in tourType -> "ATP"
                "Wta" in tourType || "Challenger Women" in tourType -> "WTA"
                else -> null
            }
            val playerEntities = response.result?.mapNotNull { dto ->
                val fKey = dto.firstPlayerKey ?: return@mapNotNull null
                val sKey = dto.secondPlayerKey ?: return@mapNotNull null
                val fName = dto.firstPlayer ?: return@mapNotNull null
                val sName = dto.secondPlayer ?: return@mapNotNull null
                listOf(
                    playerMatcher.buildPlayerEntity(fKey.toString(), fName, tour),
                    playerMatcher.buildPlayerEntity(sKey.toString(), sName, tour)
                )
            }?.flatten() ?: emptyList()
            playerDao.insertAllIfAbsent(playerEntities)

            // Resolve pending user predictions for finished matches
            entities
                .filter { 
                    val cat = TournamentCategory.valueOf(it.tournamentCategory)
                    (it.status == "Finished" || it.status == "Retired" || it.status == "Walkover" || isMatchFinished(it.finalResult, cat, it.leagueId)) 
                    && (it.winnerId != null || getWinnerFromScore(it) != null)
                }
                .forEach { entity ->
                    predictionDao.getPrediction(entity.id)?.let { pred ->
                        if (pred.isCorrect == null) {
                            val winnerKey = entity.winnerId ?: getWinnerFromScore(entity)!!
                            val isWinnerHome = winnerKey == entity.homePlayerKey
                            val winnerName = if (isWinnerHome) entity.homePlayer else entity.awayPlayer
                            // Compare by player name since predictedWinnerKey is a string player key
                            val correct = pred.predictedWinnerName == winnerName
                            predictionDao.resolveResult(entity.id, correct, winnerKey, winnerName)
                        }
                    }
                }

            // Update ELO for newly finished matches
            entities
                .filter { 
                    val cat = TournamentCategory.valueOf(it.tournamentCategory)
                    it.status == "Finished" || it.status == "Retired" || isMatchFinished(it.finalResult, cat, it.leagueId)
                }
                .forEach { entity ->
                    val winnerId = entity.winnerId ?: getWinnerFromScore(entity)
                    val homeWon = winnerId == entity.homePlayerKey
                    if (homeWon) {
                        predictor.updateEloFromResult(
                            entity.homePlayerKey, entity.homePlayer,
                            entity.awayPlayerKey, entity.awayPlayer,
                            entity.surface?.let { runCatching { Surface.valueOf(it) }.getOrNull() } ?: Surface.HARD,
                            TournamentCategory.valueOf(entity.tournamentCategory)
                        )
                    } else if (winnerId == entity.awayPlayerKey) {
                        predictor.updateEloFromResult(
                            entity.awayPlayerKey, entity.awayPlayer,
                            entity.homePlayerKey, entity.homePlayer,
                            entity.surface?.let { runCatching { Surface.valueOf(it) }.getOrNull() } ?: Surface.HARD,
                            TournamentCategory.valueOf(entity.tournamentCategory)
                        )
                    }
                }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Netzwerkfehler")
        }
    }

    /** Silent refresh — updates DB in the background without touching loading/error state. */
    suspend fun refreshSilent(date: LocalDate) {
        try {
            val key = keyStore.tennisApiKey.first()
            if (key.isBlank()) return
            val dateStr = date.format(dateFormatter)
            
            // 1. Load fixtures for the specific date to get the latest comprehensive data (finished scores)
            val fixtures = tennisApi.getFixtures(dateStart = dateStr, dateStop = dateStr, apiKey = key)
            if (fixtures.success == 1) {
                val entities = fixtures.result?.mapNotNull { it.toEntity() } ?: emptyList()
                matchDao.upsertMatches(entities)
            }

            // 2. Load livescores for the absolute latest updates on running matches
            val live = tennisApi.getLivescores(apiKey = key)
            if (live.success == 1) {
                val entities = live.result?.mapNotNull { it.toEntity() } ?: emptyList()
                matchDao.upsertMatches(entities)
                
                // Check for notifications for followed players
                checkForNotifications(entities)
                
                // Resolve predictions for matches that just finished
                entities.filter { 
                    val cat = TournamentCategory.valueOf(it.tournamentCategory)
                    (it.status == "Finished" || it.status == "Retired" || isMatchFinished(it.finalResult, cat, it.leagueId)) 
                    && (it.winnerId != null || getWinnerFromScore(it) != null)
                }.forEach { entity ->
                    predictionDao.getPrediction(entity.id)?.let { pred ->
                        if (pred.isCorrect == null) {
                            val winnerKey = entity.winnerId ?: getWinnerFromScore(entity)!!
                            val correct = pred.predictedWinnerKey == winnerKey
                            val winnerName = if (winnerKey == entity.homePlayerKey) entity.homePlayer else entity.awayPlayer
                            predictionDao.resolveResult(entity.id, correct, winnerKey, winnerName)
                        }
                    }
                }
            }
        } catch (_: Exception) { /* silently swallow — user never sees these errors */ }
    }

    suspend fun refreshLivescores(): Result<Unit> {
        val key = keyStore.tennisApiKey.first()
        return try {
            val response = tennisApi.getLivescores(apiKey = key)
            if (response.success == 0) return Result.Error(response.error ?: "Fehler")
            val entities = response.result?.mapNotNull { it.toEntity() } ?: emptyList()
            matchDao.upsertMatches(entities)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Netzwerkfehler")
        }
    }

    suspend fun refreshTournamentMatches(leagueId: String): Result<Unit> {
        val key = keyStore.tennisApiKey.first()
        if (key.isBlank()) return Result.Error("Kein API Key", isKeyExpired = true)
        
        val numericLeagueId = leagueId.split("_").firstOrNull() ?: return Result.Error("Ungültige League ID")

        return try {
            val response = tennisApi.getFixturesByLeague(apiKey = key, leagueId = numericLeagueId)
            if (response.success == 0) {
                return Result.Error(response.error ?: "Fehler")
            }

            val entities = response.result?.mapNotNull { it.toEntity() } ?: emptyList()
            matchDao.upsertMatches(entities)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Netzwerkfehler")
        }
    }

    suspend fun getLastYearWinner(leagueId: String): String? {
        val key = keyStore.tennisApiKey.first()
        if (key.isBlank()) return null
        
        val numericLeagueId = leagueId.split("_").firstOrNull() ?: return null
        
        // Try to find the winner of the previous year (roughly 365 days ago)
        val lastYear = LocalDate.now().minusYears(1).year
        val from = "$lastYear-01-01"
        val to = "$lastYear-12-31"

        return try {
            val response = tennisApi.getFixturesByLeague(apiKey = key, leagueId = numericLeagueId, from = from, to = to)
            if (response.success == 1) {
                // Find the match that is a Final and is Finished
                val finalMatch = response.result?.find { 
                    it.tournamentRound?.contains("Final", ignoreCase = true) == true && 
                    it.status.equals("Finished", ignoreCase = true) 
                }
                
                finalMatch?.let { m ->
                    when (m.winner) {
                        "First Player" -> m.firstPlayer
                        "Second Player" -> m.secondPlayer
                        else -> null
                    }
                }
            } else null
        } catch (e: Exception) {
            Timber.e(e, "Error fetching last year winner for $leagueId")
            null
        }
    }

    suspend fun getPlayerMatches(playerKey: String): Result<List<TennisMatch>> {
        val key = keyStore.tennisApiKey.first()
        if (key.isBlank()) return Result.Error("Kein API-Tennis Key gesetzt", isKeyExpired = true)

        // Validate player key format (don't allow doubles or invalid keys)
        if (playerKey.isBlank() || playerKey.contains("/")) {
            return Result.Error("Ungültiger Spielerschlüssel: $playerKey")
        }

        return try {
            val response = tennisApi.getPlayerMatches(playerKey = playerKey, apiKey = key)
            if (response.success == 0) {
                val errorMsg = response.error ?: "Fehler beim Abrufen von Matches"
                val isKeyError = errorMsg.contains("key", ignoreCase = true)
                if (isKeyError) keyStore.setTennisKeyExpired(true)
                return Result.Error(errorMsg, isKeyExpired = isKeyError)
            }

            keyStore.setTennisKeyExpired(false)
            val matches = response.result?.map { dto ->
                // Convert DTO -> Entity -> Domain
                val entity = dto.toEntity()
                entity.toDomain()
            } ?: emptyList()
            Result.Success(matches)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching matches for player: $playerKey")
            Result.Error(e.message ?: "Netzwerkfehler")
        }
    }

    suspend fun getPlayerLogo(playerKey: String): String? {
        return try {
            val match = matchDao.getMatchesByPlayerKeyList(playerKey).firstOrNull()
            if (match?.homePlayerKey == playerKey) {
                match.firstPlayerLogo
            } else if (match?.awayPlayerKey == playerKey) {
                match.secondPlayerLogo
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching player logo from DB: $playerKey")
            null
        }
    }

    suspend fun getLocalPlayerMatches(playerKey: String): List<TennisMatch> {
        return try {
            matchDao.getMatchesByPlayerKeyList(playerKey).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMatchDetail(matchId: String): Result<MatchDetail> {
        // First, try to refresh this specific match from the API to get the latest status
        val tennisKey = keyStore.tennisApiKey.first()
        if (tennisKey.isNotBlank()) {
            try {
                // Fetch live matches to update this one if it's still live or finished
                val liveResponse = tennisApi.getLivescores(apiKey = tennisKey)
                if (liveResponse.success == 1) {
                    val updatedEntities = liveResponse.result?.filter { it.eventKey.toString() == matchId }?.mapNotNull { it.toEntity() } ?: emptyList()
                    if (updatedEntities.isNotEmpty()) {
                        matchDao.upsertMatches(updatedEntities)
                        Timber.d("Updated match $matchId from livescores API")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing match details from API")
                // Continue with cached data if API fails
            }
        }

        val entity = matchDao.getMatchById(matchId) ?: return Result.Error("Match nicht gefunden")
        val match = entity.toDomain()

        val oddsKey = keyStore.oddsApiKey.first()

        // Stats are inline in cached entity — parse from stored JSON
        val stats = entity.statsJson?.let { parseStatsJson(it, entity.homePlayerKey, entity.awayPlayerKey) }
            ?: emptyList()

        // Fetch H2H
        val h2h = try {
            if (tennisKey.isNotBlank()) {
                val resp = tennisApi.getH2H(
                    player1Key = entity.homePlayerKey,
                    player2Key = entity.awayPlayerKey,
                    apiKey = tennisKey
                )
                resp.result?.let { r ->
                    val matches = r.h2hMatches ?: emptyList()
                    val p1Wins = matches.count { m ->
                        val isWinnerP1 = m.winner == "First Player" && m.firstPlayerKey.toString() == entity.homePlayerKey
                        val isWinnerP1Alt = m.winner == "Second Player" && m.secondPlayerKey.toString() == entity.homePlayerKey
                        isWinnerP1 || isWinnerP1Alt
                    }
                    val p2Wins = matches.count { m ->
                        val isWinnerP2 = m.winner == "Second Player" && m.secondPlayerKey.toString() == entity.awayPlayerKey
                        val isWinnerP2Alt = m.winner == "First Player" && m.firstPlayerKey.toString() == entity.awayPlayerKey
                        isWinnerP2 || isWinnerP2Alt
                    }
                    H2HResult(
                        player1Name = entity.homePlayer,
                        player2Name = entity.awayPlayer,
                        player1Wins = p1Wins,
                        player2Wins = p2Wins,
                        recentMatches = matches.take(5).map { m ->
                            H2HMatch(
                                date = m.eventDate,
                                winner = when (m.winner) {
                                    "First Player" -> m.firstPlayer
                                    "Second Player" -> m.secondPlayer
                                    else -> "-"
                                },
                                score = m.scores?.joinToString("  ") { "${it.scoreFirst ?: "?"}-${it.scoreSecond ?: "?"}" }
                                    ?: m.finalResult ?: "-",
                                tournament = m.tournamentName,
                                surface = inferSurface(m.tournamentName, m.eventTypeType)
                            )
                        }
                    )
                }
            } else null
        } catch (e: Exception) { null }

        // Fetch odds
        val odds = try {
            if (oddsKey.isNotBlank()) {
                val sportKey = oddsSportKey(entity.tournamentCategory)
                val oddsResp = oddsApi.getOdds(sport = sportKey, apiKey = oddsKey)
                val lastName1 = entity.homePlayer.split(" ").last().lowercase()
                val lastName2 = entity.awayPlayer.split(" ").last().lowercase()

                // Match event where both player names appear (or similar)
                val matchOdds = oddsResp.find { event ->
                    val eventHome = event.homeTeam.lowercase()
                    val eventAway = event.awayTeam.lowercase()

                    // Match: home player is in homeTeam AND away player is in awayTeam
                    (eventHome.contains(lastName1) && eventAway.contains(lastName2)) ||
                    (eventHome.contains(lastName2) && eventAway.contains(lastName1))
                }

                matchOdds?.bookmakers?.mapNotNull { b ->
                    val h2hMarket = b.markets.find { it.key == "h2h" } ?: return@mapNotNull null
                    val homeOdds = h2hMarket.outcomes.find { it.name == matchOdds.homeTeam }?.price ?: return@mapNotNull null
                    val awayOdds = h2hMarket.outcomes.find { it.name == matchOdds.awayTeam }?.price ?: return@mapNotNull null
                    BookmakerOdds(b.title, homeOdds, awayOdds)
                } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) { emptyList() }

        val prediction = predictor.predict(match, h2h, odds)

        // Resolve player data via PlayerEntity mapping (reliable key-based lookup)
        val tour = if (entity.leagueId.contains("wta", ignoreCase = true)) "WTA" else "ATP"
        val homePlayerMeta = playerDao.getByApiKey(entity.homePlayerKey)
        val awayPlayerMeta = playerDao.getByApiKey(entity.awayPlayerKey)

        suspend fun rankingForPlayer(meta: com.lenz.tennisapp.data.db.entities.PlayerEntity?, displayName: String): Int? {
            // 1. Use mapped rankingKey if available
            meta?.rankingKey?.let { key ->
                rankingDao.getRankingByPlayerAndTour(key, tour)?.let { return it.ranking }
            }
            // 2. Fallback: name-based search
            val lastName = displayName.split(" ").last()
            return rankingDao.getRankingByPlayerNameAndTour("%$lastName%", tour)?.ranking
        }

        suspend fun eloForPlayer(meta: com.lenz.tennisapp.data.db.entities.PlayerEntity?, displayName: String): com.lenz.tennisapp.data.db.entities.EloRatingEntity? {
            // 1. Use mapped eloKey if available
            meta?.eloKey?.let { key ->
                eloDao.getElo(key)?.let { return it }
            }
            // 2. Fallback: last name search
            val lastName = displayName.split(" ").last()
            return eloDao.getEloByLastName(lastName)
        }

        val homeRanking = rankingForPlayer(homePlayerMeta, entity.homePlayer)
        val awayRanking = rankingForPlayer(awayPlayerMeta, entity.awayPlayer)

        val enrichedMatch = match.copy(
            homePlayer = match.homePlayer.copy(ranking = homeRanking ?: match.homePlayer.ranking),
            awayPlayer = match.awayPlayer.copy(ranking = awayRanking ?: match.awayPlayer.ranking)
        )

        val player1Elo = eloForPlayer(homePlayerMeta, entity.homePlayer)?.toProfile()
        val player2Elo = eloForPlayer(awayPlayerMeta, entity.awayPlayer)?.toProfile()

        return Result.Success(
            MatchDetail(
                match = enrichedMatch,
                stats = stats,
                h2h = h2h ?: H2HResult(match.homePlayer.name, match.awayPlayer.name, 0, 0, emptyList()),
                odds = odds,
                prediction = prediction,
                player1Elo = player1Elo,
                player2Elo = player2Elo
            )
        )
    }

    // ─── Grouping ──────────────────────────────────────────────────────────────

    private fun groupIntoTournaments(entities: List<MatchEntity>): List<Tournament> {
        return entities
            .groupBy { it.leagueId }
            .map { (leagueId, matches) ->
                val first = matches.first()
                val type = when {
                    leagueId.contains("Doubles", ignoreCase = true) -> "Doubles"
                    leagueId.contains("Atp", ignoreCase = true) -> "ATP"
                    leagueId.contains("Wta", ignoreCase = true) -> "WTA"
                    else -> null
                }
                Tournament(
                    id = leagueId,
                    name = first.leagueName,
                    category = TournamentCategory.valueOf(first.tournamentCategory),
                    surface = first.surface?.let { runCatching { Surface.valueOf(it) }.getOrNull() } ?: Surface.HARD,
                    matches = matches.map { it.toDomain() },
                    type = type
                )
            }
            .filter { it.category != TournamentCategory.ITF } // Completely remove ITF
            .sortedWith(compareBy(
                { it.category.sortOrder },
                { 
                    when(it.type) {
                        "ATP" -> 0
                        "WTA" -> 1
                        "Doubles" -> 2
                        else -> 3
                    }
                },
                { it.name }
            ))
    }

    // ─── Stats parsing ─────────────────────────────────────────────────────────

    private fun parseStatsJson(json: String, p1Key: String, p2Key: String): List<StatLine> {
        return try {
            val moshi = com.squareup.moshi.Moshi.Builder()
                .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, MatchStatDto::class.java)
            val adapter = moshi.adapter<List<MatchStatDto>>(type)
            val allStats = adapter.fromJson(json) ?: return emptyList()

            val matchStats = allStats.filter { it.period == "match" }
            val statNames = matchStats.map { it.name }.distinct()

            statNames.mapNotNull { name ->
                val p1Stat = matchStats.find { it.playerKey.toString() == p1Key && it.name == name }
                val p2Stat = matchStats.find { it.playerKey.toString() == p2Key && it.name == name }
                if (p1Stat == null && p2Stat == null) return@mapNotNull null
                val h = p1Stat?.value ?: "-"
                val a = p2Stat?.value ?: "-"
                val hNum = h.replace("%", "").toDoubleOrNull()
                val aNum = a.replace("%", "").toDoubleOrNull()
                StatLine(
                    label = name,
                    homeValue = h,
                    awayValue = a,
                    homeIsWinning = if (hNum != null && aNum != null) hNum > aNum else null
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    // ─── Cleanup & Maintenance ──────────────────────────────────────────────────

    /**
     * Delete matches older than 90 days to manage database size
     * Called automatically by cleanup worker daily
     */
    suspend fun cleanupOldMatches(): Result<Unit> {
        return try {
            val ninetyDaysAgoMs = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
            matchDao.deleteOldMatches(ninetyDaysAgoMs)
            Timber.d("✅ Cleaned up matches older than 90 days")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ Error cleaning up old matches")
            Result.Error(e.message ?: "Cleanup error")
        }
    }

    /**
     * Clear all old/incorrect rankings and elo ratings
     * Called before syncing new data from unified TennisAbstract source
     */
    suspend fun clearOldRankingsAndElo(): Result<Unit> {
        return try {
            eloDao.clearAll()
            rankingDao.clearAll()
            Timber.d("✅ Cleared all old rankings and elo ratings")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ Error clearing rankings and elo ratings")
            Result.Error(e.message ?: "Clear error")
        }
    }

    private suspend fun checkForNotifications(matches: List<MatchEntity>) {
        try {
            val followed = followedPlayerDao.getAllFollowedPlayers().firstOrNull() ?: emptyList()
            val followedKeys = followed.filter { it.notificationsEnabled }.map { it.playerKey }.toSet()
            
            if (followedKeys.isEmpty()) return

            matches.forEach { match ->
                // Only notify for live matches that we haven't notified for yet
                if (match.isLive && (match.homePlayerKey in followedKeys || match.awayPlayerKey in followedKeys)) {
                    if (!notifiedMatchDao.wasNotified(match.id)) {
                        sendMatchNotification(match)
                        notifiedMatchDao.markAsNotified(NotifiedMatchEntity(match.id))
                        Timber.d("🔔 Notification sent for match ${match.id}: ${match.homePlayer} vs ${match.awayPlayer}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking for notifications")
        }
    }

    private fun sendMatchNotification(match: MatchEntity) {
        val title = "Match gestartet! 🎾"
        val message = "${match.homePlayer} vs. ${match.awayPlayer} (${match.leagueName})"
        NotificationHelper.notifyMatchStarted(context, match.id, title, message)
    }
}

// ─── EloRatingEntity → domain profile ──────────────────────────────────────

private fun EloRatingEntity.toProfile() = PlayerEloProfile(
    eloOverall    = (eloOverall ?: 1500.0).toInt(),
    eloClay       = (eloClay    ?: eloOverall ?: 1500.0).toInt(),
    eloGrass      = (eloGrass   ?: eloOverall ?: 1500.0).toInt(),
    eloHard       = (eloHard    ?: eloOverall ?: 1500.0).toInt(),
    eloIndoor     = (eloIndoor  ?: eloHard ?: eloOverall ?: 1500.0).toInt(),
    matchesPlayed = matchesPlayed
)

// ─── Extension functions: DTO → Entity ─────────────────────────────────────

private fun TennisMatchDto.toEntity(): MatchEntity? {
    if (eventKey == null || firstPlayerKey == null || secondPlayerKey == null ||
        eventDate == null || eventTime == null ||
        firstPlayer.isNullOrBlank() || secondPlayer.isNullOrBlank()) return null

    val cat = refineCategoryByTournamentName(tournamentName ?: "", inferCategory(eventTypeType ?: ""))
    val surf = inferSurface(tournamentName ?: "", eventTypeType ?: "")
    val winnerId = when (winner) {
        "First Player" -> firstPlayerKey.toString()
        "Second Player" -> secondPlayerKey.toString()
        else -> null
    }
    // Prefer scores array (contains all sets), fallback to finalResult from API
    val scoresStr = scores?.takeIf { it.isNotEmpty() }?.joinToString(",") { "${it.scoreFirst ?: "?"}-${it.scoreSecond ?: "?"}" }

    // If we have structured scores, use them; otherwise fallback to API result
    // For live matches, API may provide incomplete data, so structure is preferred
    val finalScore = scoresStr ?: finalResult

    Timber.d("Match ${eventKey}: scores=${scoresStr}, finalResult=${finalResult}")

    return MatchEntity(
        id = eventKey.toString(),
        date = eventDate,
        time = eventTime,
        homePlayer = firstPlayer,
        homePlayerKey = firstPlayerKey.toString(),
        awayPlayer = secondPlayer,
        awayPlayerKey = secondPlayerKey.toString(),
        finalResult = finalScore,
        gameResult = gameResult,
        status = status,
        isLive = isLive == "1",
        leagueName = tournamentName,
        leagueId = "${tournamentKey}_${eventTypeType.replace(" ", "_")}",
        round = tournamentRound,
        surface = surf.name,
        tournamentCategory = cat.name,
        winnerId = winnerId,
        statsJson = statistics?.let {
            try {
                val moshi = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, MatchStatDto::class.java)
                moshi.adapter<List<MatchStatDto>>(type).toJson(it)
            } catch (e: Exception) { null }
        },
        firstPlayerLogo = firstPlayerLogo,
        secondPlayerLogo = secondPlayerLogo,
        serve = serve
    )
}

private fun MatchEntity.toDomain(): TennisMatch {
    val cat = TournamentCategory.valueOf(tournamentCategory)
    val finishedByScore = isMatchFinished(finalResult, cat, leagueId)

    // Validate that a match marked as "live" is actually still live
    val isActuallyLive = isLive && !finishedByScore && run {
        val now = System.currentTimeMillis()
        val timeSinceCached = now - cachedAt

        // If not updated in 3+ hours, it's probably finished
        val threeHoursMs = 3 * 60 * 60 * 1000L
        timeSinceCached < threeHoursMs
    }

    return TennisMatch(
        id = id,
        date = date,
        time = time,
        homePlayer = Player(homePlayerKey, homePlayer, logoUrl = firstPlayerLogo),
        awayPlayer = Player(awayPlayerKey, awayPlayer, logoUrl = secondPlayerLogo),
        status = when {
            status == "Finished" || status == "Retired" || status == "Walkover" || finishedByScore -> MatchStatus.FINISHED
            status == "Cancelled" -> MatchStatus.CANCELLED
            isActuallyLive -> MatchStatus.LIVE
            status == "Postponed" -> MatchStatus.POSTPONED
            else -> MatchStatus.NOT_STARTED
        },
        score = finalResult?.takeIf { it.isNotEmpty() && it != "-" },
        gameScore = gameResult?.takeIf { it.isNotEmpty() && it != "-" },
        isHomeServing = when (serve) {
            "First Player" -> true
            "Second Player" -> false
            else -> null
        },
        round = round,
        tournament = leagueName,
        leagueId = leagueId,
        tournamentCategory = cat,
        surface = surface?.let { runCatching { Surface.valueOf(it) }.getOrNull() } ?: Surface.HARD
    )
}

// ─── Inference helpers ──────────────────────────────────────────────────────

private fun inferSurface(tournamentName: String, eventTypeType: String = ""): Surface {
    val name = tournamentName.lowercase()
    return when {
        name.contains("roland") || name.contains("french open") ||
                name.contains("clay") || name.contains("monte carlo") ||
                name.contains("madrid") || name.contains("rome") ||
                name.contains("barcelona") -> Surface.CLAY
        name.contains("wimbledon") || name.contains("grass") ||
                name.contains("halle") || name.contains("queens") ||
                name.contains("eastbourne") || name.contains("birmingham") ||
                name.contains("s-hertogenbosch") -> Surface.GRASS
        name.contains("indoor") || name.contains("paris") && !name.contains("french") -> Surface.INDOOR_HARD
        else -> Surface.HARD
    }
}

private fun inferCategory(eventTypeType: String): TournamentCategory {
    val t = eventTypeType.lowercase()
    return when {
        t == "atp singles" || t == "atp doubles" -> TournamentCategory.ATP_250 // Will be refined by tournament name
        t == "wta singles" || t == "wta doubles" -> TournamentCategory.WTA_250
        t.contains("challenger men") -> TournamentCategory.CHALLENGER
        t.contains("challenger women") -> TournamentCategory.CHALLENGER
        t.contains("itf men") || t.contains("itf women") || t.startsWith("m15") ||
                t.startsWith("m25") || t.startsWith("w15") || t.startsWith("w25") ||
                t.startsWith("w50") || t.startsWith("w75") || t.startsWith("w100") -> TournamentCategory.ITF
        else -> TournamentCategory.OTHER
    }
}

// Called after initial category to refine ATP/WTA by tournament prestige
private fun refineCategoryByTournamentName(name: String, baseCategory: TournamentCategory): TournamentCategory {
    val n = name.lowercase()
    val isWta = baseCategory == TournamentCategory.WTA_250
    return when {
        n.contains("australian open") || n.contains("french open") ||
                n.contains("wimbledon") || n.contains("us open") -> TournamentCategory.GRAND_SLAM
        n.contains("indian wells") || n.contains("miami") || n.contains("monte carlo") ||
                n.contains("madrid") || n.contains("rome") || n.contains("canada") ||
                n.contains("cincinnati") || n.contains("shanghai") || n.contains("paris masters") ||
                n.contains("nitto") -> if (isWta) TournamentCategory.WTA_1000 else TournamentCategory.ATP_MASTERS_1000
        n.contains("500") -> if (isWta) TournamentCategory.WTA_500 else TournamentCategory.ATP_500
        else -> baseCategory
    }
}

private fun oddsSportKey(category: String): String = when (category) {
    "GRAND_SLAM", "ATP_MASTERS_1000", "ATP_500", "ATP_250" -> "tennis_atp"
    "WTA_1000", "WTA_500", "WTA_250" -> "tennis_wta"
    "CHALLENGER" -> "tennis_challenger_men"
    else -> "tennis_atp"
}

private fun isMatchFinished(finalResult: String?, category: TournamentCategory, leagueId: String): Boolean {
    val scores = finalResult?.split(",") ?: return false
    var homeSets = 0
    var awaySets = 0

    scores.forEach { s ->
        val parts = s.split("-")
        if (parts.size == 2) {
            val s1 = parts[0].trim().toIntOrNull() ?: 0
            val s2 = parts[1].trim().toIntOrNull() ?: 0
            if (isSetFinished(s1, s2)) {
                if (s1 > s2) homeSets++ else awaySets++
            }
        }
    }

    // Grand Slam Men (ATP) is best-of-5 (3 sets to win), others are best-of-3 (2 to win)
    val isGrandSlamMen = category == TournamentCategory.GRAND_SLAM &&
            (leagueId.contains("Atp", ignoreCase = true) || leagueId.contains("Men", ignoreCase = true))
    val setsToWin = if (isGrandSlamMen) 3 else 2

    return homeSets >= setsToWin || awaySets >= setsToWin
}

private fun getWinnerFromScore(entity: MatchEntity): String? {
    val scores = entity.finalResult?.split(",") ?: return null
    var homeSets = 0
    var awaySets = 0

    scores.forEach { s ->
        val parts = s.split("-")
        if (parts.size == 2) {
            val s1 = parts[0].trim().toIntOrNull() ?: 0
            val s2 = parts[1].trim().toIntOrNull() ?: 0
            if (isSetFinished(s1, s2)) {
                if (s1 > s2) homeSets++ else awaySets++
            }
        }
    }

    val cat = TournamentCategory.valueOf(entity.tournamentCategory)
    val isGrandSlamMen = cat == TournamentCategory.GRAND_SLAM &&
            (entity.leagueId.contains("Atp", ignoreCase = true) || entity.leagueId.contains("Men", ignoreCase = true))
    val setsToWin = if (isGrandSlamMen) 3 else 2

    return when {
        homeSets >= setsToWin -> entity.homePlayerKey
        awaySets >= setsToWin -> entity.awayPlayerKey
        else -> null
    }
}

private fun isSetFinished(s1: Int, s2: Int): Boolean {
    // Standard sets
    if (s1 >= 6 && s1 - s2 >= 2) return true
    if (s2 >= 6 && s2 - s1 >= 2) return true
    // Tiebreak
    if (s1 == 7 && s2 == 6) return true
    if (s2 == 7 && s1 == 6) return true
    // Match tiebreak (10 points) - common in doubles instead of 3rd set
    if ((s1 >= 10 || s2 >= 10) && Math.abs(s1 - s2) >= 2) return true
    return false
}

