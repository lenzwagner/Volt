package com.lenz.tennisapp.data.scraper

import android.util.Log
import com.lenz.tennisapp.data.db.dao.EloDao
import com.lenz.tennisapp.data.db.dao.RankingDao
import com.lenz.tennisapp.data.db.entities.EloRatingEntity
import com.lenz.tennisapp.data.db.entities.RankingEntity
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
    private val eloDao: EloDao,
    private val rankingDao: RankingDao
) {
    companion object {
        private const val TAG = "TennisAbstractScraper"
        // Elo URLs
        private const val ATP_ELO_URL = "https://tennisabstract.com/reports/atp_elo_ratings.html"
        private const val WTA_ELO_URL = "https://tennisabstract.com/reports/wta_elo_ratings.html"
        // Ranking URLs
        private const val ATP_RANKINGS_URL = "https://tennisabstract.com/reports/atpRankings.html"
        private const val WTA_RANKINGS_URL = "https://tennisabstract.com/reports/wtaRankings.html"
    }

    /** Returns true if scrape produced any data. */
    suspend fun scrapeAndSave(): Boolean = withContext(Dispatchers.IO) {
        var eloSaved = 0
        eloSaved += scrapeEloUrl(ATP_ELO_URL)
        eloSaved += scrapeEloUrl(WTA_ELO_URL)
        Log.i(TAG, "Scraped $eloSaved ELO entries from TennisAbstract")

        var rankingsSaved = 0
        rankingsSaved += scrapeRankingsUrl(ATP_RANKINGS_URL, "ATP")
        rankingsSaved += scrapeRankingsUrl(WTA_RANKINGS_URL, "WTA")
        Log.i(TAG, "Scraped $rankingsSaved RANKINGS entries from TennisAbstract")

        (eloSaved + rankingsSaved) > 0
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

    private suspend fun scrapeRankingsUrl(url: String, tour: String): Int {
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

            parseAndStoreRankings(html, tour)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to scrape rankings from $url: ${e.message}")
            0
        }
    }

    private suspend fun parseAndStoreElo(html: String): Int {
        val doc = Jsoup.parse(html)
        // The data table has id="reportable"; fallback to any table with a <thead>
        val table = doc.select("table#reportable").firstOrNull()
            ?: doc.select("table").firstOrNull { it.select("thead").isNotEmpty() }
            ?: return 0
        val rows = table.select("tr")
        if (rows.isEmpty()) return 0

        // ── Find actual header row (scan for the row containing "Player" and "Elo") ──
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

        val playerIdx = headers.indexOfFirst { it.contains("player") || it == "name" }
        val eloIdx    = headers.indexOfFirst { it == "elo" }
        // Surface-specific columns – tennisabstract uses "hd", "cy", "gs" abbreviations
        // or full names. Accept a range of names.
        // tennisabstract columns: "helo", "celo", "gelo" (Jsoup normalises &nbsp; to space)
        // We need the actual rating columns, not the rank columns ("helo rank", "celo rank")
        val hardIdx   = headers.indexOfFirst { it == "helo" }
            .takeIf { it >= 0 }
            ?: headers.indexOfFirst { it in listOf("hd", "hard", "h elo", "hard elo") }
        val clayIdx   = headers.indexOfFirst { it == "celo" }
            .takeIf { it >= 0 }
            ?: headers.indexOfFirst { it in listOf("cy", "clay", "c elo", "clay elo") }
        val grassIdx  = headers.indexOfFirst { it == "gelo" }
            .takeIf { it >= 0 }
            ?: headers.indexOfFirst { it in listOf("gs", "grass", "g elo", "grass elo") }

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

            val eloHard  = if (hardIdx >= 0) cells.getOrNull(hardIdx)?.text()
                ?.replace(",", "")?.toDoubleOrNull() ?: eloOverall else eloOverall
            val eloClay  = if (clayIdx >= 0) cells.getOrNull(clayIdx)?.text()
                ?.replace(",", "")?.toDoubleOrNull() ?: eloOverall else eloOverall
            val eloGrass = if (grassIdx >= 0) cells.getOrNull(grassIdx)?.text()
                ?.replace(",", "")?.toDoubleOrNull() ?: eloOverall else eloOverall

            // Key: "ta_" prefix + lowercase name with spaces replaced → easy lookup
            val key = "ta_${playerName.lowercase().replace(Regex("[^a-z0-9]"), "_")}"

            eloDao.upsertElo(
                EloRatingEntity(
                    playerKey      = key,
                    playerName     = playerName,
                    eloOverall     = eloOverall,
                    eloHard        = eloHard,
                    eloClay        = eloClay,
                    eloGrass       = eloGrass,
                    eloIndoor      = eloHard,    // no indoor-specific column; use hard
                    matchesPlayed  = 200         // high value → HIGH confidence badge
                )
            )
            count++
        }
        return count
    }

    private suspend fun parseAndStoreRankings(html: String, tour: String): Int {
        val doc = Jsoup.parse(html)
        // The data table has id="reportable"; fallback to any table with a <thead>
        val table = doc.select("table#reportable").firstOrNull()
            ?: doc.select("table").firstOrNull { it.select("thead").isNotEmpty() }
            ?: return 0
        val rows = table.select("tr")
        if (rows.isEmpty()) return 0

        // ── Find actual header row (scan for the row containing "Rank" and "Player") ──
        var headerRowIndex = -1
        var headers: List<String> = emptyList()
        for ((i, row) in rows.withIndex()) {
            val cells = row.select("th, td").map { it.text().trim().lowercase() }
            if (cells.any { it == "rank" || it.contains("rank") } &&
                cells.any { it.contains("player") || it == "name" }) {
                headers = cells
                headerRowIndex = i
                break
            }
        }

        if (headerRowIndex == -1 || headers.isEmpty()) {
            Log.w(TAG, "Could not find Rank/Player columns in headers: $headers")
            return 0
        }

        val rankIdx = headers.indexOfFirst { it == "rank" || it.contains("rank") }
        val playerIdx = headers.indexOfFirst { it.contains("player") || it == "name" }
        val pointsIdx = headers.indexOfFirst { it == "points" || it.contains("points") }

        if (rankIdx == -1 || playerIdx == -1) {
            Log.w(TAG, "Could not find Rank/Player columns in headers: $headers (rankIdx=$rankIdx, playerIdx=$playerIdx)")
            return 0
        }

        var count = 0
        for (row in rows.drop(headerRowIndex + 1)) {
            val cells = row.select("td")
            if (cells.size <= maxOf(rankIdx, playerIdx)) continue

            val rankingStr = cells.getOrNull(rankIdx)?.text()?.trim()?.replace(",", "")?.replace(".", "") ?: continue
            val ranking = rankingStr.toIntOrNull() ?: continue

            // Extract player name - look for link text if available, otherwise use td text
            val playerTd = cells.getOrNull(playerIdx) ?: continue
            val rawPlayerName = playerTd.selectFirst("a")?.text()?.trim()
                ?: playerTd.text().trim()

            // Clean up name: replace non-breaking spaces and extra whitespace
            val playerName = rawPlayerName
                .replace(" ", " ") // non-breaking space
                .replace("&nbsp;", " ")
                .replace(Regex("\\s+"), " ") // normalize multiple spaces
                .trim()

            if (playerName.isBlank() || playerName.equals("Player", ignoreCase = true)) continue

            // If there's a points column, use it; otherwise calculate from ranking
            val points = if (pointsIdx >= 0 && pointsIdx < cells.size) {
                val pointsStr = cells.getOrNull(pointsIdx)?.text()?.trim()?.replace(",", "") ?: ""
                pointsStr.toIntOrNull() ?: (10000 - (ranking * 10)) // Fallback calculation
            } else {
                // Calculate points based on ranking (higher rank = more points)
                10000 - (ranking * 10)
            }

            // Key: "ta_" prefix + lowercase name with spaces replaced → easy lookup
            val key = "ta_${playerName.lowercase().replace(Regex("[^a-z0-9]"), "_")}"

            rankingDao.upsert(
                RankingEntity(
                    playerKey = key,
                    playerName = playerName,
                    tour = tour,
                    ranking = ranking,
                    points = points,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            count++
        }
        Log.d(TAG, "Parsed and stored $count $tour rankings")
        return count
    }
}
