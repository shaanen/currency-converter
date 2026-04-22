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

/**
 * Number format options for displaying currency values.
 *
 * @property decimalSeparator Character separating whole and decimal parts
 * @property thousandsSeparator Character separating thousands
 * @property displayName Example showing the format (e.g., "1,234.56")
 */
enum class DecimalFormat(
    val decimalSeparator: Char,
    val thousandsSeparator: Char,
    val displayName: String
) {
    PERIOD_COMMA('.', ',', "1,234.56"),      // US/UK style
    COMMA_PERIOD(',', '.', "1.234,56"),      // European style
    PERIOD_SPACE('.', ' ', "1 234.56"),      // SI style with period
    COMMA_SPACE(',', ' ', "1 234,56")        // SI style with comma
}

/**
 * Stores the last conversion for restoring on app restart.
 */
data class LastConversion(
    val currencyCode: String,
    val amount: Double
)

/**
 * Repository for user settings stored in DataStore.
 *
 * Settings include:
 * - Number format (decimal/thousands separators)
 * - Last conversion (currency and amount) for restoring state
 */
class SettingsRepository(private val context: Context) {

    private val decimalFormatKey = stringPreferencesKey("decimal_format")
    private val lastCurrencyKey = stringPreferencesKey("last_currency")
    private val lastAmountKey = doublePreferencesKey("last_amount")

    val decimalFormat: Flow<DecimalFormat> = context.dataStore.data.map { prefs ->
        prefs[decimalFormatKey]?.let { DecimalFormat.valueOf(it) } ?: DecimalFormat.PERIOD_COMMA
    }

    val lastConversion: Flow<LastConversion?> = context.dataStore.data.map { prefs ->
        val currency = prefs[lastCurrencyKey]
        val amount = prefs[lastAmountKey]
        if (currency != null && amount != null) {
            LastConversion(currency, amount)
        } else {
            null
        }
    }

    suspend fun setDecimalFormat(format: DecimalFormat) {
        context.dataStore.edit { it[decimalFormatKey] = format.name }
    }

    suspend fun saveLastConversion(currencyCode: String, amount: Double) {
        context.dataStore.edit { prefs ->
            prefs[lastCurrencyKey] = currencyCode
            prefs[lastAmountKey] = amount
        }
    }
}
