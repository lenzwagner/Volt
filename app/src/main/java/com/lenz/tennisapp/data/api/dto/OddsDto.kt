package com.lenz.tennisapp.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OddsEventDto(
    @Json(name = "id") val id: String,
    @Json(name = "sport_key") val sportKey: String,
    @Json(name = "sport_title") val sportTitle: String,
    @Json(name = "commence_time") val commenceTime: String,
    @Json(name = "home_team") val homeTeam: String,
    @Json(name = "away_team") val awayTeam: String,
    @Json(name = "bookmakers") val bookmakers: List<BookmakerDto>
)

@JsonClass(generateAdapter = true)
data class BookmakerDto(
    @Json(name = "key") val key: String,
    @Json(name = "title") val title: String,
    @Json(name = "last_update") val lastUpdate: String,
    @Json(name = "markets") val markets: List<MarketDto>
)

@JsonClass(generateAdapter = true)
data class MarketDto(
    @Json(name = "key") val key: String,
    @Json(name = "last_update") val lastUpdate: String?,
    @Json(name = "outcomes") val outcomes: List<OutcomeDto>
)

@JsonClass(generateAdapter = true)
data class OutcomeDto(
    @Json(name = "name") val name: String,
    @Json(name = "price") val price: Double
)
