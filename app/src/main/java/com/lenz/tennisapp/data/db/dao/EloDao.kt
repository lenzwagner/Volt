package com.lenz.tennisapp.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.lenz.tennisapp.data.db.entities.EloRatingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EloDao {

    @Upsert
    suspend fun upsert(eloRating: EloRatingEntity)

    @Upsert
    suspend fun upsertElo(rating: EloRatingEntity)

    @Upsert
    suspend fun upsertAll(eloRatings: List<EloRatingEntity>)

    @Update
    suspend fun update(eloRating: EloRatingEntity)

    @Query("SELECT * FROM elo_ratings WHERE playerKey = :playerKey")
    suspend fun getElo(playerKey: String): EloRatingEntity?

    @Query("SELECT * FROM elo_ratings WHERE playerKey = :playerKey")
    suspend fun getEloByPlayerKey(playerKey: String): EloRatingEntity?

    @Query("SELECT * FROM elo_ratings WHERE playerKey = :playerKey")
    fun getEloByPlayerKeyFlow(playerKey: String): Flow<EloRatingEntity?>

    @Query("SELECT * FROM elo_ratings ORDER BY eloOverall DESC")
    fun getAllElos(): Flow<List<EloRatingEntity>>

    @Query("SELECT * FROM elo_ratings ORDER BY eloOverall DESC LIMIT 100")
    fun getTopRatedPlayers(): Flow<List<EloRatingEntity>>

    @Query("SELECT * FROM elo_ratings WHERE LOWER(playerName) LIKE LOWER(:playerName) LIMIT 1")
    suspend fun getEloByPlayerName(playerName: String): EloRatingEntity?

    /** Fuzzy name match – used when API player-key has no ELO entry yet */
    @Query("SELECT * FROM elo_ratings WHERE LOWER(playerName) LIKE LOWER('%' || :lastName || '%') ORDER BY matchesPlayed DESC LIMIT 1")
    suspend fun getEloByLastName(lastName: String): EloRatingEntity?

    @Query("SELECT * FROM elo_ratings")
    suspend fun getAllElo(): List<EloRatingEntity>

    @Query("DELETE FROM elo_ratings")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM elo_ratings")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM elo_ratings")
    suspend fun countAll(): Int
}
