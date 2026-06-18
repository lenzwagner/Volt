package com.lenz.tennisapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lenz.tennisapp.data.api.TennisApiService
import com.lenz.tennisapp.data.datastore.ApiKeyStore
import com.lenz.tennisapp.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltWorker
class ApiKeyCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val tennisApi: TennisApiService,
    private val keyStore: ApiKeyStore
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "api_key_health_check"
    }

    override suspend fun doWork(): Result {
        checkTennisKey()
        checkOddsQuota()
        return Result.success()
    }

    private suspend fun checkTennisKey() {
        val key = keyStore.tennisApiKey.first()
        if (key.isBlank()) {
            NotificationHelper.notifyTennisKeyExpired(applicationContext)
            return
        }

        try {
            val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val response = tennisApi.getFixtures(dateStart = today, dateStop = today, apiKey = key)
            if (response.success == 0) {
                val error = response.error ?: ""
                val isKeyError = error.contains("key", ignoreCase = true) ||
                        error.contains("limit", ignoreCase = true) ||
                        error.contains("expired", ignoreCase = true)
                if (isKeyError) {
                    keyStore.setTennisKeyExpired(true)
                    NotificationHelper.notifyTennisKeyExpired(applicationContext)
                }
            } else {
                keyStore.setTennisKeyExpired(false)
            }
        } catch (_: Exception) {}
    }

    private suspend fun checkOddsQuota() {
        val remaining = keyStore.oddsRequestsRemaining.first()
        val expired = keyStore.oddsKeyExpired.first()

        when {
            expired -> NotificationHelper.notifyOddsKeyExpired(applicationContext)
            remaining in 1..50 -> NotificationHelper.notifyOddsLow(applicationContext, remaining)
        }
    }
}
