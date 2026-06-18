package com.lenz.tennisapp.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val date: String,
    val time: String,
    val homePlayer: String,
    val homePlayerKey: String,
    val awayPlayer: String,
    val awayPlayerKey: String,
    val finalResult: String?,
    val gameResult: String?,             // current game score "40-15", "AD-40" etc.
    val status: String,                  // "" | "Set 1".."Set 5" | "Finished" | "Retired"
    val isLive: Boolean,
    val leagueName: String,
    val leagueId: String,                // tournamentKey_eventTypeType (unique per bracket)
    val round: String?,
    val surface: String?,                // Surface enum name
    val tournamentCategory: String,      // TournamentCategory enum name
    val winnerId: String?,               // homePlayerKey or awayPlayerKey when finished
    val statsJson: String?,              // serialized List<MatchStatDto> for detail screen
    val firstPlayerLogo: String?,
    val secondPlayerLogo: String?,
    val serve: String? = null,           // "First Player" | "Second Player" during live
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "elo_ratings")
data class EloRatingEntity(
    @PrimaryKey val playerKey: String,
    val playerName: String,
    val eloOverall: Double? = null,
    val eloClay: Double? = null,
    val eloGrass: Double? = null,
    val eloHard: Double? = null,
    val eloIndoor: Double? = null,
    // Elo ranking positions (e.g. #3 overall, #4 on grass)
    val rankOverall: Int? = null,
    val rankHard: Int? = null,
    val rankClay: Int? = null,
    val rankGrass: Int? = null,
    val peakElo: Double? = null,
    val peakEloDate: String? = null,
    val matchesPlayed: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_predictions")
data class UserPredictionEntity(
    @PrimaryKey val matchId: String,
    val predictedWinnerKey: String,
    val predictedWinnerName: String,
    val homePlayerKey: String,
    val homePlayerName: String,
    val awayPlayerKey: String,
    val awayPlayerName: String,
    val matchDate: String,
    val tournamentName: String,
    val predictedAt: Long = System.currentTimeMillis(),
    val isCorrect: Boolean? = null,       // null = pending
    val actualWinnerKey: String? = null,
    val actualWinnerName: String? = null
)

@Entity(tableName = "notified_matches")
data class NotifiedMatchEntity(
    @PrimaryKey val matchId: String,
    val notifiedAt: Long = System.currentTimeMillis()
)
