package com.lenz.tennisapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lenz.tennisapp.data.repository.TennisRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Worker that cleans up old matches (older than 90 days) daily
 * Helps manage database size and improves app performance
 */
@HiltWorker
class DatabaseCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TennisRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "database_cleanup"
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting database cleanup...")

            when (val result = repository.cleanupOldMatches()) {
                is com.lenz.tennisapp.domain.model.Result.Success<*> -> {
                    Timber.d("✅ Database cleanup completed successfully")
                    Result.success()
                }
                is com.lenz.tennisapp.domain.model.Result.Error -> {
                    Timber.e("⚠️ Database cleanup failed: ${result.message}")
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
                else -> {
                    Timber.e("❌ Database cleanup unknown error")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Exception during database cleanup")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
