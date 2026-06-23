package com.lenz.tennisapp.data.api

import com.lenz.tennisapp.data.api.dto.OddsBlazResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OddsBlazService {
    @GET("/")
    suspend fun getOdds(
        @Query("sportsbook") sportsbook: String = "draftkings",
        @Query("league") league: String,
        @Query("key") key: String
    ): OddsBlazResponse
}
