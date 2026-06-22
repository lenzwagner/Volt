package com.lenz.tennisapp.data.api

import com.lenz.tennisapp.data.api.dto.*
import retrofit2.http.GET
import retrofit2.http.Query

interface TennisApiService {

    @GET("?method=get_fixtures")
    suspend fun getFixtures(
        @Query("event_type") eventType: Int,
        @Query("date_start") dateStart: String,
        @Query("date_stop") dateStop: String,
        @Query("APIkey") apiKey: String? = null
    ): TennisApiResponse

    @GET("?method=get_livescore")
    suspend fun getLivescores(
        @Query("event_type") eventType: Int
    ): TennisApiResponse

    @GET("?method=get_event_statistics")
    suspend fun getMatchStatistics(
        @Query("event_key") eventKey: Long
    ): TennisStatsResponse

    @GET("?method=get_players")
    suspend fun getPlayer(
        @Query("player_key") playerKey: String
    ): PlayerApiResponse

    @GET("?method=get_tournaments")
    suspend fun getTournaments(): TournamentInfoResponse
}
