package com.lenz.tennisapp.data.scraper

import android.util.Log
import com.lenz.tennisapp.data.db.dao.EloDao
import com.lenz.tennisapp.data.db.entities.EloRatingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TennisAbstractScraper @Inject constructor(
    @Named("tennis") private val okHttpClient: OkHttpClient,
    private val eloDao: EloDao
) {
    companion object {
        private const val TAG = "TennisAbstractScraper"
        private const val ATP_ELO_URL = "https://tennisabstract.com/reports/atp_elo_ratings.html"
        private const val WTA_ELO_URL = "https://tennisabstract.com/reports/wta_elo_ratings.html"
    }

    /** Returns true if scrape produced any Elo data. */
    suspend fun scrapeAndSave(): Boolean = withContext(Dispatchers.IO) {
        var eloSaved = 0
        eloSaved += scrapeEloUrl(ATP_ELO_URL)
        eloSaved += scrapeEloUrl(WTA_ELO_URL)
        Log.i(TAG, "Scraped $eloSaved ELO entries from TennisAbstract")
        eloSaved > 0
    }

    private suspend fun scrapeEloUrl(url: String): Int {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val html = okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return 0
                resp.body?.string() ?: return 0
            }

            parseAndStoreElo(html)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to scrape ELO from $url: ${e.message}")
            0
        }
    }

    private suspend fun parseAndStoreElo(html: String): Int {
        val doc = Jsoup.parse(html)
        val table = doc.select("table#reportable").firstOrNull()
            ?: doc.select("table").firstOrNull { it.select("thead").isNotEmpty() }
            ?: return 0
        val rows = table.select("tr")
        if (rows.isEmpty()) return 0

        var headerRowIndex = -1
        var headers: List<String> = emptyList()
        for ((i, row) in rows.withIndex()) {
            val cells = row.select("th, td").map { it.text().trim().lowercase() }
            if (cells.any { it.contains("player") || it == "name" } &&
                cells.any { it == "elo" || it.contains("elo") }) {
                headers = cells
                headerRowIndex = i
                break
            }
        }

        if (headerRowIndex == -1 || headers.isEmpty()) {
            Log.w(TAG, "Could not find Player/Elo columns in headers: $headers")
            return 0
        }

        val playerIdx    = headers.indexOfFirst { it.contains("player") || it == "name" }
        val eloIdx       = headers.indexOfFirst { it == "elo" }
        // Overall Elo rank is the very first column ("elo rank" or just index 0)
        val rankOverallIdx = headers.indexOfFirst { it.contains("elo") && it.contains("rank") }
            .takeIf { it >= 0 } ?: 0
        // Surface Elo values and their rank columns
        // TennisAbstract layout: hEloRank | hElo | cEloRank | cElo | gEloRank | gElo
        val hardRankIdx  = headers.indexOfFirst { it.contains("helo") && it.contains("rank") }
        val hardIdx      = headers.indexOfFirst { it == "helo" }
            .takeIf { it >= 0 }
            ?: headers.indexOfFirst { it in listOf("hd", "hard", "h elo", "hard elo") }
        val clayRankIdx  = headers.indexOfFirst { it.contains("celo") && it.contains("rank") }
        val clayIdx      = headers.indexOfFirst { it == "celo" }
            .takeIf { it >= 0 }
            ?: headers.indexOfFirst { it in listOf("cy", "clay", "c elo", "clay elo") }
        val grassRankIdx = headers.indexOfFirst { it.contains("gelo") && it.contains("rank") }
        val grassIdx     = headers.indexOfFirst { it == "gelo" }
            .takeIf { it >= 0 }
            ?: headers.indexOfFirst { it in listOf("gs", "grass", "g elo", "grass elo") }
        val peakEloIdx   = headers.indexOfFirst { it.contains("peak") && it.contains("elo") }
        val peakDateIdx  = if (peakEloIdx >= 0) peakEloIdx + 1 else -1

        if (playerIdx == -1 || eloIdx == -1) {
            Log.w(TAG, "Could not find Player/Elo columns in headers: $headers")
            return 0
        }

        var count = 0
        for (row in rows.drop(headerRowIndex + 1)) {
            val cells = row.select("td")
            if (cells.size <= maxOf(playerIdx, eloIdx)) continue

            val playerName = cells.getOrNull(playerIdx)?.text()?.trim() ?: continue
            if (playerName.isBlank() || playerName.equals("Player", ignoreCase = true)) continue

            val eloOverall = cells.getOrNull(eloIdx)?.text()?.replace(",", "")
                ?.toDoubleOrNull() ?: continue

            fun cellDouble(idx: Int) = if (idx >= 0) cells.getOrNull(idx)?.text()
                ?.replace(",", "")?.toDoubleOrNull() else null
            fun cellInt(idx: Int) = if (idx >= 0) cells.getOrNull(idx)?.text()
                ?.trim()?.toIntOrNull() else null

            val eloHard  = cellDouble(hardIdx)  ?: eloOverall
            val eloClay  = cellDouble(clayIdx)  ?: eloOverall
            val eloGrass = cellDouble(grassIdx) ?: eloOverall

            val key = "ta_${playerName.lowercase().replace(Regex("[^a-z0-9]"), "_")}"

            eloDao.upsertElo(
                EloRatingEntity(
                    playerKey    = key,
                    playerName   = playerName,
                    eloOverall   = eloOverall,
                    eloHard      = eloHard,
                    eloClay      = eloClay,
                    eloGrass     = eloGrass,
                    eloIndoor    = eloHard,
                    rankOverall  = cellInt(rankOverallIdx),
                    rankHard     = cellInt(hardRankIdx),
                    rankClay     = cellInt(clayRankIdx),
                    rankGrass    = cellInt(grassRankIdx),
                    peakElo      = cellDouble(peakEloIdx),
                    peakEloDate  = if (peakDateIdx >= 0) cells.getOrNull(peakDateIdx)?.text()?.trim() else null,
                    matchesPlayed = 200
                )
            )
            count++
        }
        return count
    }
}
