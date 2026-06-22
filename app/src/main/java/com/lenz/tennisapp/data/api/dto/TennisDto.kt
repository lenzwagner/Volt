package com.lenz.tennisapp.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TennisApiResponse(
    // success/error are Any?: the API sends `success` as a JSON number (1), and a
    // non-lenient Moshi throws on a number→String mismatch, which aborts the whole
    // parse and drops `result`. We only rely on `result` being present, so keep
    // these tolerant of whatever type the API returns.
    @Json(name = "success") val success: Any? = null,
    @Json(name = "result") val result: List<TennisMatchDto>? = null,
    @Json(name = "error") val error: Any? = null
)

@JsonClass(generateAdapter = true)
data class TennisStatsResponse(
    @Json(name = "success") val success: Int,
    @Json(name = "result") val result: List<MatchStatDto>
)

@JsonClass(generateAdapter = true)
data class TournamentInfoResponse(
    @Json(name = "success") val success: Int,
    @Json(name = "result") val result: List<TournamentInfoDto>
)

@JsonClass(generateAdapter = true)
data class TournamentInfoDto(
    @Json(name = "tournament_key") val tournamentKey: Long,
    @Json(name = "tournament_name") val tournamentName: String,
    @Json(name = "country_name") val countryName: String? = null,
    @Json(name = "tournament_type") val tournamentType: String? = null
)

@JsonClass(generateAdapter = true)
data class TennisMatchDto(
    @Json(name = "event_key") val eventKey: Long?,
    @Json(name = "event_date") val eventDate: String?,
    @Json(name = "event_time") val eventTime: String?,
    @Json(name = "event_first_player") val firstPlayer: String?,
    @Json(name = "first_player_key") val firstPlayerKey: Long?,
    @Json(name = "event_second_player") val secondPlayer: String?,
    @Json(name = "second_player_key") val secondPlayerKey: Long?,
    @Json(name = "event_final_result") val finalResult: String?,
    @Json(name = "event_game_result") val gameResult: String?,
    @Json(name = "event_serve") val serve: String?,        // "First Player" | "Second Player" | null
    @Json(name = "event_winner") val winner: String?,      // "First Player" | "Second Player" | null
    @Json(name = "event_status") val status: String?,       // "" | "Set 1".."Set 5" | "Finished" | "Retired" | "Walkover"
    @Json(name = "event_type_type") val eventTypeType: String?, // "Atp Singles" | "Wta Singles" | "Challenger Men Singles" etc.
    @Json(name = "tournament_name") val tournamentName: String?,
    @Json(name = "tournament_key") val tournamentKey: Long?,
    @Json(name = "tournament_round") val tournamentRound: String?,
    @Json(name = "tournament_season") val tournamentSeason: String?,
    @Json(name = "event_live") val isLive: String?,         // "0" | "1"
    @Json(name = "event_first_player_logo") val firstPlayerLogo: String?,
    @Json(name = "event_second_player_logo") val secondPlayerLogo: String?,
    @Json(name = "event_qualification") val qualification: String?,
    @Json(name = "scores") val scores: List<SetScoreDto>?,
    @Json(name = "statistics") val statistics: List<MatchStatDto>?
)

@JsonClass(generateAdapter = true)
data class SetScoreDto(
    @Json(name = "score_first") val scoreFirst: String,
    @Json(name = "score_second") val scoreSecond: String,
    @Json(name = "score_set") val setNumber: String
)

@JsonClass(generateAdapter = true)
data class MatchStatDto(
    @Json(name = "player_key") val playerKey: Long,
    @Json(name = "stat_period") val period: String,   // "match" | "set1" etc.
    @Json(name = "stat_type") val type: String?,       // "Service" | "Return" etc.
    @Json(name = "stat_name") val name: String?,       // "Aces" | "Double Faults" etc.
    @Json(name = "stat_value") val value: String?,
    @Json(name = "stat_won") val won: String?,
    @Json(name = "stat_total") val total: String?
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
