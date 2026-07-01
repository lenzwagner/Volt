package com.lenz.tennisapp.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TheOddsSport(
    @Json(name = "key") val key: String,
    @Json(name = "group") val group: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "active") val active: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TheOddsApiEvent(
    @Json(name = "id") val id: String,
    @Json(name = "sport_key") val sportKey: String,
    @Json(name = "commence_time") val commenceTime: String,
    @Json(name = "home_team") val homeTeam: String,
    @Json(name = "away_team") val awayTeam: String,
    @Json(name = "bookmakers") val bookmakers: List<TheOddsBookmaker> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TheOddsBookmaker(
    @Json(name = "key") val key: String,
    @Json(name = "title") val title: String,
    @Json(name = "markets") val markets: List<TheOddsMarket> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TheOddsMarket(
    @Json(name = "key") val key: String,
    @Json(name = "outcomes") val outcomes: List<TheOddsOutcome> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TheOddsOutcome(
    @Json(name = "name") val name: String,
    @Json(name = "price") val price: Double
)

// Legacy DTOs kept for OddsBlaze (unused path, left for build compat)
@JsonClass(generateAdapter = true)
data class OddsBlazResponse(
    @Json(name = "updated") val updated: String = "",
    @Json(name = "events") val events: List<OddsBlazEvent> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OddsBlazEvent(
    @Json(name = "id") val id: String,
    @Json(name = "teams") val teams: OddsBlazTeams,
    @Json(name = "date") val date: String,
    @Json(name = "live") val live: Boolean = false,
    @Json(name = "odds") val odds: List<OddsBlazOdd> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OddsBlazTeams(
    @Json(name = "home") val home: OddsBlazTeam,
    @Json(name = "away") val away: OddsBlazTeam
)

@JsonClass(generateAdapter = true)
data class OddsBlazTeam(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String,
    @Json(name = "abbreviation") val abbreviation: String = ""
)

@JsonClass(generateAdapter = true)
data class OddsBlazOdd(
    @Json(name = "market") val market: String,
    @Json(name = "name") val name: String,
    @Json(name = "price") val price: String,
    @Json(name = "main") val main: Boolean = false
)

fun americanToDecimal(price: String): Double {
    val n = price.replace("+", "").toIntOrNull() ?: return 0.0
    return if (n > 0) (n + 100.0) / 100.0 else (100.0 / (-n)) + 1.0
}
