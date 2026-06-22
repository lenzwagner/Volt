package com.lenz.tennisapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.lenz.tennisapp.data.datastore.ApiKeyStore
import com.lenz.tennisapp.notification.NotificationHelper
import com.lenz.tennisapp.ui.theme.CourtType
import com.lenz.tennisapp.worker.*
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.Calendar
import javax.inject.Inject

@HiltAndroidApp
class TennisApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    // Player avatars repeat across lists; a generous memory + disk cache keeps
    // scrolling smooth and avoids re-decoding/re-downloading the same images.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(false)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(80L * 1024 * 1024)
                    .build()
            }
            .build()

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
    @Inject lateinit var apiKeyStore: ApiKeyStore

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Randomized in initializer

        // Logging only in debug — DebugTree string-building adds overhead on hot
        // paths (list grouping runs on every poll) and isn't needed in release.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        setupCrashHandler()

        NotificationHelper.createChannels(this)
        scheduleApiKeyCheck()
        scheduleEloSync()
        scheduleRankingsAndEloSync()
        scheduleDatabaseCleanup()
        scheduleMatchNotifications()
        // Cancel any previously scheduled bulk odds sync — odds are fetched on-demand only
        WorkManager.getInstance(this).cancelUniqueWork(com.lenz.tennisapp.worker.OddsSyncWorker.WORK_NAME)

        // Always reset expired flag on startup — the interceptor re-sets it if the key is truly dead
        CoroutineScope(Dispatchers.IO).launch {
            apiKeyStore.setOddsKeyExpired(false)
        }
        // Odds are fetched on-demand when user opens a match (max 1 call/match/day)
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

    private fun scheduleOddsSync() {
        // Run at 7:00 AM every day
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_MONTH, 1)
        val initialDelay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<OddsSyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            OddsSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Timber.d("Odds sync scheduled daily at 7:00 AM")
    }
}
