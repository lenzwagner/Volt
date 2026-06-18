package com.lenz.tennisapp.data.db.dao

import androidx.room.*
import com.lenz.tennisapp.data.db.entities.FollowedPlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowedPlayerDao {
    @Query("SELECT * FROM followed_players")
    fun getAllFollowedPlayers(): Flow<List<FollowedPlayerEntity>>

    @Query("SELECT * FROM followed_players WHERE playerKey = :playerKey")
    suspend fun getFollowedPlayer(playerKey: String): FollowedPlayerEntity?

    @Query("SELECT * FROM followed_players WHERE playerKey = :playerKey")
    fun getFollowedPlayerFlow(playerKey: String): Flow<FollowedPlayerEntity?>

    @Upsert
    suspend fun upsert(player: FollowedPlayerEntity)

    @Delete
    suspend fun delete(player: FollowedPlayerEntity)

    @Query("DELETE FROM followed_players WHERE playerKey = :playerKey")
    suspend fun deleteByKey(playerKey: String)

    @Query("SELECT EXISTS(SELECT 1 FROM followed_players WHERE playerKey = :playerKey AND notificationsEnabled = 1)")
    fun isNotificationsEnabled(playerKey: String): Flow<Boolean>
}
