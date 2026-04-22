package com.example.myapplication

import android.app.Application
import com.example.myapplication.di.AppContainer
import com.example.myapplication.worker.SyncRatesWorker

/**
 * Main Application class that initializes app-wide dependencies.
 *
 * Responsibilities:
 * - Creates the dependency injection container
 * - Schedules hourly background sync for exchange rates
 */
class CurrencyExchangeApp : Application() {

    // Dependency injection container - initialized in onCreate()
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)

        // Schedule hourly background sync for exchange rates
        SyncRatesWorker.schedule(this)
    }

    companion object {
        // Global app instance for accessing container from ViewModels
        lateinit var instance: CurrencyExchangeApp
            private set
    }
}
