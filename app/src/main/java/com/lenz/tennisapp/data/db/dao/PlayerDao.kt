package com.lenz.tennisapp.data.db.dao

import androidx.room.*
import com.lenz.tennisapp.data.db.entities.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(player: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfAbsent(players: List<PlayerEntity>)

    @Update
    suspend fun update(player: PlayerEntity)

    @Query("SELECT * FROM players WHERE apiKey = :apiKey")
    suspend fun getByApiKey(apiKey: String): PlayerEntity?

    @Query("SELECT * FROM players WHERE lastName = :lastName")
    suspend fun getByLastName(lastName: String): List<PlayerEntity>

    // Used to set eloKey after Elo sync
    @Query("UPDATE players SET eloKey = :eloKey, updatedAt = :now WHERE apiKey = :apiKey")
    suspend fun updateEloKey(apiKey: String, eloKey: String, now: Long = System.currentTimeMillis())

    // Used to set rankingKey after Rankings sync
    @Query("UPDATE players SET rankingKey = :rankingKey, tour = :tour, updatedAt = :now WHERE apiKey = :apiKey")
    suspend fun updateRankingKey(apiKey: String, rankingKey: String, tour: String, now: Long = System.currentTimeMillis())

    // All players without an eloKey — needs matching
    @Query("SELECT * FROM players WHERE eloKey IS NULL")
    suspend fun getUnmatchedElo(): List<PlayerEntity>

    // All players without a rankingKey — needs matching
    @Query("SELECT * FROM players WHERE rankingKey IS NULL")
    suspend fun getUnmatchedRanking(): List<PlayerEntity>

    @Query("SELECT COUNT(*) FROM players")
    suspend fun count(): Int

    @Query("SELECT * FROM players")
    fun getAllPlayers(): Flow<List<PlayerEntity>>
}
