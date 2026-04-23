package com.example.myapplication.data.api

import retrofit2.http.GET

/**
 * API response from the exchange rates worker.
 */
data class ExchangeRateResponse(
    val timestamp: Long,              // Unix timestamp when rates were published
    val base: String,                 // Base currency (USD)
    val rates: Map<String, Double>,   // Currency code -> rate relative to USD
    val fetched_at: Long              // When the worker fetched the rates (millis)
)

/**
 * Retrofit interface for the Cloudflare Worker that serves exchange rates.
 */
interface ExchangeRateApi {
    @GET("/")
    suspend fun getLatestRates(): ExchangeRateResponse
}
