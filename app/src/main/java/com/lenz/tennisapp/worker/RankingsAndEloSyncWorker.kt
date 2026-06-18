package com.lenz.tennisapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lenz.tennisapp.data.repository.TennisRepository
import com.lenz.tennisapp.data.scraper.LiveTennisScraper
import com.lenz.tennisapp.data.scraper.PlayerMatcher
import com.lenz.tennisapp.data.scraper.TennisAbstractScraper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Worker that syncs rankings (live-tennis.eu) and Elo ratings (tennisabstract.com) daily at 3:00 AM.
 */
@HiltWorker
class RankingsAndEloSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TennisRepository,
    private val tennisAbstractScraper: TennisAbstractScraper,
    private val liveTennisScraper: LiveTennisScraper,
    private val playerMatcher: PlayerMatcher
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "rankings_elo_sync"
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting Rankings + Elo sync...")
            repository.clearOldRankingsAndElo()

            val eloOk = tennisAbstractScraper.scrapeAndSave()
            val rankingsOk = liveTennisScraper.scrapeAndSave()

            // Match players across all three sources
            Timber.d("Matching players across data sources...")
            playerMatcher.matchAll()

            if (eloOk || rankingsOk) {
                Timber.d("✅ Rankings + Elo sync completed (elo=$eloOk, rankings=$rankingsOk)")
                Result.success()
            } else {
                Timber.w("⚠️ No data synced")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Rankings + Elo sync failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
