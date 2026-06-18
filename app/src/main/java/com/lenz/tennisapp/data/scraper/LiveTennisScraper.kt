package com.lenz.tennisapp.data.scraper

import com.lenz.tennisapp.data.api.RankingProxyService
import com.lenz.tennisapp.data.db.dao.RankingDao
import com.lenz.tennisapp.data.db.entities.RankingEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerRankingInfo(
    val playerName: String,
    val ranking: Int,
    val points: Int
)

data class PlayerPrizeMoney(
    val playerName: String,
    val prizeMoneyCurrentYear: Long
)

@Singleton
class LiveTennisScraper @Inject constructor(
    private val proxyService: RankingProxyService,
    private val rankingDao: RankingDao
) {
    companion object {
        private const val TAG = "LiveTennisScraper"
    }

    /** Fetches ATP + WTA rankings from the Render proxy and saves them to DB. */
    suspend fun scrapeAndSave(): Boolean {
        var atpSaved = 0
        var wtaSaved = 0
        try {
            atpSaved = fetchAndStore("ATP")
            wtaSaved = fetchAndStore("WTA")
        } catch (e: Exception) {
            Timber.e(e, "LiveTennisScraper failed")
        }
        Timber.i("Saved $atpSaved ATP + $wtaSaved WTA ranking entries from proxy")
        return (atpSaved + wtaSaved) > 0
    }

    private suspend fun fetchAndStore(tour: String): Int {
        return try {
            val response = if (tour == "ATP") proxyService.getAtpRankings()
                          else proxyService.getWtaRankings()

            if (!response.success || response.data.isNullOrEmpty()) {
                Timber.w("Proxy returned no $tour data")
                return 0
            }

            val entities = response.data.map { p ->
                val key = "lt_${p.name.lowercase().replace(Regex("[^a-z0-9]"), "_")}"
                RankingEntity(
                    playerKey   = key,
                    playerName  = p.name,
                    tour        = tour,
                    ranking     = p.rank,
                    points      = p.points,
                    lastUpdated = System.currentTimeMillis()
                )
            }

            rankingDao.clearTour(tour)
            rankingDao.upsertAll(entities)
            Timber.d("Stored ${entities.size} $tour rankings")
            entities.size
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch $tour rankings from proxy")
            0
        }
    }
}
