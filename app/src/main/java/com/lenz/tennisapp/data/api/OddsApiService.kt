package com.lenz.tennisapp.data.api

import com.lenz.tennisapp.data.api.dto.TheOddsApiEvent
import com.lenz.tennisapp.data.api.dto.TheOddsSport
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TheOddsApiService {
    /** List active sports — free, does NOT count against quota. */
    @GET("v4/sports/")
    suspend fun getSports(
        @Query("apiKey") apiKey: String
    ): List<TheOddsSport>

    /**
     * Fetch h2h odds for a sport.
     * sportKey: tournament-specific, e.g. tennis_atp_wimbledon (from getSports)
     * regions: eu → Betfair, Pinnacle, Unibet, William Hill, etc.
     * Response headers expose quota: x-requests-remaining
     */
    @GET("v4/sports/{sportKey}/odds/")
    suspend fun getOdds(
        @Path("sportKey") sportKey: String,
        @Query("apiKey") apiKey: String,
        @Query("regions") regions: String = "eu",
        @Query("markets") markets: String = "h2h",
        @Query("oddsFormat") oddsFormat: String = "decimal",
        @Query("bookmakers") bookmakers: String = "sport888"
    ): Response<List<TheOddsApiEvent>>
}

const val TARGET_BOOKMAKER = "sport888"
