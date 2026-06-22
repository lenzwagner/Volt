package com.lenz.tennisapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lenz.tennisapp.data.repository.TennisRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class OddsSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TennisRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "odds_sync"
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.d("OddsSyncWorker: syncing odds for all open matches")
            repository.syncOddsForAllMatches()
            Timber.d("OddsSyncWorker: done")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "OddsSyncWorker: failed")
            Result.retry()
        }
    }
}
