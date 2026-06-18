package com.lenz.tennisapp.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_rankings")
data class RankingEntity(
    @PrimaryKey
    val playerKey: String,
    val playerName: String,
    val tour: String,  // "ATP" oder "WTA"
    val ranking: Int,
    val points: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "followed_players")
data class FollowedPlayerEntity(
    @PrimaryKey val playerKey: String,
    val playerName: String,
    val notificationsEnabled: Boolean = true,
    val isFavorite: Boolean = false
)
