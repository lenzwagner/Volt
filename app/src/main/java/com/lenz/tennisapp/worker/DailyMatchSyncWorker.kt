package com.lenz.tennisapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lenz.tennisapp.data.repository.TennisRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.time.LocalDate

/**
 * Pulls all matches for today.
 * - Scheduled at 5:00 AM (initial warm-up before user opens the app)
 * - Also runs every 4 hours to catch cancellations and late additions
 */
@HiltWorker
class DailyMatchSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TennisRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "daily_match_sync"
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.d("DailyMatchSyncWorker: pulling today's matches...")
            val result = repository.refreshMatches(LocalDate.now())
            if (result is com.lenz.tennisapp.domain.model.Result.Success) {
                Timber.d("✅ DailyMatchSyncWorker: matches synced")
                Result.success()
            } else {
                Timber.w("⚠️ DailyMatchSyncWorker: sync returned error, retrying")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ DailyMatchSyncWorker failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
