package com.lenz.tennisapp.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TennisFixturesResponse(
    @Json(name = "success") val success: Int,
    @Json(name = "result") val result: List<TennisMatchDto>? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class TennisMatchDto(
    @Json(name = "event_key") val eventKey: Long? = null,
    @Json(name = "event_date") val eventDate: String? = null,
    @Json(name = "event_time") val eventTime: String? = null,
    @Json(name = "event_first_player") val firstPlayer: String? = null,
    @Json(name = "first_player_key") val firstPlayerKey: Long? = null,
    @Json(name = "event_second_player") val secondPlayer: String? = null,
    @Json(name = "second_player_key") val secondPlayerKey: Long? = null,
    @Json(name = "event_final_result") val finalResult: String?,
    @Json(name = "event_game_result") val gameResult: String?,
    @Json(name = "event_serve") val serve: String?,        // "First Player" | "Second Player" | null
    @Json(name = "event_winner") val winner: String?,      // "First Player" | "Second Player" | null
    @Json(name = "event_status") val status: String,       // "" | "Set 1".."Set 5" | "Finished" | "Retired" | "Walkover"
    @Json(name = "event_type_type") val eventTypeType: String, // "Atp Singles" | "Wta Singles" | "Challenger Men Singles" etc.
    @Json(name = "tournament_name") val tournamentName: String,
    @Json(name = "tournament_key") val tournamentKey: Long,
    @Json(name = "tournament_round") val tournamentRound: String?,
    @Json(name = "tournament_season") val tournamentSeason: String?,
    @Json(name = "event_live") val isLive: String,         // "0" | "1"
    @Json(name = "event_first_player_logo") val firstPlayerLogo: String?,
    @Json(name = "event_second_player_logo") val secondPlayerLogo: String?,
    @Json(name = "event_qualification") val qualification: String?,
    @Json(name = "scores") val scores: List<SetScoreDto>?,
    @Json(name = "statistics") val statistics: List<MatchStatDto>?
)

@JsonClass(generateAdapter = true)
data class SetScoreDto(
    @Json(name = "score_first") val scoreFirst: String? = null,
    @Json(name = "score_second") val scoreSecond: String? = null,
    @Json(name = "score_set") val setNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class MatchStatDto(
    @Json(name = "player_key") val playerKey: Long? = null,
    @Json(name = "stat_period") val period: String? = null,
    @Json(name = "stat_type") val type: String? = null,
    @Json(name = "stat_name") val name: String? = null,
    @Json(name = "stat_value") val value: String? = null,
    @Json(name = "stat_won") val won: String? = null,
    @Json(name = "stat_total") val total: String? = null
)

@JsonClass(generateAdapter = true)
data class TennisH2HResponse(
    @Json(name = "success") val success: Int? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "result") val result: H2HResultDto? = null
)

@JsonClass(generateAdapter = true)
data class H2HResultDto(
    @Json(name = "H2H") val h2hMatches: List<TennisMatchDto>? = null,
    @Json(name = "firstPlayerResults") val firstPlayerResults: List<TennisMatchDto>? = null,
    @Json(name = "secondPlayerResults") val secondPlayerResults: List<TennisMatchDto>? = null
)
