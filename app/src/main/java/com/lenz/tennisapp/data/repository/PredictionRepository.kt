package com.lenz.tennisapp.data.repository

import com.lenz.tennisapp.data.db.dao.PredictionDao
import com.lenz.tennisapp.data.db.entities.UserPredictionEntity
import com.lenz.tennisapp.domain.model.PredictionStats
import com.lenz.tennisapp.domain.model.UserPrediction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionRepository @Inject constructor(
    private val dao: PredictionDao
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
