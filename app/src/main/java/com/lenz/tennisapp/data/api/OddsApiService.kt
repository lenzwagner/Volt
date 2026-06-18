package com.lenz.tennisapp.data.api

import com.lenz.tennisapp.data.api.dto.OddsEventDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OddsApiService {

    // sport: tennis_atp, tennis_wta, tennis_challenger_men, tennis_challenger_women
    @GET("sports/{sport}/odds/")
    suspend fun getOdds(
        @Path("sport") sport: String,
        @Query("apiKey") apiKey: String,
        @Query("regions") regions: String = "eu,uk",
        @Query("markets") markets: String = "h2h",
        @Query("oddsFormat") oddsFormat: String = "decimal",
        @Query("dateFormat") dateFormat: String = "iso"
    ): List<OddsEventDto>
}
