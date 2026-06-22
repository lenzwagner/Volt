package com.lenz.tennisapp.data.repository

import com.lenz.tennisapp.data.db.dao.MatchDao
import com.lenz.tennisapp.data.db.dao.PredictionDao
import com.lenz.tennisapp.data.db.entities.UserPredictionEntity
import com.lenz.tennisapp.domain.model.PredictionStats
import com.lenz.tennisapp.domain.model.UserPrediction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionRepository @Inject constructor(
    private val dao: PredictionDao,
    private val matchDao: MatchDao
) {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Save or overwrite a pick. */
    suspend fun savePrediction(
        matchId: String,
        predictedWinnerKey: String,
        predictedWinnerName: String,
        homePlayerKey: String,
        homePlayerName: String,
        awayPlayerKey: String,
        awayPlayerName: String,
        matchDate: String,
        tournamentName: String
    ) {
        dao.savePrediction(
            UserPredictionEntity(
                matchId             = matchId,
                predictedWinnerKey  = predictedWinnerKey,
                predictedWinnerName = predictedWinnerName,
                homePlayerKey       = homePlayerKey,
                homePlayerName      = homePlayerName,
                awayPlayerKey       = awayPlayerKey,
                awayPlayerName      = awayPlayerName,
                matchDate           = matchDate,
                tournamentName      = tournamentName
            )
        )
    }

    fun getAllPredictions(): Flow<List<UserPrediction>> =
        dao.getAllPredictions().map { list -> list.map { it.toDomain() } }

    fun getPredictionMap(): Flow<Map<String, UserPrediction>> =
        dao.getAllPredictions().map { list -> list.associate { it.matchId to it.toDomain() } }

    fun getStats(): Flow<PredictionStats> =
        dao.getAllPredictions().map { all ->
            val now    = LocalDate.now()
            val week7  = now.minusDays(7).format(fmt)
            val month30 = now.minusDays(30).format(fmt)

            val resolved = all.filter { it.isCorrect != null }
            val correct  = all.count { it.isCorrect == true }

            val weekly   = resolved.filter { it.matchDate >= week7 }
            val monthly  = resolved.filter { it.matchDate >= month30 }

            PredictionStats(
                totalResolved   = resolved.size,
                correct         = correct,
                pending         = all.count { it.isCorrect == null },
                weeklyResolved  = weekly.size,
                weeklyCorrect   = weekly.count { it.isCorrect == true },
                monthlyResolved = monthly.size,
                monthlyCorrect  = monthly.count { it.isCorrect == true }
            )
        }

    suspend fun deleteAllPredictions() {
        dao.deleteAllPredictions()
    }

    /**
     * Resolve any pending pick whose match has finished: mark it correct/incorrect
     * by comparing the picked winner to the match's actual winnerId.
     * Falls back to set-count inference if winnerId is missing from the API response.
     */
    suspend fun resolvePending() {
        val finishedStatuses = setOf("finished", "retired", "walkover")
        val pending = dao.getAllPredictions().first().filter { it.isCorrect == null }
        for (p in pending) {
            val match = matchDao.getMatchById(p.matchId) ?: continue
            val isFinished = match.status.lowercase() in finishedStatuses
            if (!isFinished) continue
            val winnerKey = match.winnerId?.takeIf { it.isNotBlank() }
                ?: inferWinnerFromScore(match.finalResult, match.homePlayerKey, match.awayPlayerKey)
            if (winnerKey.isNullOrBlank()) continue
            val correct = winnerKey == p.predictedWinnerKey
            val actualName = if (winnerKey == p.homePlayerKey) p.homePlayerName else p.awayPlayerName
            dao.resolveResult(p.matchId, correct, winnerKey, actualName)
            Timber.d("Resolved prediction ${p.matchId}: correct=$correct winner=$winnerKey")
        }
    }

    private fun inferWinnerFromScore(score: String?, homeKey: String, awayKey: String): String? {
        if (score.isNullOrBlank()) return null
        return try {
            var homeSets = 0; var awaySets = 0
            for (set in score.split(",")) {
                val parts = set.trim().split("-")
                if (parts.size < 2) continue
                val h = parts[0].trim().takeWhile { it.isDigit() }.toIntOrNull() ?: continue
                val a = parts[1].trim().takeWhile { it.isDigit() }.toIntOrNull() ?: continue
                if (h > a) homeSets++ else if (a > h) awaySets++
            }
            when {
                homeSets > awaySets -> homeKey
                awaySets > homeSets -> awayKey
                else -> null
            }
        } catch (e: Exception) { null }
    }

    private fun UserPredictionEntity.toDomain() = UserPrediction(
        matchId             = matchId,
        predictedWinnerKey  = predictedWinnerKey,
        predictedWinnerName = predictedWinnerName,
        homePlayerKey       = homePlayerKey,
        homePlayerName      = homePlayerName,
        awayPlayerKey       = awayPlayerKey,
        awayPlayerName      = awayPlayerName,
        matchDate           = matchDate,
        tournamentName      = tournamentName,
        predictedAt         = predictedAt,
        isCorrect           = isCorrect,
        actualWinnerKey     = actualWinnerKey,
        actualWinnerName    = actualWinnerName
    )
}
