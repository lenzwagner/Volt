package com.lenz.tennisapp.data.api

import com.lenz.tennisapp.data.api.dto.*
import retrofit2.http.GET
import retrofit2.http.Query

interface TennisApiService {

    @GET(".")
    suspend fun getFixtures(
        @Query("method") method: String = "get_fixtures",
        @Query("APIkey") apiKey: String,
        @Query("date_start") dateStart: String,
        @Query("date_stop") dateStop: String
    ): TennisFixturesResponse

    @GET(".")
    suspend fun getFixturesByLeague(
        @Query("method") method: String = "get_fixtures",
        @Query("APIkey") apiKey: String,
        @Query("league_id") leagueId: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): TennisFixturesResponse

    @GET(".")
    suspend fun getLivescores(
        @Query("method") method: String = "get_livescore",
        @Query("APIkey") apiKey: String
    ): TennisFixturesResponse

    @GET(".")
    suspend fun getH2H(
        @Query("method") method: String = "get_H2H",
        @Query("APIkey") apiKey: String,
        @Query("first_player_key") player1Key: String,
        @Query("second_player_key") player2Key: String
    ): TennisH2HResponse

    @GET(".")
    suspend fun getPlayerMatches(
        @Query("method") method: String = "get_events",
        @Query("APIkey") apiKey: String,
        @Query("player_key") playerKey: String
    ): TennisFixturesResponse
}
