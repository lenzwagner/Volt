package com.lenz.tennisapp.data.api

import com.lenz.tennisapp.data.api.dto.OddsEventDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class SportDto(
    @Json(name = "key") val key: String,
    @Json(name = "active") val active: Boolean = false,
    @Json(name = "title") val title: String = ""
)

interface OddsApiService {

    @GET("sports")
    suspend fun getSports(
        @Query("apiKey") apiKey: String,
        @Query("all") all: Boolean = false
    ): List<SportDto>

    // sport: tennis_atp, tennis_wta, tennis_atp_challenger
    @GET("sports/{sport}/odds")
    suspend fun getOdds(
        @Path("sport") sport: String,
        @Query("apiKey") apiKey: String,
        @Query("regions") regions: String = "eu,uk",
        @Query("markets") markets: String = "h2h",
        @Query("oddsFormat") oddsFormat: String = "decimal",
        @Query("dateFormat") dateFormat: String = "iso"
    ): List<OddsEventDto>
}
