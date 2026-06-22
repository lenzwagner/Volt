package com.lenz.tennisapp.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RankingProxyService {
    @GET("api/elo/{tour}")
    suspend fun getElo(@Path("tour") tour: String): EloResponse

    @GET("api/rankings/{tour}/live")
    suspend fun getLiveRankings(@Path("tour") tour: String): RankingResponse

    @GET("api/player/{tour}/{name}")
    suspend fun getPlayer(
        @Path("tour") tour: String,
        @Path("name") name: String,
        @Query("live") live: Boolean = false
    ): PlayerRankingResponse

    @GET("api/rankings/{tour}")
    suspend fun getRankings(@Path("tour") tour: String): RankingResponse

    @GET("api/sync/all")
    suspend fun syncAll(): SyncAllResponse

    @GET("api/prize/{tour}")
    suspend fun getPrizeMoney(@Path("tour") tour: String): PrizeMoneyResponse

    @GET("api/predictions")
    suspend fun getPredictions(): PredictionsResponse

    @GET("api/grandslam")
    suspend fun getGrandSlam(@Query("player") player: String): GrandSlamResponse

    @GET("api/h2h")
    suspend fun getH2H(
        @Query("p1") p1: String,
        @Query("p2") p2: String,
        @Query("date") date: String,
        @Query("tour") tour: String
    ): H2HResponse
}

@JsonClass(generateAdapter = true)
data class RankingResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<RankedPlayerDto>,
    @Json(name = "cached") val cached: Boolean = false,
    @Json(name = "scrapedAt") val scrapedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RankedPlayerDto(
    @Json(name = "rank") val rank: Int,
    @Json(name = "name") val name: String,
    @Json(name = "points") val points: Int,
    @Json(name = "tour") val tour: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "careerHighRank") val careerHighRank: Int? = null
)

@JsonClass(generateAdapter = true)
data class EloResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<EloPlayerDto>
)

@JsonClass(generateAdapter = true)
data class EloPlayerDto(
    @Json(name = "rank") val rank: Int,
    @Json(name = "name") val name: String,
    @Json(name = "elo") val elo: Int,
    @Json(name = "eloHard") val eloHard: Int? = null,
    @Json(name = "eloClay") val eloClay: Int? = null,
    @Json(name = "eloGrass") val eloGrass: Int? = null
)

@JsonClass(generateAdapter = true)
data class SyncAllResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class PlayerRankingResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: RankedPlayerDto?
)

@JsonClass(generateAdapter = true)
data class PrizeMoneyResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<PrizeMoneyDto>
)

@JsonClass(generateAdapter = true)
data class PrizeMoneyDto(
    @Json(name = "rank") val rank: Int,
    @Json(name = "name") val name: String,
    @Json(name = "prizeRaw") val prizeRaw: String,
    @Json(name = "prizeUsd") val prizeUsd: Int?,
    @Json(name = "tour") val tour: String
)

@JsonClass(generateAdapter = true)
data class GrandSlamResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: GrandSlamData?
)

@JsonClass(generateAdapter = true)
data class GrandSlamData(
    @Json(name = "player") val player: String,
    // year → (slamCode → resultCode), e.g. "2024" → {"AO":"QF","USO":"F"}
    @Json(name = "results") val results: Map<String, Map<String, String>>
)

@JsonClass(generateAdapter = true)
data class PredictionsResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: PredictionsData?
)

@JsonClass(generateAdapter = true)
data class PredictionsData(
    @Json(name = "date") val date: String,
    @Json(name = "generated_at") val generatedAt: String,
    @Json(name = "matches") val matches: List<PredictionMatchDto>
)

@JsonClass(generateAdapter = true)
data class PredictionMatchDto(
    @Json(name = "p1_fullname") val p1Fullname: String,
    @Json(name = "p2_fullname") val p2Fullname: String,
    @Json(name = "p1_prob") val p1Prob: Float,
    @Json(name = "p2_prob") val p2Prob: Float,
    @Json(name = "favorite") val favorite: String,
    @Json(name = "confidence") val confidence: Float
)

@JsonClass(generateAdapter = true)
data class H2HResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: H2HData?
)

@JsonClass(generateAdapter = true)
data class H2HData(
    @Json(name = "overall") val overall: H2HOverall,
    @Json(name = "player1") val player1: String?,
    @Json(name = "player2") val player2: String?,
    @Json(name = "matches") val matches: List<H2HMatch>
)

@JsonClass(generateAdapter = true)
data class H2HOverall(
    @Json(name = "p1") val p1: Int,
    @Json(name = "p2") val p2: Int
)

@JsonClass(generateAdapter = true)
data class H2HMatch(
    @Json(name = "year") val year: String?,
    @Json(name = "tournament") val tournament: String,
    @Json(name = "surface") val surface: String,
    @Json(name = "round") val round: String,
    @Json(name = "winner") val winner: String,
    @Json(name = "player1") val player1: H2HPlayerScore,
    @Json(name = "player2") val player2: H2HPlayerScore
)

@JsonClass(generateAdapter = true)
data class H2HPlayerScore(
    @Json(name = "name") val name: String,
    @Json(name = "sets") val sets: Int,
    @Json(name = "scores") val scores: List<String>
)
