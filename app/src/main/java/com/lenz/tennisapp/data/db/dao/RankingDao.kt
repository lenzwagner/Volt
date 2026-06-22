package com.lenz.tennisapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lenz.tennisapp.data.db.entities.RankingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RankingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ranking: RankingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rankings: List<RankingEntity>)

    @Query("SELECT * FROM player_rankings WHERE playerKey = :playerKey AND tour = :tour")
    fun getRanking(playerKey: String, tour: String): Flow<RankingEntity?>

    @Query("SELECT * FROM player_rankings WHERE playerKey = :playerKey AND tour = :tour")
    suspend fun getRankingByPlayerAndTour(playerKey: String, tour: String): RankingEntity?

    @Query("SELECT * FROM player_rankings WHERE LOWER(playerName) LIKE LOWER(:playerName) AND tour = :tour LIMIT 1")
    suspend fun getRankingByPlayerNameAndTour(playerName: String, tour: String): RankingEntity?

    @Query("SELECT * FROM player_rankings WHERE tour = :tour ORDER BY ranking ASC")
    fun getRankingsByTour(tour: String): Flow<List<RankingEntity>>

    @Query("SELECT * FROM player_rankings")
    fun getAllRankings(): Flow<List<RankingEntity>>

    @Query("DELETE FROM player_rankings WHERE tour = :tour")
    suspend fun clearTour(tour: String)

    @Query("DELETE FROM player_rankings")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM player_rankings")
    suspend fun count(): Int
}
