package com.example.myapplication.di

import android.content.Context
import androidx.room.Room
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.api.ExchangeRateApi
import com.example.myapplication.data.db.ExchangeRateDatabase
import com.example.myapplication.data.repository.ExchangeRateRepository
import com.example.myapplication.data.repository.SettingsRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Manual dependency injection container.
 *
 * Creates and provides all dependencies needed throughout the app:
 * - Network client (Retrofit + OkHttp)
 * - Database (Room)
 * - Repositories
 *
 * Supports two modes for fetching exchange rates:
 * - Worker mode: Uses custom Cloudflare Worker URL (set EXCHANGE_RATE_WORKER_URL)
 * - Direct API mode: Uses OpenExchangeRates directly (set OPENEXCHANGERATES_API_KEY)
 */
class AppContainer(context: Context) {

    // Determine which mode to use based on configuration
    private val useWorkerMode = BuildConfig.EXCHANGE_RATE_WORKER_URL.isNotBlank()
    private val baseUrl = if (useWorkerMode) {
        BuildConfig.EXCHANGE_RATE_WORKER_URL
    } else {
        "https://openexchangerates.org/api/"
    }

    // HTTP client with logging for debugging API calls (debug builds only)
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }
        .build()

    // Retrofit client for exchange rates
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val exchangeRateApi: ExchangeRateApi = retrofit.create(ExchangeRateApi::class.java)

    // Room database for offline storage
    private val database: ExchangeRateDatabase = Room.databaseBuilder(
        context.applicationContext,
        ExchangeRateDatabase::class.java,
        "exchange_rate_db"
    ).fallbackToDestructiveMigration().build()

    // Public repositories for use by ViewModels
    val exchangeRateRepository: ExchangeRateRepository = ExchangeRateRepository(
        api = exchangeRateApi,
        apiKey = BuildConfig.OPENEXCHANGERATES_API_KEY,
        useWorkerMode = useWorkerMode,
        exchangeRateDao = database.exchangeRateDao(),
        userCurrencyDao = database.userCurrencyDao()
    )

    val settingsRepository: SettingsRepository = SettingsRepository(context.applicationContext)
}
