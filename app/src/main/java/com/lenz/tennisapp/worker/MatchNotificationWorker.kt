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

@HiltWorker
class MatchNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: TennisRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "match_notification_worker"
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.d("MatchNotificationWorker running...")
            repository.refreshSilent(LocalDate.now())
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error in MatchNotificationWorker")
            Result.retry()
        }
    }
}
