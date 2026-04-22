package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for user's currency preferences.
 *
 * @property currencyCode ISO 4217 currency code
 * @property position Display order in the converter list (lower = higher)
 * @property isVisible Whether to show this currency in the converter
 */
@Entity(tableName = "user_currencies")
data class UserCurrencyEntity(
    @PrimaryKey
    val currencyCode: String,
    val position: Int,
    val isVisible: Boolean = true
)
