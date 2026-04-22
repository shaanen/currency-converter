package com.example.myapplication.data.api

import com.example.myapplication.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API response from OpenExchangeRates.org
 */
data class ExchangeRateResponse(
    val disclaimer: String,
    val license: String,
    val timestamp: Long,      // Unix timestamp when rates were published
    val base: String,         // Base currency (always USD for free tier)
    val rates: Map<String, Double>  // Currency code -> rate relative to USD
)

/**
 * Retrofit interface for OpenExchangeRates API.
 * API docs: https://docs.openexchangerates.org/
 */
interface ExchangeRateApi {
    @GET("latest.json")
    suspend fun getLatestRates(
        @Query("app_id") appId: String = BuildConfig.OPENEXCHANGERATES_API_KEY
    ): ExchangeRateResponse
}
