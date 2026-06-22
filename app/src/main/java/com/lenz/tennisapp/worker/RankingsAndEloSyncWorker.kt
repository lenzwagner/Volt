package com.lenz.tennisapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lenz.tennisapp.data.repository.PlayerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Worker that synchronizes ATP/WTA Rankings and Elo Scores.
 */
@HiltWorker
class RankingsAndEloSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playerRepository: PlayerRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "rankings_elo_sync"
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.d("RankingsAndEloSyncWorker: starting sync")

            playerRepository.syncLiveRankings()
            playerRepository.syncEloRatings()
            playerRepository.syncPrizeMoney()

            Timber.d("RankingsAndEloSyncWorker: sync completed")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "RankingsAndEloSyncWorker: sync failed")
            Result.retry()
        }
    }
}
