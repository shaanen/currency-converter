package com.example.myapplication.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API response for exchange rates.
 * Works with both direct OpenExchangeRates API and custom Worker.
 */
data class ExchangeRateResponse(
    val timestamp: Long,               // Unix timestamp when rates were published (seconds)
    val base: String,                  // Base currency (USD)
    val rates: Map<String, Double>,    // Currency code -> rate relative to USD
    val fetched_at: Long? = null       // Optional: when Worker fetched rates (millis), null for direct API
)

/**
 * Retrofit interface for fetching exchange rates.
 * Supports two modes:
 * - Worker mode: GET / (no params needed)
 * - Direct API mode: GET /latest.json?app_id=KEY
 */
interface ExchangeRateApi {
    @GET("/")
    suspend fun getLatestRatesFromWorker(): ExchangeRateResponse

    @GET("latest.json")
    suspend fun getLatestRatesFromApi(
        @Query("app_id") appId: String
    ): ExchangeRateResponse
}
