package com.handhot.app.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.handhot.app.data.repository.FeedRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: FeedRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val retentionDays = applicationContext.settingsDataStore.data
            .map { it[intPreferencesKey("retention_days")] ?: 7 }
            .let { flow ->
                var days = 7
                flow.collect { days = it; return@collect }
                days
            }
        val deleted = repository.cleanOldData(retentionDays)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CleanupWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "cleanup_old_data",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
