package com.lenz.tennisapp.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val playerKey: String,
    val name: String,
    val fullName: String?,
    val nationality: String?,
    val birthDate: String?,
    val photoUrl: String?,
    val playerType: String?, // "atp" | "wta"
    val currentRanking: Int?,
    val currentRankingPoints: Int?,
    val currentSeasonTitles: Int?,
    val currentSeasonWins: Int?,
    val currentSeasonLosses: Int?,
    val careerHighRanking: Int?,
    val careerTitles: Int?,
    val hardWinRate: Double?,
    val clayWinRate: Double?,
    val grassWinRate: Double?,
    val statsJson: String?,       // List<PlayerSeasonStatDto>
    val tournamentsJson: String?, // List<PlayerTournamentDto>
    val liveRanking: Int? = null,
    val liveRankingPoints: Int? = null,
    val liveRankingScrapedAt: Long? = null,
    val eloRating: Int? = null,
    val eloHard: Int? = null,
    val eloClay: Int? = null,
    val eloGrass: Int? = null,
    val eloScrapedAt: Long? = null,
    val prizeMoneyYtd: Int? = null,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
