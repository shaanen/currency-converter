package com.example.myapplication.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.myapplication.CurrencyExchangeApp
import java.util.concurrent.TimeUnit

/**
 * Background worker that syncs exchange rates every hour.
 *
 * Uses WorkManager for reliable scheduling even when app is closed.
 * Only runs when network is available.
 */
class SyncRatesWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as CurrencyExchangeApp).container.exchangeRateRepository
        return if (repository.refreshRates().isSuccess) {
            Result.success()
        } else {
            Result.retry()  // Will retry with backoff
        }
    }

    companion object {
        private const val WORK_NAME = "sync_exchange_rates"

        /**
         * Schedules hourly rate sync. Safe to call multiple times -
         * existing work will be kept (not replaced).
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Calculate initial delay to align with :06 past the hour
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
            val currentMinute = calendar.get(java.util.Calendar.MINUTE)
            val currentSecond = calendar.get(java.util.Calendar.SECOND)

            // Minutes until next :06
            val minutesUntilTarget = if (currentMinute < 6) {
                6 - currentMinute
            } else {
                66 - currentMinute  // Next hour's :06
            }
            val initialDelayMillis = (minutesUntilTarget * 60 - currentSecond) * 1000L

            val workRequest = PeriodicWorkRequestBuilder<SyncRatesWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
