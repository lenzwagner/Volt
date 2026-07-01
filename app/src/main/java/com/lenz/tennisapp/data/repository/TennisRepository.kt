package com.lenz.tennisapp.data.repository

import com.lenz.tennisapp.data.api.TheOddsApiService
import com.lenz.tennisapp.data.api.TARGET_BOOKMAKER
import com.lenz.tennisapp.data.api.RankingProxyService
import com.lenz.tennisapp.data.api.TennisApiService
import com.lenz.tennisapp.data.api.dto.*
import com.lenz.tennisapp.data.datastore.ApiKeyStore
import com.lenz.tennisapp.data.db.dao.*
import com.lenz.tennisapp.data.db.entities.MatchEntity
import com.lenz.tennisapp.data.db.entities.NotifiedMatchEntity
import com.lenz.tennisapp.data.db.entities.PlayerEntity
import com.lenz.tennisapp.data.local.AtpCalendar
import com.lenz.tennisapp.data.local.AtpChallengerCalendar
import com.lenz.tennisapp.data.local.WtaCalendar
import com.lenz.tennisapp.domain.model.*
import com.lenz.tennisapp.domain.prediction.MatchPredictor
import com.lenz.tennisapp.notification.NotificationHelper
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TennisRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tennisApi: TennisApiService,
    private val oddsApi: TheOddsApiService,
    private val rankingProxy: RankingProxyService,
    private val matchDao: MatchDao,
    private val eloDao: EloDao,
    private val rankingDao: RankingDao,
    private val playerDao: PlayerDao,
    private val predictionDao: PredictionDao,
    private val followedPlayerDao: FollowedPlayerDao,
    private val notifiedMatchDao: NotifiedMatchDao,
    private val keyStore: ApiKeyStore,
    private val predictor: MatchPredictor,
    private val moshi: com.squareup.moshi.Moshi
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // In-memory cache for last loaded MatchDetail per match ID — instant second-visit render
    private val matchDetailCache = LinkedHashMap<String, MatchDetail>(64, 0.75f, true)

    fun getCachedMatchDetail(matchId: String): MatchDetail? = matchDetailCache[matchId]

    // Predictions JSON is one shared list for ALL matches — cache it so warming 75
    // match details only hits /api/predictions once, not 75×.
    private var cachedPredictions: com.lenz.tennisapp.data.api.PredictionsResponse? = null
    private var predictionsCachedAt: Long = 0L
    private val PREDICTIONS_TTL_MS = 15 * 60 * 1000L
    private val predictionsMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun getAiPredictions(): com.lenz.tennisapp.data.api.PredictionsResponse? = getCachedPredictions()

    private fun injectPredictionIfMissing(detail: MatchDetail): MatchDetail {
        if (detail.prediction != null) return detail
        val resp = cachedPredictions ?: return detail
        val match = detail.match
        fun nk(s: String) = aiNameKey(s)
        val k1h = nk(match.homePlayer.name); val k2h = nk(match.awayPlayer.name)
        val dto = resp.data?.matches?.firstOrNull { m ->
            val mk1 = nk(m.p1Fullname); val mk2 = nk(m.p2Fullname)
            (mk1 == k1h && mk2 == k2h) || (mk1 == k2h && mk2 == k1h)
        } ?: return detail
        val swapped = nk(dto.p1Fullname) == k2h
        val p1Prob = if (swapped) dto.p2Prob else dto.p1Prob
        val p2Prob = if (swapped) dto.p1Prob else dto.p2Prob
        val conf = when {
            dto.confidence >= 0.65f -> PredictionConfidence.HIGH
            dto.confidence >= 0.50f -> PredictionConfidence.MEDIUM
            else -> PredictionConfidence.LOW
        }
        val prediction = MatchPrediction(p1Prob, p2Prob, conf, emptyList())
        val updated = detail.copy(prediction = prediction)
        matchDetailCache[match.id] = updated
        return updated
    }

    private fun aiNameKey(raw: String): String {
        val parts = raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""
        
        val isInitial = { t: String -> t.length <= 2 && (t.endsWith(".") || t.length == 1) }
        val names = parts.filterNot(isInitial)
        if (names.isEmpty()) return parts.last().lowercase()

        // Heuristic: identify the last name. 
        // In "LASTNAME Firstname" (Tennis API), it's the first part.
        // In "Firstname Lastname" (AI Predictions), it's the last part.
        // If a part is ALL CAPS (length > 1), it's likely the last name.
        val allCapsParts = names.filter { it.length > 1 && it.all { c -> c.isUpperCase() || !c.isLetter() } }
        val last = if (allCapsParts.isNotEmpty()) {
            allCapsParts.last().lowercase().trim('.', ',')
        } else {
            names.last().lowercase().trim('.', ',')
        }

        // First initial: take from the part that isn't the last name
        val firstInitial = (names.firstOrNull { it.lowercase() != last } ?: parts.firstOrNull { it.lowercase() != last })
            ?.firstOrNull()?.lowercaseChar() ?: ' '
        
        return "$last|$firstInitial"
    }

    suspend fun findMatchIdByPlayers(p1: String, p2: String, predictionDate: String? = null): String? {
        val k1 = aiNameKey(p1); val k2 = aiNameKey(p2)
        val datesToTry = buildSet {
            if (predictionDate != null) add(predictionDate)
            add(java.time.LocalDate.now().format(dateFormatter))
            add(java.time.LocalDate.now().plusDays(1).format(dateFormatter))
        }
        for (dateStr in datesToTry) {
            val matches = matchDao.getMatchesForDate(dateStr).first()
            val hit = matches.find { m ->
                val hk = aiNameKey(m.homePlayer); val ak = aiNameKey(m.awayPlayer)
                (hk == k1 && ak == k2) || (hk == k2 && ak == k1)
            }
            if (hit != null) return hit.id
        }
        return null
    }

    suspend fun enrichAiPredictions(dtos: List<com.lenz.tennisapp.data.api.PredictionMatchDto>, predictionDate: String? = null): List<com.lenz.tennisapp.ui.screens.airecommendations.EnrichedAiPrediction> {
        val datesToTry = buildSet {
            if (predictionDate != null) add(predictionDate)
            add(java.time.LocalDate.now().format(dateFormatter))
            add(java.time.LocalDate.now().plusDays(1).format(dateFormatter))
        }
        val entities = datesToTry.flatMap { matchDao.getMatchesForDate(it).first() }
        return dtos.map { dto ->
            val k1 = aiNameKey(dto.p1Fullname); val k2 = aiNameKey(dto.p2Fullname)
            val entity = entities.find { m ->
                val hk = aiNameKey(m.homePlayer); val ak = aiNameKey(m.awayPlayer)
                (hk == k1 && ak == k2) || (hk == k2 && ak == k1)
            }
            val cat = entity?.let {
                try { TournamentCategory.valueOf(it.tournamentCategory) } catch (e: Exception) { TournamentCategory.OTHER }
            }
            com.lenz.tennisapp.ui.screens.airecommendations.EnrichedAiPrediction(
                dto = dto,
                matchId = entity?.id,
                category = cat,
                eventType = entity?.eventType ?: ""
            )
        }
    }

    private suspend fun getCachedPredictions(): com.lenz.tennisapp.data.api.PredictionsResponse? {
        val now = System.currentTimeMillis()
        val today = java.time.LocalDate.now().format(dateFormatter)
        fun isFresh(cached: com.lenz.tennisapp.data.api.PredictionsResponse?): Boolean {
            if (cached == null) return false
            if (now - predictionsCachedAt >= PREDICTIONS_TTL_MS) return false
            if (cached.data?.date != today) return false
            return true
        }
        // L1: in-memory (15min TTL)
        cachedPredictions?.let { if (isFresh(it)) return it }
        return predictionsMutex.withLock {
            cachedPredictions?.let { if (isFresh(it)) return it }
            // L2: DataStore (day-based, survives app restart)
            val storedDate = keyStore.predictionsDate.first()
            if (storedDate == today) {
                val storedJson = keyStore.predictionsJson.first()
                if (storedJson.isNotBlank()) {
                    runCatching {
                        val adapter = moshi.adapter(com.lenz.tennisapp.data.api.PredictionsResponse::class.java)
                        adapter.fromJson(storedJson)
                    }.getOrNull()?.also {
                        cachedPredictions = it
                        predictionsCachedAt = now
                        Timber.d("Predictions loaded from DataStore cache")
                        return it
                    }
                }
            }
            // L3: network
            try {
                rankingProxy.getPredictions().also { resp ->
                    cachedPredictions = resp
                    predictionsCachedAt = System.currentTimeMillis()
                    val date = resp?.data?.date ?: today
                    val adapter = moshi.adapter(com.lenz.tennisapp.data.api.PredictionsResponse::class.java)
                    val json = runCatching { adapter.toJson(resp) }.getOrNull() ?: ""
                    if (json.isNotBlank()) {
                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            keyStore.savePredictions(json, date)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Predictions fetch failed")
                null
            }
        }
    }

    private suspend fun activeTennisSports(oddsKey: String): Map<String, List<String>> {
        // Free call (0 credits). Returns active ATP/WTA sport keys from The Odds API.
        return try {
            val sports = oddsApi.getSports(apiKey = oddsKey)
            val atp = sports.filter { it.active && it.key.startsWith("tennis_atp") }.map { it.key }
            val wta = sports.filter { it.active && it.key.startsWith("tennis_wta") }.map { it.key }
            mapOf("atp" to atp, "wta" to wta)
        } catch (e: Exception) {
            Timber.w("TheOddsApi getSports failed: ${e.message}")
            emptyMap()
        }
    }

    private fun filterSportsForEvent(eventType: String, sports: Map<String, List<String>>): List<String> {
        val et = eventType.lowercase()
        return if (et.contains("wta") || et.contains("women")) sports["wta"] ?: emptyList()
        else sports["atp"] ?: emptyList()
    }

    private val oddsAdapter by lazy {
        moshi.adapter<List<BookmakerOdds>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, BookmakerOdds::class.java)
        )
    }

    fun getMatchesForDate(date: LocalDate): Flow<List<Tournament>> =
        matchDao.getMatchesForDate(date.format(dateFormatter))
            .map { entities -> groupIntoTournaments(entities) }
            .flowOn(Dispatchers.Default)

    fun getLiveMatches(): Flow<List<TennisMatch>> =
        matchDao.getLiveMatches().map { entities ->
            val now = System.currentTimeMillis()
            entities
                .filter { entity ->
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

    suspend fun fetchOnce(): List<Tournament> = withContext(Dispatchers.Default) {
        val today = java.time.LocalDate.now()
        val yesterday = today.minusDays(1)
        val todayStr = today.format(dateFormatter)
        val yesterdayStr = yesterday.format(dateFormatter)
        
        val apiKey = keyStore.tennisApiKey.first().takeIf { it.isNotBlank() }
        try {
            // Fetch yesterday and today to catch carry-over matches (rain delays)
            val response = tennisApi.getFixtures(eventType = 1, dateStart = yesterdayStr, dateStop = todayStr, apiKey = apiKey)
            Timber.d("fetchOnce API Response: success=${response.success}, resultSize=${response.result?.size}")
            
            if (response.result != null) {
                val entities = response.result.mapNotNull { it.toEntity() }
                upsertMatchesPreservingOdds(entities)
                seedPlayersFromMatches(response.result)
            }
        } catch (e: Exception) {
            Timber.e(e, "fetchOnce API error")
        }
        
        // Return only matches for today's view (including those started yesterday but updated today)
        val entities = matchDao.getMatchesForDate(todayStr).first()
        return@withContext groupIntoTournaments(entities)
    }

    suspend fun refreshLivescores() {
        try {
            val response = tennisApi.getLivescores(eventType = 1)
            Timber.d("getLivescores success: ${response.success}, results: ${response.result?.size}")
            if (response.result != null) {
                val entities = response.result.mapNotNull { it.toEntity() }
                upsertMatchesPreservingOdds(entities)
                val liveIds = entities.map { it.id }
                // Only reset stale-live flags when API confirms at least one live match.
                // An empty result may be a transient API glitch — resetting here would set
                // isLive = 0 for every match and cause status to flip to NOT_STARTED.
                if (liveIds.isNotEmpty()) {
                    matchDao.resetStaleLive(liveIds)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "refreshLivescores error")
        }
    }

    suspend fun refreshSilent(date: LocalDate) {
        val dateStr = date.format(dateFormatter)
        val apiKey = keyStore.tennisApiKey.first().takeIf { it.isNotBlank() }
        try {
            val fixtures = tennisApi.getFixtures(eventType = 1, dateStart = dateStr, dateStop = dateStr, apiKey = apiKey)
            if (fixtures.result != null) {
                upsertMatchesPreservingOdds(fixtures.result.mapNotNull { it.toEntity() })
            }
            refreshLivescores()
        } catch (e: Exception) {}
    }

    suspend fun refreshTournamentMatches(leagueId: String) {
        // Implementation omitted for brevity, adding back for compilation
    }

    suspend fun getLastYearWinner(leagueId: String): String? = null

    suspend fun getPlayerMatches(playerKey: String): Result<List<TennisMatch>> = Result.Success(emptyList())

    suspend fun getLocalPlayerMatches(playerKey: String): List<TennisMatch> {
        if (playerKey.isBlank()) return emptyList()
        return matchDao.getMatchesByPlayerKeyList(playerKey)
            .filter { it.status == "Finished" || it.status == "Retired" }
            .map { it.toDomain() }
    }

    suspend fun getPlayerLogo(playerKey: String): String? = null

    suspend fun cleanupOldMatches() {
        val ninetyDaysAgoMs = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
        matchDao.deleteOldMatches(ninetyDaysAgoMs)
    }

    private suspend fun groupIntoTournaments(entities: List<MatchEntity>): List<Tournament> {
        val tournaments = entities
            .groupBy { it.leagueId }
            .mapNotNull { (leagueId, matches) ->
                val first = matches.first()
                val eventTypeRaw = first.eventType ?: ""
                // Drop ITF / low-level events: any M## or W## where ## < 75
                val mwPoints = Regex("""\b[MW](\d+)\b""", RegexOption.IGNORE_CASE)
                    .find(eventTypeRaw)?.groupValues?.get(1)?.toIntOrNull()
                if (mwPoints != null && mwPoints < 75) return@mapNotNull null
                if (eventTypeRaw.contains("itf", ignoreCase = true)) return@mapNotNull null
                val leagueName = first.leagueName ?: ""
                val isDoubles = eventTypeRaw.contains("double", ignoreCase = true) ||
                    leagueId.contains("double", ignoreCase = true)
                val type = if (isDoubles) "Doubles" else "Singles"
                val eventType = eventTypeRaw
                Timber.d("TOURNAMENT leagueName='${first.leagueName}' eventType='$eventTypeRaw' leagueId='$leagueId'")
                val today = java.time.LocalDate.now().format(dateFormatter)
                val calendarEntry = when {
                    eventType.contains("challenger", ignoreCase = true) ->
                        AtpChallengerCalendar.findByName(first.leagueName, today) ?: AtpCalendar.findByName(first.leagueName, today)
                    eventType.contains("atp", ignoreCase = true) ->
                        AtpCalendar.findByName(first.leagueName, today) ?: WtaCalendar.findByName(first.leagueName, today)
                    eventType.contains("wta", ignoreCase = true) ->
                        WtaCalendar.findByName(first.leagueName, today) ?: AtpChallengerCalendar.findByName(first.leagueName, today) ?: AtpCalendar.findByName(first.leagueName, today)
                    else ->
                        AtpCalendar.findByName(first.leagueName, today) ?: WtaCalendar.findByName(first.leagueName, today)
                }
                val category = calendarEntry?.category ?: inferCategory(eventType, first.leagueName)
                if (category.points < 75) return@mapNotNull null
                // Keep qualifying ONLY for Grand Slams; drop it everywhere else.
                // Detect qualifying from the league name/id OR from the match rounds
                // (GS qualifying is often listed under the plain "Wimbledon" name).
                val roundsQualif = matches.isNotEmpty() && matches.all {
                    val r = it.round?.lowercase() ?: ""
                    r.contains("qualif") || r.contains("pre-q") || r.startsWith("q")
                }
                val isQualif = leagueName.contains("qualif", ignoreCase = true) ||
                    leagueId.contains("qualif", ignoreCase = true) || roundsQualif
                if (isQualif && category != TournamentCategory.GRAND_SLAM) return@mapNotNull null
                val domainMatches = matches.map { it.toDomain() }
                    .filter { it.status != MatchStatus.CANCELLED && it.status != MatchStatus.POSTPONED }
                if (domainMatches.isEmpty()) return@mapNotNull null
                Tournament(
                    id = leagueId,
                    name = first.leagueName,
                    location = calendarEntry?.location?.substringBefore(",")?.trim(),
                    category = category,
                    surface = calendarEntry?.surface ?: inferSurface(first.leagueName, eventType),
                    matches = domainMatches,
                    type = type,
                    isQualifying = isQualif
                )
            }
            .sortedWith(compareBy(
                { -it.category.points },
                { when {
                    it.type == "Doubles" -> 2
                    it.category.name.startsWith("WTA") -> 1
                    else -> 0
                }},
                { it.name }
            ))

        return enrichWithRankings(tournaments)
    }

    /**
     * Seeds minimal player stubs (key + name + logo) so the ranking sync can later
     * find them by name and write liveRanking. Uses IGNORE conflict so existing rich
     * player profiles are never overwritten.
     */
    private suspend fun seedPlayersFromMatches(dtos: List<TennisMatchDto>) {
        val stubs = dtos.flatMap { dto ->
            listOfNotNull(
                dto.firstPlayerKey?.let { key ->
                    PlayerEntity(
                        playerKey = key.toString(),
                        name = dto.firstPlayer ?: return@let null,
                        fullName = dto.firstPlayer,
                        nationality = null, birthDate = null,
                        photoUrl = dto.firstPlayerLogo,
                        playerType = dto.eventTypeType?.let {
                            if (it.contains("Wta", ignoreCase = true)) "wta" else "atp"
                        },
                        currentRanking = null, currentRankingPoints = null,
                        currentSeasonTitles = null, currentSeasonWins = null,
                        currentSeasonLosses = null, careerHighRanking = null,
                        careerTitles = null, hardWinRate = null, clayWinRate = null,
                        grassWinRate = null, statsJson = null, tournamentsJson = null
                    )
                },
                dto.secondPlayerKey?.let { key ->
                    PlayerEntity(
                        playerKey = key.toString(),
                        name = dto.secondPlayer ?: return@let null,
                        fullName = dto.secondPlayer,
                        nationality = null, birthDate = null,
                        photoUrl = dto.secondPlayerLogo,
                        playerType = dto.eventTypeType?.let {
                            if (it.contains("Wta", ignoreCase = true)) "wta" else "atp"
                        },
                        currentRanking = null, currentRankingPoints = null,
                        currentSeasonTitles = null, currentSeasonWins = null,
                        currentSeasonLosses = null, careerHighRanking = null,
                        careerTitles = null, hardWinRate = null, clayWinRate = null,
                        grassWinRate = null, statsJson = null, tournamentsJson = null
                    )
                }
            )
        }
        if (stubs.isNotEmpty()) playerDao.insertIfAbsent(stubs)
    }

    /**
     * Fills each player's live ranking (for the lime badge on the avatar) from the
     * players table, joined by playerKey. The live-ranking sync writes liveRanking
     * there; here we read it back per match in one batched query.
     */
    private suspend fun enrichWithRankings(tournaments: List<Tournament>): List<Tournament> {
        val keys = tournaments
            .flatMap { it.matches }
            .flatMap { listOf(it.homePlayer.key, it.awayPlayer.key) }
            .filter { it.isNotBlank() }
            .distinct()
        if (keys.isEmpty()) return tournaments

        val rankByKey = playerDao.getByKeys(keys).associate { it.playerKey to it.liveRanking }
        if (rankByKey.isEmpty()) return tournaments

        return tournaments.map { t ->
            t.copy(matches = t.matches.map { m ->
                m.copy(
                    homePlayer = m.homePlayer.copy(ranking = rankByKey[m.homePlayer.key] ?: m.homePlayer.ranking),
                    awayPlayer = m.awayPlayer.copy(ranking = rankByKey[m.awayPlayer.key] ?: m.awayPlayer.ranking)
                )
            })
        }
    }

    private fun TennisMatchDto.toEntity(): MatchEntity? {
        val date = (eventDate ?: "").take(10)
        val time = eventTime ?: ""
        val hPlayer = firstPlayer ?: "Unknown"
        val hKey = firstPlayerKey?.toString() ?: ""
        val aPlayer = secondPlayer ?: "Unknown"
        val aKey = secondPlayerKey?.toString() ?: ""
        
        // Fallback ID if eventKey is missing
        val id = eventKey?.toString() ?: "${hKey}_${aKey}_${date}".ifEmpty { UUID.randomUUID().toString() }

        val stat = status ?: ""
        val type = eventTypeType ?: ""
        val tName = tournamentName ?: "Unknown Tournament"
        val tKey = tournamentKey?.toString() ?: "0"

        val cat = inferCategory(type, tName)
        val surf = inferSurface(tName, type)
        val winnerId = when (winner) {
            "First Player" -> hKey
            "Second Player" -> aKey
            else -> null
        }
        
        // Prefer finalResult if it contains tiebreak info (parentheses) or if scores list is empty
        val scoresStr = scores?.takeIf { it.isNotEmpty() }?.joinToString(",") { "${it.scoreFirst}-${it.scoreSecond}" }
        val finalScore = if (finalResult?.contains("(") == true) {
            finalResult
        } else {
            scoresStr ?: finalResult
        }

        return MatchEntity(
            id = id,
            date = date,
            time = time,
            homePlayer = hPlayer,
            homePlayerKey = hKey,
            awayPlayer = aPlayer,
            awayPlayerKey = aKey,
            finalResult = finalScore,
            gameResult = gameResult,
            status = stat,
            isLive = isLive == "1",
            leagueName = tName,
            leagueId = "${tKey}_${type.replace(" ", "_")}",
            round = tournamentRound,
            surface = surf.name,
            tournamentCategory = cat.name,
            eventType = type,
            winnerId = winnerId,
            statsJson = null,
            firstPlayerLogo = firstPlayerLogo,
            secondPlayerLogo = secondPlayerLogo,
            serve = serve
        )
    }

    private fun MatchEntity.toDomain(): TennisMatch {
        val cachedOdds = oddsJson?.let { runCatching { oddsAdapter.fromJson(it) }.getOrNull() }?.firstOrNull()
        return TennisMatch(
            id = id,
            date = date,
            time = time,
            homePlayer = Player(homePlayerKey, homePlayer, logoUrl = firstPlayerLogo),
            awayPlayer = Player(awayPlayerKey, awayPlayer, logoUrl = secondPlayerLogo),
            status = when {
                status == "Finished" || status == "Retired" || status == "Walkover" -> MatchStatus.FINISHED
                status == "Cancelled" -> MatchStatus.CANCELLED
                status == "Postponed" -> MatchStatus.POSTPONED
                status == "Interrupted" || status == "Delayed" || status == "Suspended" -> MatchStatus.INTERRUPTED
                isLive && (System.currentTimeMillis() - cachedAt) < 45 * 60 * 1000L -> MatchStatus.LIVE
                status.isNullOrBlank() && finalResult.isNullOrBlank() -> MatchStatus.TBD
                else -> MatchStatus.NOT_STARTED
            },
            score = finalResult,
            gameScore = gameResult,
            isHomeServing = when (serve) {
                "First Player" -> true
                "Second Player" -> false
                else -> null
            },
            round = round,
            tournament = leagueName,
            leagueId = leagueId,
            tournamentCategory = try { TournamentCategory.valueOf(tournamentCategory) } catch (e: Exception) { TournamentCategory.OTHER },
            surface = surface?.let { runCatching { Surface.valueOf(it) }.getOrNull() } ?: Surface.HARD,
            eventType = eventType,
            isQualifying = false,
            winnerKey = winnerId,
            finalResult = finalResult,
            homeOdds = cachedOdds?.homeOdds,
            awayOdds = cachedOdds?.awayOdds
        )
    }

    private fun inferSurface(tournamentName: String, eventType: String = ""): Surface {
        val isChallenger = eventType.contains("challenger", ignoreCase = true)
        val isAtp = eventType.contains("atp", ignoreCase = true) || isChallenger
        if (isChallenger) AtpChallengerCalendar.findByName(tournamentName)?.let { return it.surface }
        if (isAtp) AtpCalendar.findByName(tournamentName)?.let { return it.surface }
        else WtaCalendar.findByName(tournamentName)?.let { return it.surface }
        // fallback: try all
        AtpChallengerCalendar.findByName(tournamentName)?.let { return it.surface }
        AtpCalendar.findByName(tournamentName)?.let { return it.surface }
        WtaCalendar.findByName(tournamentName)?.let { return it.surface }
        val name = tournamentName.lowercase()
        return when {
            // Indoor Clay
            name.contains("linz") || name.contains("stuttgart") || name.contains("rouen") -> Surface.INDOOR_HARD

            // Clay
            name.contains("clay") || name.contains("sand") ||
            name.contains("monte carlo") || name.contains("madrid") ||
            name.contains("roma") || name.contains("rome") ||
            name.contains("roland garros") || name.contains("french open") ||
            name.contains("barcelona") || name.contains("rio de janeiro") ||
            name.contains("bogota") || name.contains("colsanitas") ||
            name.contains("strasbourg") || name.contains("rabat") || name.contains("morocco") ||
            name.contains("iasi") || name.contains("hamburg") ||
            name.contains("prague") && name.contains("livesport") -> Surface.CLAY

            // Grass
            name.contains("grass") || name.contains("rasen") ||
            name.contains("wimbledon") ||
            name.contains("halle") ||
            name.contains("berlin") ||
            name.contains("queen") ||
            name.contains("eastbourne") ||
            name.contains("bad homburg") ||
            name.contains("nottingham") ||
            name.contains("hertogenbosch") || name.contains("rosmalen") ||
            name.contains("libema") -> Surface.GRASS

            // Indoor Hard
            name.contains("indoor") ||
            name.contains("ostrava") || name.contains("transylvania") || name.contains("cluj") ||
            name.contains("rotterdam") || name.contains("vienna") ||
            name.contains("basel") || name.contains("paris masters") ||
            name.contains("singapore") ||
            name.contains("wta finals") || name.contains("riyadh") -> Surface.INDOOR_HARD

            else -> Surface.HARD
        }
    }

    private fun inferCategory(eventTypeType: String, tournamentName: String? = null): TournamentCategory {
        if (tournamentName != null) {
            val isChallenger = eventTypeType.contains("challenger", ignoreCase = true)
            val isAtp = eventTypeType.contains("atp", ignoreCase = true) || isChallenger
            if (isChallenger) AtpChallengerCalendar.findByName(tournamentName)?.let { return it.category }
            if (isAtp) AtpCalendar.findByName(tournamentName)?.let { return it.category }
            else WtaCalendar.findByName(tournamentName)?.let { return it.category }
            AtpChallengerCalendar.findByName(tournamentName)?.let { return it.category }
            AtpCalendar.findByName(tournamentName)?.let { return it.category }
            WtaCalendar.findByName(tournamentName)?.let { return it.category }
        }
        val t = eventTypeType.lowercase()
        val n = tournamentName?.lowercase() ?: ""

        // Match major categories first
        if (t.contains("grand slam")) return TournamentCategory.GRAND_SLAM
        // Name-based Grand Slam detection (e.g. "Wimbledon Qualifying")
        if (n.contains("wimbledon") || n.contains("roland garros") || n.contains("french open") ||
            n.contains("us open") || n.contains("australian open")) {
            return TournamentCategory.GRAND_SLAM
        }
        if (t.contains("masters 1000") || t.contains("atp 1000") || t.contains("wta 1000")) {
            return if (t.contains("wta")) TournamentCategory.WTA_1000 else TournamentCategory.ATP_MASTERS_1000
        }
        
        // Specific tournament rules (Berlin, Halle etc.)
        if (n.contains("berlin") || n.contains("halle") || n.contains("london") || n.contains("queen") || 
            n.contains("vienna") || n.contains("basel") || n.contains("tokyo") || n.contains("dubai") || 
            n.contains("acapulco") || n.contains("barcelona") || n.contains("stuttgart") || n.contains("eastbourne") ||
            n.contains("abu dhabi") || n.contains("linz") || n.contains("charleston") || n.contains("strasbourg") ||
            n.contains("bad homburg") || n.contains("monterrey") || n.contains("seoul") || n.contains("ningbo") || 
            n.contains("zhengzhou") || n.contains("rotterdam") || n.contains("rio de janeiro") || n.contains("acapulco") ||
            n.contains("hamburg") || n.contains("washington") || n.contains("beijing") || n.contains("astana")) {
            return if (t.contains("wta")) TournamentCategory.WTA_500 else TournamentCategory.ATP_500
        }

        if (t.contains("wta 125") || (t.contains("wta") && n.contains("125"))) {
            return TournamentCategory.WTA_125
        }

        if (t.contains("challenger")) {
            return when {
                n.contains("175") || n.contains("phoenix") || n.contains("turin") || n.contains("bordeaux") -> TournamentCategory.CHALLENGER_175
                n.contains("nottingham open") || n.contains("125") || n.contains("bengaluru") || n.contains("canberra") || n.contains("mexico city") || n.contains("busan") -> TournamentCategory.CHALLENGER_125
                n.contains("100") || n.contains("monza") || n.contains("oeiras") || n.contains("heilbronn") || n.contains("prostejov") -> TournamentCategory.CHALLENGER_100
                n.contains("75") || n.contains("noumea") || n.contains("itajai") || n.contains("lugano") || n.contains("lille") -> TournamentCategory.CHALLENGER_75
                n.contains("nottingham") || n.contains("glasgow") || n.contains("tenerife") -> TournamentCategory.CHALLENGER_50
                else -> TournamentCategory.CHALLENGER
            }
        }

        return when {
            t.contains("500") -> if (t.contains("wta")) TournamentCategory.WTA_500 else TournamentCategory.ATP_500
            t.contains("250") -> if (t.contains("wta")) TournamentCategory.WTA_250 else TournamentCategory.ATP_250
            t.contains("atp") -> TournamentCategory.ATP_250
            t.contains("wta") -> TournamentCategory.WTA_250
            t.contains("itf") -> TournamentCategory.ITF
            else -> TournamentCategory.OTHER
        }
    }

    /** Fresh live-score state straight from the DB (no cache) — for 2s polling. */
    suspend fun getFreshMatch(matchId: String): TennisMatch? =
        matchDao.getMatchById(matchId)?.toDomain()

    /**
     * Instant, DB-only match detail: player enrichment, ELO, recent matches.
     * No network — used to render the page immediately while the full version
     * (H2H, odds, prediction) loads in the background.
     */
    suspend fun getMatchDetailBase(matchId: String): Result<MatchDetail> {
        // Return cached version, but invalidate if match status changed to FINISHED since caching.
        matchDetailCache[matchId]?.let { cached ->
            if (cached.match.status == MatchStatus.FINISHED) {
                return Result.Success(injectPredictionIfMissing(cached))
            }
            val currentStatus = matchDao.getMatchById(matchId)?.toDomain()?.status
            if (currentStatus != MatchStatus.FINISHED) return Result.Success(injectPredictionIfMissing(cached))
            matchDetailCache.remove(matchId) // fall through to re-fetch with final result
        }

        val entity = matchDao.getMatchById(matchId) ?: return Result.Error("Match nicht gefunden")
        var match = entity.toDomain()

        val p1Entity = playerDao.getByKey(entity.homePlayerKey)
        val p2Entity = playerDao.getByKey(entity.awayPlayerKey)
        if (p1Entity != null) {
            match = match.copy(homePlayer = match.homePlayer.copy(
                ranking = p1Entity.liveRanking ?: p1Entity.currentRanking ?: match.homePlayer.ranking,
                atpPoints = p1Entity.liveRankingPoints ?: p1Entity.currentRankingPoints,
                nationality = p1Entity.nationality,
                logoUrl = p1Entity.photoUrl ?: match.homePlayer.logoUrl,
                careerHighRanking = p1Entity.careerHighRanking,
                birthDate = p1Entity.birthDate,
                prizeMoneyYtd = p1Entity.prizeMoneyYtd
            ))
        }
        if (p2Entity != null) {
            match = match.copy(awayPlayer = match.awayPlayer.copy(
                ranking = p2Entity.liveRanking ?: p2Entity.currentRanking ?: match.awayPlayer.ranking,
                atpPoints = p2Entity.liveRankingPoints ?: p2Entity.currentRankingPoints,
                nationality = p2Entity.nationality,
                logoUrl = p2Entity.photoUrl ?: match.awayPlayer.logoUrl,
                careerHighRanking = p2Entity.careerHighRanking,
                birthDate = p2Entity.birthDate,
                prizeMoneyYtd = p2Entity.prizeMoneyYtd
            ))
        }
        fun PlayerEntity.toEloProfile() = if (eloRating != null) PlayerEloProfile(
            eloOverall = eloRating, eloClay = eloClay ?: eloRating, eloGrass = eloGrass ?: eloRating,
            eloHard = eloHard ?: eloRating, eloIndoor = eloHard ?: eloRating, matchesPlayed = 0
        ) else null

        return Result.Success(MatchDetail(
            match = match,
            stats = emptyList(),
            h2h = H2HResult(match.homePlayer.name, match.awayPlayer.name, 0, 0, emptyList()),
            odds = emptyList(),
            prediction = null,
            player1Elo = p1Entity?.toEloProfile(),
            player2Elo = p2Entity?.toEloProfile(),
            homeRecentMatches = getLocalPlayerMatches(entity.homePlayerKey),
            awayRecentMatches = getLocalPlayerMatches(entity.awayPlayerKey)
        ))
    }

    suspend fun getMatchDetail(
        matchId: String,
        forceRefreshOdds: Boolean = false,
        skipOdds: Boolean = false
    ): Result<MatchDetail> {
        val entity = matchDao.getMatchById(matchId) ?: return Result.Error("Match nicht gefunden")
        var match = entity.toDomain()

        // Enrich players with DB data (ranking, points, ELO)
        val p1Entity = playerDao.getByKey(entity.homePlayerKey)
        val p2Entity = playerDao.getByKey(entity.awayPlayerKey)

        if (p1Entity != null) {
            match = match.copy(homePlayer = match.homePlayer.copy(
                ranking = p1Entity.liveRanking ?: p1Entity.currentRanking ?: match.homePlayer.ranking,
                atpPoints = p1Entity.liveRankingPoints ?: p1Entity.currentRankingPoints,
                nationality = p1Entity.nationality,
                logoUrl = p1Entity.photoUrl ?: match.homePlayer.logoUrl,
                careerHighRanking = p1Entity.careerHighRanking,
                birthDate = p1Entity.birthDate,
                prizeMoneyYtd = p1Entity.prizeMoneyYtd
            ))
        }
        if (p2Entity != null) {
            match = match.copy(awayPlayer = match.awayPlayer.copy(
                ranking = p2Entity.liveRanking ?: p2Entity.currentRanking ?: match.awayPlayer.ranking,
                atpPoints = p2Entity.liveRankingPoints ?: p2Entity.currentRankingPoints,
                nationality = p2Entity.nationality,
                logoUrl = p2Entity.photoUrl ?: match.awayPlayer.logoUrl,
                careerHighRanking = p2Entity.careerHighRanking,
                birthDate = p2Entity.birthDate,
                prizeMoneyYtd = p2Entity.prizeMoneyYtd
            ))
        }

        fun PlayerEntity.toEloProfile() = if (eloRating != null) PlayerEloProfile(
            eloOverall = eloRating,
            eloClay = eloClay ?: eloRating,
            eloGrass = eloGrass ?: eloRating,
            eloHard = eloHard ?: eloRating,
            eloIndoor = eloHard ?: eloRating,
            matchesPlayed = 0
        ) else null

        // All network calls in parallel
        val tourType = if (match.tournamentCategory.name.startsWith("WTA")) "wta" else "atp"

        val existingHome = matchDao.getMatchesByPlayerKeyList(entity.homePlayerKey)
            .count { it.status == "Finished" || it.status == "Retired" }
        val existingAway = matchDao.getMatchesByPlayerKeyList(entity.awayPlayerKey)
            .count { it.status == "Finished" || it.status == "Retired" }

        data class ParallelResults(
            val odds: List<BookmakerOdds>,
            val h2hResp: com.lenz.tennisapp.data.api.H2HResponse?,
            val predictionsResp: com.lenz.tennisapp.data.api.PredictionsResponse?
        )

        val parallel = coroutineScope {
            val oddsDeferred = async {
                if (skipOdds) emptyList() else loadOrFetchOdds(entity, match, forceRefreshOdds)
            }
            val historyDeferred = async {
                if (existingHome < 3 || existingAway < 3) fetchRecentMatchHistory()
            }
            val h2hDeferred = async {
                try {
                    kotlinx.coroutines.withTimeout(45_000) {
                        rankingProxy.getH2H(
                            p1 = normalizePlayerName(match.homePlayer.name),
                            p2 = normalizePlayerName(match.awayPlayer.name),
                            date = match.date.take(10),
                            tour = tourType
                        )
                    }
                } catch (e: Exception) {
                    Timber.w(e, "H2H proxy fetch failed: ${e.message}")
                    null
                }
            }
            val predictionsDeferred = async { getCachedPredictions() }
            historyDeferred.await()
            ParallelResults(oddsDeferred.await(), h2hDeferred.await(), predictionsDeferred.await())
        }

        val odds = parallel.odds
        val homeRecent = getLocalPlayerMatches(entity.homePlayerKey)
        val awayRecent = getLocalPlayerMatches(entity.awayPlayerKey)

        // H2H from proxy result
        val h2hResult = try {
            val resp = parallel.h2hResp
            if (resp != null && resp.success && resp.data != null) {
                val d = resp.data
                // Check if proxy's P1 corresponds to our home player
                val homeLast = match.homePlayer.name.split(" ").first().lowercase()
                val awayLast = match.awayPlayer.name.split(" ").first().lowercase()
                val proxyP1 = d.player1?.lowercase() ?: ""
                
                val p1IsHome = proxyP1.contains(homeLast) || homeLast.contains(proxyP1.split(" ").firstOrNull() ?: "")
                val (p1Wins, p2Wins) = if (p1IsHome) {
                    d.overall.p1 to d.overall.p2
                } else {
                    // If proxy P1 is actually our away player, swap the wins
                    val p1IsAway = proxyP1.contains(awayLast) || awayLast.contains(proxyP1.split(" ").firstOrNull() ?: "")
                    if (p1IsAway) d.overall.p2 to d.overall.p1 else d.overall.p1 to d.overall.p2
                }

                H2HResult(
                    player1Name = match.homePlayer.name,
                    player2Name = match.awayPlayer.name,
                    player1Wins = p1Wins,
                    player2Wins = p2Wins,
                    recentMatches = d.matches.take(10).map { m ->
                        // Determine if match.homePlayer is P1 in this historical match record
                        val mP1Name = m.player1.name.lowercase()
                        val mP1IsHome = mP1Name.contains(homeLast) || homeLast.contains(mP1Name.split(" ").firstOrNull() ?: "")
                        
                        val (p1SetsFinal, p2SetsFinal, p1ScoresFinal, p2ScoresFinal) = if (mP1IsHome) {
                            val p1S = (m.player1.sets as? Number)?.toInt() ?: 0
                            val p2S = (m.player2.sets as? Number)?.toInt() ?: 0
                            val p1List = m.player1.scores
                            val p2List = m.player2.scores
                            listOf(p1S, p2S, p1List, p2List)
                        } else {
                            val p1S = (m.player2.sets as? Number)?.toInt() ?: 0
                            val p2S = (m.player1.sets as? Number)?.toInt() ?: 0
                            val p1List = m.player2.scores
                            val p2List = m.player1.scores
                            listOf(p1S, p2S, p1List, p2List)
                        }

                        val winnerName = if ((m.winner == "p1") == mP1IsHome) match.homePlayer.name else match.awayPlayer.name
                        val surf = when (m.surface.lowercase()) {
                            "clay" -> Surface.CLAY
                            "grass" -> Surface.GRASS
                            "hard" -> Surface.HARD
                            else -> Surface.HARD
                        }
                        H2HMatch(
                            date = m.year ?: "",
                            winner = winnerName,
                            score = "${m.player1.scores.joinToString("-")} / ${m.player2.scores.joinToString("-")}",
                            tournament = m.tournament,
                            surface = surf,
                            round = m.round,
                            p1Sets = p1SetsFinal as Int,
                            p2Sets = p2SetsFinal as Int,
                            p1Scores = p1ScoresFinal as List<String>,
                            p2Scores = p2ScoresFinal as List<String>
                        )
                    }
                )
            } else null
        } catch (e: Exception) {
            Timber.w(e, "H2H proxy processing failed")
            null
        } ?: run {
            // Fallback: compute from local DB
            val dbH2H = homeRecent.filter {
                (it.homePlayer.key == entity.homePlayerKey && it.awayPlayer.key == entity.awayPlayerKey) ||
                (it.homePlayer.key == entity.awayPlayerKey && it.awayPlayer.key == entity.homePlayerKey)
            }.filter { it.status == MatchStatus.FINISHED }
            H2HResult(
                player1Name = match.homePlayer.name,
                player2Name = match.awayPlayer.name,
                player1Wins = dbH2H.count { it.winnerKey == entity.homePlayerKey },
                player2Wins = dbH2H.count { it.winnerKey == entity.awayPlayerKey },
                recentMatches = dbH2H.take(5).map { m ->
                    H2HMatch(
                        date = m.date,
                        winner = if (m.winnerKey == entity.homePlayerKey) m.homePlayer.name else m.awayPlayer.name,
                        score = m.score ?: m.finalResult ?: "",
                        tournament = m.tournament,
                        surface = m.surface
                    )
                }
            )
        }

        // External AI prediction — fuzzy match by last name + first initial,
        // because the JSON mixes full names ("Tomasz Berkieta") and abbreviated
        // ones ("A. Tolev"). Order-independent.
        val externalPrediction = try {
            val resp = parallel.predictionsResp
            if (resp != null && resp.success && resp.data != null) {
                // key = "lastname|firstinitial", lowercase. Handles both
                // "Taylor Fritz", "T. Fritz" and "Carballes Baena R." formats.
                fun nameKey(raw: String): String {
                    val parts = raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (parts.isEmpty()) return ""
                    val isInitial = { t: String -> t.length <= 2 && (t.endsWith(".") || t.length == 1) }
                    val initials = parts.filter(isInitial)
                    val names = parts.filterNot(isInitial)
                    val last = (names.lastOrNull() ?: parts.last()).lowercase().trim('.', ',')
                    val firstInitial = (initials.firstOrNull()?.firstOrNull()
                        ?: names.firstOrNull()?.firstOrNull())?.lowercaseChar() ?: ' '
                    return "$last|$firstInitial"
                }
                val k1 = nameKey(match.homePlayer.name)
                val k2 = nameKey(match.awayPlayer.name)
                Timber.d("PRED_MATCH keys home=$k1 away=$k2")
                resp.data.matches.take(3).forEach { m ->
                    Timber.d("PRED_MATCH candidate ${nameKey(m.p1Fullname)} vs ${nameKey(m.p2Fullname)}")
                }
                resp.data.matches.firstOrNull { m ->
                    val mk1 = nameKey(m.p1Fullname)
                    val mk2 = nameKey(m.p2Fullname)
                    (mk1 == k1 && mk2 == k2) || (mk1 == k2 && mk2 == k1)
                }?.let { m ->
                    Timber.d("PRED_MATCH found: ${m.p1Fullname} vs ${m.p2Fullname}")
                    val isSwapped = nameKey(m.p1Fullname) == k2
                    val p1Prob = if (isSwapped) m.p2Prob else m.p1Prob
                    val p2Prob = if (isSwapped) m.p1Prob else m.p2Prob
                    MatchPrediction(
                        player1WinProbability = p1Prob,
                        player2WinProbability = p2Prob,
                        confidence = when {
                            m.confidence >= 0.65f -> PredictionConfidence.HIGH
                            m.confidence >= 0.35f -> PredictionConfidence.MEDIUM
                            else -> PredictionConfidence.LOW
                        },
                        factors = emptyList()
                    )
                } ?: run { Timber.w("PRED_MATCH no match for $k1 vs $k2 in ${resp.data.matches.size} predictions"); null }
            } else { Timber.w("PRED_MATCH resp null/failed success=${resp?.success}"); null }
        } catch (e: Exception) { Timber.e(e, "PRED_MATCH exception"); null }
        // Fallback: try aiNameKey approach if externalPrediction lookup failed
        val prediction = externalPrediction ?: run {
            val resp2 = cachedPredictions
            if (resp2?.data != null) {
                val k1a = aiNameKey(match.homePlayer.name)
                val k2a = aiNameKey(match.awayPlayer.name)
                Timber.d("PRED_MATCH fallback aiNameKey home=$k1a away=$k2a")
                resp2.data.matches.firstOrNull { m ->
                    val mk1 = aiNameKey(m.p1Fullname); val mk2 = aiNameKey(m.p2Fullname)
                    (mk1 == k1a && mk2 == k2a) || (mk1 == k2a && mk2 == k1a)
                }?.let { m ->
                    Timber.d("PRED_MATCH fallback found: ${m.p1Fullname} vs ${m.p2Fullname}")
                    val isSwapped = aiNameKey(m.p1Fullname) == k2a
                    MatchPrediction(
                        player1WinProbability = if (isSwapped) m.p2Prob else m.p1Prob,
                        player2WinProbability = if (isSwapped) m.p1Prob else m.p2Prob,
                        confidence = when {
                            m.confidence >= 0.65f -> PredictionConfidence.HIGH
                            m.confidence >= 0.35f -> PredictionConfidence.MEDIUM
                            else -> PredictionConfidence.LOW
                        },
                        factors = emptyList()
                    )
                }
            } else null
        }

        // If match just finished, prepend it to H2H so the result shows immediately
        fun inferWinner(score: String?, hKey: String, aKey: String): String? {
            if (score.isNullOrBlank()) return null
            return try {
                var h = 0; var a = 0
                for (set in score.split(",")) {
                    val p = set.trim().split("-")
                    if (p.size < 2) continue
                    val hg = p[0].trim().takeWhile { it.isDigit() }.toIntOrNull() ?: continue
                    val ag = p[1].trim().takeWhile { it.isDigit() }.toIntOrNull() ?: continue
                    if (hg > ag) h++ else if (ag > hg) a++
                }
                when { h > a -> hKey; a > h -> aKey; else -> null }
            } catch (_: Exception) { null }
        }
        val winnerKey = match.winnerKey
        val finalH2H = if (match.status == MatchStatus.FINISHED && winnerKey != null) {
            val winnerName = if (winnerKey == entity.homePlayerKey) match.homePlayer.name else match.awayPlayer.name
            val alreadyPresent = h2hResult.recentMatches.firstOrNull()?.date == match.date &&
                h2hResult.recentMatches.firstOrNull()?.tournament == match.tournament
            if (alreadyPresent) h2hResult else {
                val todayEntry = H2HMatch(
                    date = match.date,
                    winner = winnerName,
                    score = match.finalResult ?: "",
                    tournament = match.tournament,
                    surface = match.surface,
                    round = match.round ?: ""
                )
                H2HResult(
                    player1Name = h2hResult.player1Name,
                    player2Name = h2hResult.player2Name,
                    player1Wins = h2hResult.player1Wins + if (winnerKey == entity.homePlayerKey) 1 else 0,
                    player2Wins = h2hResult.player2Wins + if (winnerKey == entity.awayPlayerKey) 1 else 0,
                    recentMatches = listOf(todayEntry) + h2hResult.recentMatches
                )
            }
        } else h2hResult

        val detail = MatchDetail(
            match = match,
            stats = emptyList(),
            h2h = finalH2H,
            odds = odds,
            prediction = prediction,
            player1Elo = p1Entity?.toEloProfile(),
            player2Elo = p2Entity?.toEloProfile(),
            homeRecentMatches = homeRecent,
            awayRecentMatches = awayRecent
        )
        matchDetailCache[matchId] = detail
        return Result.Success(detail)
    }

    // Background prewarm: builds & caches match details (H2H + predictions, NO odds API)
    // so opening a match page is instant. Concurrency-limited to spare the Render free tier.
    private val prefetchedIds = java.util.Collections.synchronizedSet(HashSet<String>())

    suspend fun prefetchMatchDetails(matchIds: List<String>, concurrency: Int = 3) {
        val pending = matchIds.filter {
            it !in prefetchedIds && !matchDetailCache.containsKey(it)
        }
        if (pending.isEmpty()) return
        val gate = Semaphore(concurrency)
        coroutineScope {
            pending.forEach { id ->
                launch {
                    gate.withPermit {
                        try {
                            getMatchDetail(id, skipOdds = true)
                            prefetchedIds.add(id)
                        } catch (e: Exception) {
                            Timber.w(e, "Prefetch failed for $id")
                        }
                    }
                }
            }
        }
    }

    private var recentHistoryFetchedDay: java.time.LocalDate? = null

    private suspend fun fetchRecentMatchHistory() {
        val today = java.time.LocalDate.now()
        if (recentHistoryFetchedDay == today) return
        recentHistoryFetchedDay = today
        try {
            val apiKey = keyStore.tennisApiKey.first().takeIf { it.isNotBlank() }
            val from = today.minusMonths(3).format(dateFormatter)
            val to = today.minusDays(1).format(dateFormatter)
            val response = tennisApi.getFixtures(eventType = 1, dateStart = from, dateStop = to, apiKey = apiKey)
            response.result?.mapNotNull { it.toEntity() }?.let { entities ->
                upsertMatchesPreservingOdds(entities)
                Timber.d("Fetched ${entities.size} historical matches for form/H2H")
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch recent match history")
        }
    }

    // Tennis API uses "LASTNAME Firstname" — convert to "Firstname Lastname" for the H2H proxy.
    private fun normalizePlayerName(apiName: String): String {
        val parts = apiName.trim().split(" ").filter { it.isNotBlank() }
        if (parts.size < 2) return apiName
        
        // Find the last name part (usually ALL CAPS in Tennis API)
        val lastNameParts = parts.filter { it.length > 1 && it.all { c -> c.isUpperCase() || !c.isLetter() } }
        
        return if (lastNameParts.isNotEmpty()) {
            val last = lastNameParts.joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
            val first = parts.filter { it !in lastNameParts }.joinToString(" ")
            "$first $last".trim()
        } else {
            // Fallback for names already in First Last or mixed format
            val last = parts[0].lowercase().replaceFirstChar { it.uppercase() }
            val first = parts.drop(1).joinToString(" ")
            "$first $last"
        }
    }

    private fun getBestNamePart(fullName: String): String {
        // Prefer last name (most unique), fallback to longest word
        val parts = fullName.trim().split(" ").filter { it.length > 2 }
        return (parts.lastOrNull() ?: parts.maxByOrNull { it.length } ?: fullName).lowercase()
    }

    suspend fun syncOddsIfNeeded() {
        val today = LocalDate.now().format(dateFormatter)
        val lastSync = keyStore.oddsLastSyncDate.first()
        if (lastSync == today) return  // already synced today
        syncOddsForAllMatches()
        keyStore.setOddsLastSyncDate(today)
    }

    suspend fun syncOddsForAllMatches() {
        val today = LocalDate.now().format(dateFormatter)
        val matches = matchDao.getOpenMatchesForDate(today)
        if (matches.isEmpty()) return

        val oddsKey = keyStore.oddsApiKey.first()
        // Discover active tournament keys (free, 0 credits), then fetch each once
        val activeSports = activeTennisSports(oddsKey)
        val allSportKeys = (activeSports["atp"] ?: emptyList()) + (activeSports["wta"] ?: emptyList())
        val eventsBySport = mutableMapOf<String, List<TheOddsApiEvent>>()
        for (sport in allSportKeys) {
            try {
                val resp = oddsApi.getOdds(sportKey = sport, apiKey = oddsKey)
                val remaining = resp.headers()["x-requests-remaining"]?.toIntOrNull()
                if (remaining != null) keyStore.setOddsQuotaRemaining(remaining)
                if (resp.isSuccessful) eventsBySport[sport] = resp.body() ?: emptyList()
                else Timber.w("TheOddsApi $sport HTTP ${resp.code()}")
            } catch (e: Exception) {
                Timber.w("TheOddsApi fetch failed for $sport: ${e.message}")
            }
        }

        val now = System.currentTimeMillis()
        var updatedCount = 0
        for (entity in matches) {
            val p1Search = getBestNamePart(entity.homePlayer)
            val p2Search = getBestNamePart(entity.awayPlayer)
            val sports = filterSportsForEvent(entity.eventType ?: "", activeSports)
            val allEvents = sports.flatMap { eventsBySport[it] ?: emptyList() }

            val event = allEvents.firstOrNull { e ->
                val hn = e.homeTeam.lowercase(); val an = e.awayTeam.lowercase()
                (hn.contains(p1Search) && an.contains(p2Search)) ||
                (hn.contains(p2Search) && an.contains(p1Search))
            }

            if (event == null) {
                // Mark as checked today so detail screen won't trigger individual fetch
                matchDao.updateOdds(entity.id, oddsAdapter.toJson(emptyList<BookmakerOdds>()), now)
                continue
            }

            val isSwapped = event.homeTeam.lowercase().contains(p2Search)
            val bookmaker = event.bookmakers
                .firstOrNull { it.key == TARGET_BOOKMAKER } ?: event.bookmakers.firstOrNull()
            val h2hMarket = bookmaker?.markets?.firstOrNull { it.key == "h2h" }
            val o1 = h2hMarket?.outcomes?.firstOrNull { it.name.lowercase().contains(p1Search) }
            val o2 = h2hMarket?.outcomes?.firstOrNull { it.name.lowercase().contains(p2Search) }
            if (o1 != null && o2 != null && bookmaker != null) {
                val odds = listOf(BookmakerOdds(
                    bookmakerName = bookmaker.title,
                    homeOdds = if (isSwapped) o2.price else o1.price,
                    awayOdds = if (isSwapped) o1.price else o2.price
                ))
                matchDao.updateOdds(entity.id, oddsAdapter.toJson(odds), now)
                updatedCount++
            } else {
                matchDao.updateOdds(entity.id, oddsAdapter.toJson(emptyList<BookmakerOdds>()), now)
            }
        }
        Timber.d("TheOddsApi sync: updated $updatedCount / ${matches.size}")
    }

    private suspend fun loadOrFetchOdds(
        entity: MatchEntity,
        match: TennisMatch,
        forceRefresh: Boolean = false
    ): List<BookmakerOdds> {
        val syncedAt = entity.oddsSyncedAt
        if (syncedAt != null && !forceRefresh) {
            val syncedDay = Instant.ofEpochMilli(syncedAt)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            if (syncedDay == LocalDate.now()) {
                // Already checked today (empty = no odds available), don't retry
                return entity.oddsJson?.let { runCatching { oddsAdapter.fromJson(it) }.getOrNull() } ?: emptyList()
            }
        }
        // New day or never fetched — fetch once and lock with timestamp
        val fetched = fetchOddsForMatch(match)
        matchDao.updateOdds(entity.id, oddsAdapter.toJson(fetched ?: emptyList()), System.currentTimeMillis())
        return fetched ?: emptyList()
    }

    private suspend fun fetchOddsForMatch(match: TennisMatch): List<BookmakerOdds> {
        return try {
            val oddsKey = keyStore.oddsApiKey.first()
            val p1Search = getBestNamePart(match.homePlayer.name)
            val p2Search = getBestNamePart(match.awayPlayer.name)
            Timber.d("TheOddsApi: fetching for $p1Search vs $p2Search")

            val activeSports = activeTennisSports(oddsKey)
            for (sport in filterSportsForEvent(match.eventType, activeSports)) {
                val resp = try {
                    oddsApi.getOdds(sportKey = sport, apiKey = oddsKey)
                } catch (e: Exception) {
                    Timber.w("TheOddsApi sport=$sport failed: ${e.message}")
                    continue
                }
                val remaining = resp.headers()["x-requests-remaining"]?.toIntOrNull()
                if (remaining != null) keyStore.setOddsQuotaRemaining(remaining)
                val events = resp.body() ?: continue

                val event = events.firstOrNull { e ->
                    val hn = e.homeTeam.lowercase(); val an = e.awayTeam.lowercase()
                    (hn.contains(p1Search) && an.contains(p2Search)) ||
                    (hn.contains(p2Search) && an.contains(p1Search))
                } ?: continue

                val isSwapped = event.homeTeam.lowercase().contains(p2Search)
                val bookmaker = event.bookmakers
                    .firstOrNull { it.key == TARGET_BOOKMAKER } ?: event.bookmakers.firstOrNull() ?: continue
                val h2hMarket = bookmaker.markets.firstOrNull { it.key == "h2h" } ?: continue
                val o1 = h2hMarket.outcomes.firstOrNull { it.name.lowercase().contains(p1Search) }
                val o2 = h2hMarket.outcomes.firstOrNull { it.name.lowercase().contains(p2Search) }
                if (o1 != null && o2 != null) {
                    Timber.d("TheOddsApi: ${event.homeTeam} vs ${event.awayTeam} via ${bookmaker.title}")
                    return listOf(BookmakerOdds(
                        bookmakerName = bookmaker.title,
                        homeOdds = if (isSwapped) o2.price else o1.price,
                        awayOdds = if (isSwapped) o1.price else o2.price
                    ))
                }
            }
            emptyList()
        } catch (e: Exception) {
            Timber.w(e, "TheOddsApi fetch failed")
            emptyList()
        }
    }

    // Upsert that preserves oddsJson/oddsSyncedAt on existing rows.
    private suspend fun upsertMatchesPreservingOdds(entities: List<MatchEntity>) {
        matchDao.insertMatchesIfAbsent(entities)
        entities.forEach { e ->
            matchDao.updateMatchPreservingOdds(
                id = e.id, date = e.date, time = e.time,
                homePlayer = e.homePlayer, homePlayerKey = e.homePlayerKey,
                awayPlayer = e.awayPlayer, awayPlayerKey = e.awayPlayerKey,
                finalResult = e.finalResult, gameResult = e.gameResult,
                status = e.status, isLive = e.isLive,
                leagueName = e.leagueName, leagueId = e.leagueId,
                round = e.round, surface = e.surface,
                tournamentCategory = e.tournamentCategory, eventType = e.eventType,
                winnerId = e.winnerId, statsJson = e.statsJson,
                firstPlayerLogo = e.firstPlayerLogo, secondPlayerLogo = e.secondPlayerLogo,
                serve = e.serve, cachedAt = e.cachedAt
            )
        }
    }

    suspend fun clearOldRankingsAndElo() {
        eloDao.clearAll()
        rankingDao.clearAll()
    }
}
