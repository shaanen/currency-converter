package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for storing exchange rates.
 *
 * @property currencyCode ISO 4217 currency code (e.g., "EUR", "USD")
 * @property rateToUsd Exchange rate relative to USD (e.g., EUR = 0.92 means 1 USD = 0.92 EUR)
 * @property timestamp When the rate was published by OpenExchangeRates (Unix millis)
 * @property fetchedAt When we fetched this rate from the API (Unix millis)
 */
@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey
    val currencyCode: String,
    val rateToUsd: Double,
    val timestamp: Long,
    val fetchedAt: Long = System.currentTimeMillis()
)
