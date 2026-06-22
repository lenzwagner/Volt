package com.lenz.tennisapp.data.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import timber.log.Timber

data class PlayerRankingInfo(
    val playerName: String,
    val ranking: Int,
    val points: Int
)

data class PlayerPrizeMoney(
    val playerName: String,
    val prizeMoneyCurrentYear: Long  // in Euro
)

object LiveTennisScraper {

    suspend fun getATPRanking(playerName: String): PlayerRankingInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://live-tennis.eu/en/atp-live-ranking"
            val doc = Jsoup.connect(url).timeout(10000).get()

            val rows = doc.select("table tbody tr")
            rows.forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 5) { // Increased minimum cells
                    val name = cells[1].text().trim()
                    if (name.contains(playerName, ignoreCase = true)) {
                        val ranking = cells[0].text().trim().toIntOrNull() ?: return@forEach
                        // Points are usually in the 4th or 5th column
                        val points = cells.drop(3).mapNotNull { 
                            it.text().trim().replace(".", "").replace(",", "").toIntOrNull() 
                        }.firstOrNull() ?: 0
                        return@withContext PlayerRankingInfo(name, ranking, points)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "Error scraping ATP ranking for $playerName")
            null
        }
    }

    suspend fun getWTARanking(playerName: String): PlayerRankingInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://live-tennis.eu/en/wta-live-ranking"
            val doc = Jsoup.connect(url).timeout(10000).get()

            val rows = doc.select("table tbody tr")
            rows.forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 5) { // Increased minimum cells
                    val name = cells[1].text().trim()
                    if (name.contains(playerName, ignoreCase = true)) {
                        val ranking = cells[0].text().trim().toIntOrNull() ?: return@forEach
                        // Points are usually in the 4th or 5th column
                        val points = cells.drop(3).mapNotNull { 
                            it.text().trim().replace(".", "").replace(",", "").toIntOrNull() 
                        }.firstOrNull() ?: 0
                        return@withContext PlayerRankingInfo(name, ranking, points)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "Error scraping WTA ranking for $playerName")
            null
        }
    }

    suspend fun getATPPrizeMoney(playerName: String): PlayerPrizeMoney? = withContext(Dispatchers.IO) {
        try {
            val url = "https://live-tennis.eu/de/atp-preisgeld-des-laufenden-jahres"
            val doc = Jsoup.connect(url).timeout(10000).get()

            val rows = doc.select("table tbody tr")
            rows.forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 3) {
                    val name = cells[1].text().trim()
                    if (name.contains(playerName, ignoreCase = true)) {
                        val moneyStr = cells[2].text().trim()
                            .replace("€", "")
                            .replace(".", "")
                            .replace(",", "")
                            .trim()
                        val money = moneyStr.toLongOrNull() ?: 0L
                        return@withContext PlayerPrizeMoney(name, money)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "Error scraping ATP prize money for $playerName")
            null
        }
    }

    suspend fun getWTAPrizeMoney(playerName: String): PlayerPrizeMoney? = withContext(Dispatchers.IO) {
        try {
            val url = "https://live-tennis.eu/de/wta-preisgeld-des-laufenden-jahres"
            val doc = Jsoup.connect(url).timeout(10000).get()

            val rows = doc.select("table tbody tr")
            rows.forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 3) {
                    val name = cells[1].text().trim()
                    if (name.contains(playerName, ignoreCase = true)) {
                        val moneyStr = cells[2].text().trim()
                            .replace("€", "")
                            .replace(".", "")
                            .replace(",", "")
                            .trim()
                        val money = moneyStr.toLongOrNull() ?: 0L
                        return@withContext PlayerPrizeMoney(name, money)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "Error scraping WTA prize money for $playerName")
            null
        }
    }
}
