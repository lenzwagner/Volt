package com.lenz.tennisapp.data.repository

import com.lenz.tennisapp.data.api.RankingProxyService
import com.lenz.tennisapp.data.api.TennisApiService
import com.lenz.tennisapp.data.api.dto.PlayerDto
import com.lenz.tennisapp.data.api.dto.PlayerSeasonStatDto
import com.lenz.tennisapp.data.api.dto.PlayerTournamentDto
import com.lenz.tennisapp.data.db.dao.PlayerDao
import com.lenz.tennisapp.data.db.entities.PlayerEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import com.lenz.tennisapp.data.api.RankedPlayerDto
import java.lang.reflect.ParameterizedType

@Singleton
class PlayerRepository @Inject constructor(
    private val api: TennisApiService,
    private val rankingProxy: RankingProxyService,
    private val playerDao: PlayerDao,
    private val moshi: Moshi
) {

    fun observePlayer(playerKey: String): Flow<PlayerEntity?> = playerDao.observeByKey(playerKey)

    suspend fun getLastLiveRankingSyncTime(): Long? = playerDao.getLastLiveRankingSyncTime()

    fun searchFlow(query: String): Flow<List<PlayerEntity>> = playerDao.searchFlow(query)

    suspend fun getOrFetchPlayer(playerKey: String, playerType: String? = null): PlayerEntity? {
        val cached = playerDao.getByKey(playerKey)
        if (cached != null && (System.currentTimeMillis() - cached.lastUpdatedAt < 24 * 60 * 60 * 1000)) {
            return cached
        }

        return try {
            val response = api.getPlayer(playerKey)
            val playerDto = response.result.firstOrNull() ?: return cached
            val entity = toEntity(playerDto, playerType)
            playerDao.upsert(entity)
            entity
        } catch (e: Exception) {
            Timber.e(e, "Error fetching player $playerKey")
            cached
        }
    }

    suspend fun syncLiveRankings() {
        try {
            Timber.d("Syncing live rankings...")
            val now = System.currentTimeMillis()
            val atp = rankingProxy.getLiveRankings("atp")
            if (atp.success) applyRankings(atp.data, "atp", true, now)
            
            val wta = rankingProxy.getLiveRankings("wta")
            if (wta.success) applyRankings(wta.data, "wta", true, now)
        } catch (e: Exception) {
            Timber.e(e, "Error syncing live rankings")
        }
    }

    suspend fun syncEloRatings() {
        try {
            Timber.d("Syncing ELO ratings...")
            val now = System.currentTimeMillis()
            
            val tours = listOf("atp", "wta")
            tours.forEach { tour ->
                try {
                    val response = rankingProxy.getElo(tour)
                    if (response.success) {
                        // The proxy keys players by full name ("Ben Shelton") while our DB's
                        // primary key is a numeric playerKey. Resolve the matching player via
                        // a normalized name key before updating.
                        val keyMap = buildNameKeyMap(tour)
                        var matched = 0
                        response.data.forEach { dto ->
                            val playerKey = nameMatchKey(dto.name)?.let { keyMap[it] }
                            if (playerKey != null) {
                                playerDao.updateElo(playerKey, dto.elo, dto.eloHard, dto.eloClay, dto.eloGrass, now)
                                matched++
                            }
                        }
                        Timber.d("Synced ELO for $tour: matched $matched/${response.data.size} players")
                    }
                } catch (e: retrofit2.HttpException) {
                    Timber.e(e, "HTTP Error syncing ELO for $tour: ${e.code()}")
                } catch (e: Exception) {
                    Timber.e(e, "Error syncing ELO for $tour")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Fatal error in syncEloRatings")
        }
    }

    suspend fun syncPrizeMoney() {
        try {
            Timber.d("Syncing prize money rankings...")
            listOf("atp", "wta").forEach { tour ->
                try {
                    val response = rankingProxy.getPrizeMoney(tour)
                    if (response.success) {
                        val keyMap = buildNameKeyMap(tour)
                        var matched = 0
                        response.data.forEach { dto ->
                            if (dto.prizeUsd == null) return@forEach
                            val playerKey = nameMatchKey(dto.name)?.let { keyMap[it] }
                            if (playerKey != null) {
                                playerDao.updatePrizeMoney(playerKey, dto.prizeUsd)
                                matched++
                            }
                        }
                        Timber.d("Synced prize money for $tour: matched $matched/${response.data.size}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error syncing prize money for $tour")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Fatal error in syncPrizeMoney")
        }
    }

    private suspend fun applyRankings(rankings: List<RankedPlayerDto>, tour: String, live: Boolean, now: Long) {
        val keyMap = buildNameKeyMap(tour)
        var matched = 0
        rankings.forEach { r ->
            val playerKey = nameMatchKey(r.name)?.let { keyMap[it] }
            if (playerKey != null) {
                playerDao.updateLiveRanking(playerKey, r.rank, r.points, now)
                r.careerHighRank?.let { playerDao.updateCareerHighRanking(playerKey, it) }
                matched++
            }
        }
        Timber.d("Applied rankings for $tour: matched $matched/${rankings.size} players")
    }

    /**
     * Builds a lookup from a normalized name key to the DB playerKey for one tour.
     * Players with an unknown tour are included so they can still be matched.
     */
    private suspend fun buildNameKeyMap(tour: String): Map<String, String> {
        return playerDao.getAll()
            .filter { it.playerType == null || it.playerType.equals(tour, ignoreCase = true) }
            .mapNotNull { p -> nameMatchKey(p.name)?.let { it to p.playerKey } }
            .toMap()
    }

    companion object {
        /**
         * Normalizes a player name to "surname initial" (lowercase, accent-free) so the
         * proxy format ("Ben Shelton") and the api-tennis format ("B. Shelton") collapse
         * to the same key ("shelton b").
         */
        fun nameMatchKey(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val noAccents = java.text.Normalizer
                .normalize(raw, java.text.Normalizer.Form.NFD)
                .replace("\\p{M}+".toRegex(), "")
            val tokens = noAccents.lowercase()
                .replace(".", " ")
                .split(" ", "-")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (tokens.isEmpty()) return null
            val initial = tokens.first().first()
            val surname = tokens.last()
            return "$surname $initial"
        }
    }

    private fun toEntity(dto: PlayerDto, playerType: String?): PlayerEntity {
        val singleStats = dto.stats?.filter { it.type == "singles" } ?: emptyList()
        val currentSeason = singleStats.maxByOrNull { it.season.toIntOrNull() ?: 0 }
        
        val careerHighRank = singleStats.mapNotNull { it.rank?.toIntOrNull() }.minOrNull()
        val careerTitles = singleStats.sumOf { it.titles?.toIntOrNull() ?: 0 }

        val hardWinRate = currentSeason?.let { winRate(it.hardWon, it.hardLost) }
        val clayWinRate = currentSeason?.let { winRate(it.clayWon, it.clayLost) }
        val grassWinRate = currentSeason?.let { winRate(it.grassWon, it.grassLost) }

        val listType: ParameterizedType = Types.newParameterizedType(List::class.java, PlayerSeasonStatDto::class.java)
        val statsJson = moshi.adapter<List<PlayerSeasonStatDto>>(listType).toJson(dto.stats ?: emptyList())

        val tournamentsType: ParameterizedType = Types.newParameterizedType(List::class.java, PlayerTournamentDto::class.java)
        val tournamentsJson = dto.tournaments?.let {
            moshi.adapter<List<PlayerTournamentDto>>(tournamentsType).toJson(it)
        }

        return PlayerEntity(
            playerKey = dto.playerKey.toString(),
            name = dto.playerName,
            fullName = dto.playerFullName,
            nationality = dto.playerCountry,
            birthDate = dto.playerBday,
            photoUrl = dto.playerLogo,
            playerType = playerType,
            currentRanking = currentSeason?.rank?.toIntOrNull(),
            currentRankingPoints = null,
            currentSeasonTitles = currentSeason?.titles?.toIntOrNull(),
            currentSeasonWins = currentSeason?.matchesWon?.toIntOrNull(),
            currentSeasonLosses = currentSeason?.matchesLost?.toIntOrNull(),
            careerHighRanking = careerHighRank,
            careerTitles = careerTitles,
            hardWinRate = hardWinRate,
            clayWinRate = clayWinRate,
            grassWinRate = grassWinRate,
            statsJson = statsJson,
            tournamentsJson = tournamentsJson,
            lastUpdatedAt = System.currentTimeMillis()
        )
    }

    private fun winRate(won: String?, lost: String?): Double? {
        val w = won?.toIntOrNull() ?: return null
        val l = lost?.toIntOrNull() ?: return null
        val total = w + l
        return if (total > 0) w.toDouble() / total else null
    }
}
