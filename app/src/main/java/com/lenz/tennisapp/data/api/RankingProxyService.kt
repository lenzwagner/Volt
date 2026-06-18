package com.lenz.tennisapp.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class ProxyRankingsResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: List<ProxyPlayerRanking>? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ProxyPlayerRanking(
    @Json(name = "rank") val rank: Int,
    @Json(name = "name") val name: String,
    @Json(name = "points") val points: Int,
    @Json(name = "tour") val tour: String
)

interface RankingProxyService {
    @GET("api/rankings/atp")
    suspend fun getAtpRankings(): ProxyRankingsResponse

    @GET("api/rankings/wta")
    suspend fun getWtaRankings(): ProxyRankingsResponse
}
