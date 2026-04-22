package com.example.myapplication.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class DecimalFormat(val decimalSeparator: Char, val thousandsSeparator: Char, val displayName: String) {
    PERIOD_COMMA('.', ',', "1,234.56"),
    COMMA_PERIOD(',', '.', "1.234,56"),
    PERIOD_SPACE('.', ' ', "1 234.56"),
    COMMA_SPACE(',', ' ', "1 234,56")
}

data class LastConversion(
    val currencyCode: String,
    val amount: Double
)

class SettingsRepository(private val context: Context) {
    private val decimalFormatKey = stringPreferencesKey("decimal_format")
    private val lastCurrencyKey = stringPreferencesKey("last_currency")
    private val lastAmountKey = doublePreferencesKey("last_amount")

    val decimalFormat: Flow<DecimalFormat> = context.dataStore.data.map { preferences ->
        preferences[decimalFormatKey]?.let { DecimalFormat.valueOf(it) } ?: DecimalFormat.PERIOD_COMMA
    }

    val lastConversion: Flow<LastConversion?> = context.dataStore.data.map { preferences ->
        val currency = preferences[lastCurrencyKey]
        val amount = preferences[lastAmountKey]
        if (currency != null && amount != null) {
            LastConversion(currency, amount)
        } else {
            null
        }
    }

    suspend fun setDecimalFormat(format: DecimalFormat) {
        context.dataStore.edit { preferences ->
            preferences[decimalFormatKey] = format.name
        }
    }

    suspend fun saveLastConversion(currencyCode: String, amount: Double) {
        context.dataStore.edit { preferences ->
            preferences[lastCurrencyKey] = currencyCode
            preferences[lastAmountKey] = amount
        }
    }
}
