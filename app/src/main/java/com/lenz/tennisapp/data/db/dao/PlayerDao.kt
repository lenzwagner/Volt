package com.lenz.tennisapp.data.db.dao

import androidx.room.*
import com.lenz.tennisapp.data.db.entities.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Upsert
    suspend fun upsert(player: PlayerEntity)

    @Upsert
    suspend fun upsertAll(players: List<PlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(players: List<PlayerEntity>)

    @Query("SELECT * FROM players WHERE playerKey = :key")
    suspend fun getByKey(key: String): PlayerEntity?

    @Query("SELECT * FROM players WHERE playerKey IN (:keys)")
    suspend fun getByKeys(keys: List<String>): List<PlayerEntity>

    @Query("SELECT * FROM players")
    suspend fun getAll(): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE playerKey = :key")
    fun observeByKey(key: String): Flow<PlayerEntity?>

    @Query("SELECT * FROM players WHERE name LIKE '%' || :query || '%' OR fullName LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun search(query: String, limit: Int = 30): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE name LIKE '%' || :query || '%' OR fullName LIKE '%' || :query || '%' LIMIT :limit")
    fun searchFlow(query: String, limit: Int = 30): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE liveRankingScrapedAt IS NULL OR liveRankingScrapedAt < :cutoff")
    suspend fun getStaleForLiveRanking(cutoff: Long): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE eloScrapedAt IS NULL OR eloScrapedAt < :cutoff")
    suspend fun getStaleForElo(cutoff: Long): List<PlayerEntity>

    @Query("SELECT MAX(liveRankingScrapedAt) FROM players")
    suspend fun getLastLiveRankingSyncTime(): Long?

    @Query("UPDATE players SET liveRanking = :rank, liveRankingPoints = :points, liveRankingScrapedAt = :scrapedAt, lastUpdatedAt = :scrapedAt WHERE playerKey = :key")
    suspend fun updateLiveRanking(key: String, rank: Int?, points: Int?, scrapedAt: Long)

    @Query("UPDATE players SET careerHighRanking = :careerHighRank WHERE playerKey = :key AND (careerHighRanking IS NULL OR careerHighRanking > :careerHighRank)")
    suspend fun updateCareerHighRanking(key: String, careerHighRank: Int)

    @Query("UPDATE players SET eloRating = :elo, eloHard = :eloHard, eloClay = :eloClay, eloGrass = :eloGrass, eloScrapedAt = :scrapedAt, lastUpdatedAt = :scrapedAt WHERE playerKey = :key")
    suspend fun updateElo(key: String, elo: Int?, eloHard: Int?, eloClay: Int?, eloGrass: Int?, scrapedAt: Long)

    @Query("UPDATE players SET prizeMoneyYtd = :prizeUsd WHERE playerKey = :key")
    suspend fun updatePrizeMoney(key: String, prizeUsd: Int?)
}
