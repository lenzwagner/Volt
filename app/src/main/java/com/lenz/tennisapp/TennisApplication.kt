package com.lenz.tennisapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.lenz.tennisapp.notification.NotificationHelper
import com.lenz.tennisapp.ui.theme.CourtType
import com.lenz.tennisapp.worker.*
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.Calendar
import javax.inject.Inject

@HiltAndroidApp
class TennisApplication : Application(), Configuration.Provider {

    companion object {
        /** Global court background used across all screens for consistency. */
        private val _sessionCourt = androidx.compose.runtime.mutableStateOf(CourtType.entries.random())
        val sessionCourt: CourtType get() = _sessionCourt.value

        fun rotateCourt() {
            val entries = CourtType.entries
            val currentIndex = entries.indexOf(_sessionCourt.value)
            val nextIndex = (currentIndex + 1) % entries.size
            _sessionCourt.value = entries[nextIndex]
        }
    }

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Randomized in initializer

        // Setup Timber for crash logging
        Timber.plant(Timber.DebugTree())
        setupCrashHandler()

        NotificationHelper.createChannels(this)
        scheduleApiKeyCheck()
        scheduleEloSync()
        scheduleRankingsAndEloSync()
        scheduleDailyMatchSync()
        scheduleDatabaseCleanup()
        scheduleMatchNotifications()
    }

    private fun setupCrashHandler() {
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "App crashed on thread: ${thread.name}")
            // Call original handler to show system crash dialog
            defaultExceptionHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun scheduleEloSync() {
        val request = OneTimeWorkRequestBuilder<EloSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            EloSyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleApiKeyCheck() {
        val request = PeriodicWorkRequestBuilder<ApiKeyCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ApiKeyCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleRankingsAndEloSync() {
        // Sync Rankings and Elo on app startup (immediate)
        val immediateRequest = OneTimeWorkRequestBuilder<RankingsAndEloSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "${RankingsAndEloSyncWorker.WORK_NAME}_startup",
            ExistingWorkPolicy.KEEP,
            immediateRequest
        )

        Timber.d("Rankings and Elo sync scheduled for app startup")

        // Also schedule daily sync at 3:00 AM
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // Wenn 3:00 Uhr bereits vorbei ist, planen für morgen
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val dailyRequest = PeriodicWorkRequestBuilder<RankingsAndEloSyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RankingsAndEloSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )

        Timber.d("Rankings and Elo sync also scheduled to run daily at 3:00 AM")
    }

    private fun scheduleMatchNotifications() {
        val request = PeriodicWorkRequestBuilder<MatchNotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            MatchNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleDailyMatchSync() {
        // Initial run at 5:00 AM (warm DB before user opens app)
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_MONTH, 1)
        val initialDelay = target.timeInMillis - now.timeInMillis

        // Repeat every 4 hours to catch cancellations and schedule changes
        val request = PeriodicWorkRequestBuilder<DailyMatchSyncWorker>(4, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailyMatchSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Timber.d("DailyMatchSyncWorker scheduled at 5:00 AM, then every 4h")
    }

    private fun scheduleDatabaseCleanup() {
        // Berechne Verzögerung bis 4:00 Uhr (nach Rankings-Sync)
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // Wenn 4:00 Uhr bereits vorbei ist, planen für morgen
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<DatabaseCleanupWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DatabaseCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )

        Timber.d("Database cleanup scheduled to run daily at 4:00 AM (90-day rotation)")
    }
}
