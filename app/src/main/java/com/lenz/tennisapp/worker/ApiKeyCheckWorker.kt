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
            val response = tennisApi.getFixtures(eventType = 1, dateStart = today, dateStop = today, apiKey = key)
            if (response.success?.toString() == "0") {
                val error = response.error?.toString() ?: ""
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
}
