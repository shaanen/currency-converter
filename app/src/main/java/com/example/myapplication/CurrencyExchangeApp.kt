package com.example.myapplication

import android.app.Application
import com.example.myapplication.di.AppContainer
import com.example.myapplication.worker.SyncRatesWorker

class CurrencyExchangeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
        SyncRatesWorker.schedule(this)
    }

    companion object {
        lateinit var instance: CurrencyExchangeApp
            private set
    }
}
