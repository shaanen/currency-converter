package com.example.myapplication.di

import android.content.Context
import androidx.room.Room
import com.example.myapplication.data.api.ExchangeRateApi
import com.example.myapplication.data.db.ExchangeRateDatabase
import com.example.myapplication.data.repository.ExchangeRateRepository
import com.example.myapplication.data.repository.SettingsRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppContainer(context: Context) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://openexchangerates.org/api/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val exchangeRateApi: ExchangeRateApi = retrofit.create(ExchangeRateApi::class.java)

    private val database: ExchangeRateDatabase = Room.databaseBuilder(
        context.applicationContext,
        ExchangeRateDatabase::class.java,
        "exchange_rate_db"
    ).fallbackToDestructiveMigration().build()

    val exchangeRateRepository: ExchangeRateRepository = ExchangeRateRepository(
        api = exchangeRateApi,
        exchangeRateDao = database.exchangeRateDao(),
        userCurrencyDao = database.userCurrencyDao()
    )

    val settingsRepository: SettingsRepository = SettingsRepository(context.applicationContext)
}
