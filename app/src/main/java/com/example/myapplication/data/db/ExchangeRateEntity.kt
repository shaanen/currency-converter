package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey
    val currencyCode: String,
    val rateToUsd: Double,
    val timestamp: Long,
    val fetchedAt: Long = System.currentTimeMillis()
)
