package com.lenz.tennisapp.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerApiResponse(
    @Json(name = "success") val success: Int,
    @Json(name = "result") val result: List<PlayerDto>
)

@JsonClass(generateAdapter = true)
data class PlayerDto(
    @Json(name = "player_key") val playerKey: Long,
    @Json(name = "player_name") val playerName: String,
    @Json(name = "player_full_name") val playerFullName: String? = null,
    @Json(name = "player_country") val playerCountry: String? = null,
    @Json(name = "player_bday") val playerBday: String? = null,
    @Json(name = "player_logo") val playerLogo: String? = null,
    @Json(name = "stats") val stats: List<PlayerSeasonStatDto>? = null,
    @Json(name = "tournaments") val tournaments: List<PlayerTournamentDto>? = null
)

@JsonClass(generateAdapter = true)
data class PlayerSeasonStatDto(
    @Json(name = "season") val season: String,
    @Json(name = "type") val type: String, // "singles" | "doubles"
    @Json(name = "rank") val rank: String? = null,
    @Json(name = "titles") val titles: String? = null,
    @Json(name = "matches_won") val matchesWon: String? = null,
    @Json(name = "matches_lost") val matchesLost: String? = null,
    @Json(name = "hard_won") val hardWon: String? = null,
    @Json(name = "hard_lost") val hardLost: String? = null,
    @Json(name = "clay_won") val clayWon: String? = null,
    @Json(name = "clay_lost") val clayLost: String? = null,
    @Json(name = "grass_won") val grassWon: String? = null,
    @Json(name = "grass_lost") val grassLost: String? = null
)

@JsonClass(generateAdapter = true)
data class PlayerTournamentDto(
    @Json(name = "tournament_name") val tournamentName: String,
    @Json(name = "tournament_key") val tournamentKey: Long,
    @Json(name = "tournament_round") val tournamentRound: String? = null,
    @Json(name = "tournament_season") val tournamentSeason: String? = null
)
