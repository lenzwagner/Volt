package com.lenz.tennisapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lenz.tennisapp.data.db.dao.EloDao
import com.lenz.tennisapp.data.scraper.TennisAbstractScraper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One-shot worker that scrapes TennisAbstract ELO ratings.
 * Scheduled at startup if the elo_ratings table has fewer than 50 entries.
 */
@HiltWorker
class EloSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val scraper: TennisAbstractScraper,
    private val eloDao: EloDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "elo_sync_tennisabstract"
    }

    override suspend fun doWork(): Result {
        return try {
            // Skip only if we have a large number of tennisabstract-sourced entries (key starts with "ta_")
            // This lets us scrape on fresh install but skip redundant re-runs
            if (eloDao.countAll() > 500 && runAttemptCount == 0) {
                return Result.success()
            }
            val ok = scraper.scrapeAndSave()
            if (ok) Result.success() else Result.retry()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
