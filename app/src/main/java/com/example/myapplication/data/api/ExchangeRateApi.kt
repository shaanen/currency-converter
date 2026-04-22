package com.example.myapplication.data.api

import com.example.myapplication.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

data class ExchangeRateResponse(
    val disclaimer: String,
    val license: String,
    val timestamp: Long,
    val base: String,
    val rates: Map<String, Double>
)

interface ExchangeRateApi {
    @GET("latest.json")
    suspend fun getLatestRates(
        @Query("app_id") appId: String = BuildConfig.OPENEXCHANGERATES_API_KEY
    ): ExchangeRateResponse
}
