package com.lenz.tennisapp.data.db.dao

import androidx.room.*
import com.lenz.tennisapp.data.db.entities.MatchEntity
import com.lenz.tennisapp.data.db.entities.NotifiedMatchEntity
import com.lenz.tennisapp.data.db.entities.UserPredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches WHERE date = :date ORDER BY tournamentCategory, leagueName, time")
    fun getMatchesForDate(date: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE isLive = 1 ORDER BY date ASC, time ASC")
    fun getLiveMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: String): MatchEntity?

    @Query("SELECT * FROM matches WHERE leagueId = :leagueId ORDER BY date ASC, time ASC")
    fun getMatchesByLeagueId(leagueId: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE homePlayerKey = :playerKey OR awayPlayerKey = :playerKey ORDER BY date DESC, time DESC")
    suspend fun getMatchesByPlayerKeyList(playerKey: String): List<MatchEntity>

    @Upsert
    suspend fun upsertMatches(matches: List<MatchEntity>)

    @Query("DELETE FROM matches WHERE date = :date")
    suspend fun deleteMatchesForDate(date: String)

    @Query("SELECT MAX(cachedAt) FROM matches WHERE date = :date")
    suspend fun getLatestCachedAt(date: String): Long?

    @Query("DELETE FROM matches WHERE cachedAt < :timestamp")
    suspend fun deleteOldMatches(timestamp: Long)
}

@Dao
interface PredictionDao {
    @Upsert
    suspend fun savePrediction(prediction: UserPredictionEntity)

    @Query("SELECT * FROM user_predictions WHERE matchId = :matchId")
    suspend fun getPrediction(matchId: String): UserPredictionEntity?

    @Query("SELECT * FROM user_predictions ORDER BY predictedAt DESC")
    fun getAllPredictions(): Flow<List<UserPredictionEntity>>

    @Query("SELECT * FROM user_predictions WHERE matchDate >= :fromDate ORDER BY predictedAt DESC")
    fun getPredictionsSince(fromDate: String): Flow<List<UserPredictionEntity>>

    @Query("""
        UPDATE user_predictions
        SET isCorrect = :isCorrect, actualWinnerKey = :actualWinnerKey, actualWinnerName = :actualWinnerName
        WHERE matchId = :matchId AND isCorrect IS NULL
    """)
    suspend fun resolveResult(matchId: String, isCorrect: Boolean, actualWinnerKey: String, actualWinnerName: String)

    @Query("SELECT COUNT(*) FROM user_predictions WHERE isCorrect IS NULL")
    fun countPending(): Flow<Int>

    @Query("DELETE FROM user_predictions")
    suspend fun deleteAllPredictions()
}

@Dao
interface NotifiedMatchDao {
    @Query("SELECT EXISTS(SELECT 1 FROM notified_matches WHERE matchId = :matchId)")
    suspend fun wasNotified(matchId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markAsNotified(match: NotifiedMatchEntity)

    @Query("DELETE FROM notified_matches WHERE notifiedAt < :timestamp")
    suspend fun cleanupOld(timestamp: Long)
}
