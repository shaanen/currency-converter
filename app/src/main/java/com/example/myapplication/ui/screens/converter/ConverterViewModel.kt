package com.example.myapplication.ui.screens.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.CurrencyExchangeApp
import com.example.myapplication.data.repository.DecimalFormat
import com.example.myapplication.data.repository.ExchangeRateRepository
import com.example.myapplication.data.repository.SettingsRepository
import com.example.myapplication.domain.model.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Currency paired with its calculated converted value.
 */
data class CurrencyWithValue(
    val currency: Currency,
    val value: Double
)

/**
 * UI state for the converter screen.
 */
data class ConverterUiState(
    val currencies: List<CurrencyWithValue> = emptyList(),
    val lastUpdated: Long? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val decimalFormat: DecimalFormat = DecimalFormat.PERIOD_COMMA
)

/**
 * ViewModel for the currency converter screen.
 *
 * Handles:
 * - Loading and refreshing exchange rates
 * - Converting values between currencies
 * - Persisting last conversion for app restart
 */
class ConverterViewModel(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _lastUpdatedTimestamp = MutableStateFlow<Long?>(null)

    // Calculated values for each currency code
    private val _currencyValues = MutableStateFlow<Map<String, Double>>(emptyMap())

    // The currency and amount the user last edited (for persistence and conversion base)
    private var lastEditedCurrencyCode: String? = null
    private var lastEditedAmount: Double = 1.0

    // Combines all data sources into a single UI state
    val uiState: StateFlow<ConverterUiState> = combine(
        exchangeRateRepository.getVisibleCurrencies(),
        _currencyValues,
        _isRefreshing,
        _error,
        settingsRepository.decimalFormat,
        _lastUpdatedTimestamp
    ) { values ->
        val currencies = values[0] as List<Currency>
        val currencyValues = values[1] as Map<String, Double>
        val isRefreshing = values[2] as Boolean
        val error = values[3] as String?
        val decimalFormat = values[4] as DecimalFormat
        val lastUpdated = values[5] as Long?

        ConverterUiState(
            currencies = currencies.map { currency ->
                CurrencyWithValue(currency, currencyValues[currency.code] ?: 0.0)
            },
            lastUpdated = lastUpdated,
            isLoading = currencies.isEmpty() && lastUpdated == null,
            isRefreshing = isRefreshing,
            error = error,
            decimalFormat = decimalFormat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConverterUiState()
    )

    // Tracks which currency row is currently being edited
    private val _editingCurrencyCode = MutableStateFlow<String?>(null)
    val editingCurrencyCode: StateFlow<String?> = _editingCurrencyCode.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            // Load saved conversion from last session
            val savedConversion = settingsRepository.lastConversion.first()
            if (savedConversion != null) {
                lastEditedCurrencyCode = savedConversion.currencyCode
                lastEditedAmount = savedConversion.amount
            }

            // Fetch rates if this is a fresh install
            val timestamp = exchangeRateRepository.getLastUpdateTimestamp()
            _lastUpdatedTimestamp.value = timestamp

            if (timestamp == null) {
                _isRefreshing.value = true
                val result = exchangeRateRepository.refreshRates()
                if (result.isFailure) {
                    _error.value = "Failed to refresh rates. Please check your connection."
                }
                _lastUpdatedTimestamp.value = exchangeRateRepository.getLastUpdateTimestamp()
                _isRefreshing.value = false
            }

            // Calculate initial values once currencies are available
            val currencies = exchangeRateRepository.getVisibleCurrencies().first()
            if (currencies.isNotEmpty()) {
                val baseCurrency = lastEditedCurrencyCode ?: currencies.sortedBy { it.position }.first().code
                recalculateAllValues(currencies, baseCurrency, lastEditedAmount)
            }

            // Keep values updated when currency list changes
            observeCurrencyChanges()
        }
    }

    /**
     * Watches for changes to visible currencies and updates values accordingly.
     */
    private fun observeCurrencyChanges() {
        viewModelScope.launch {
            exchangeRateRepository.getVisibleCurrencies().collect { currencies ->
                if (currencies.isNotEmpty()) {
                    val sortedCurrencies = currencies.sortedBy { it.position }

                    if (_currencyValues.value.isEmpty()) {
                        // First time currencies become visible, calculate all values
                        val baseCurrency = lastEditedCurrencyCode ?: sortedCurrencies.first().code
                        recalculateAllValues(currencies, baseCurrency, lastEditedAmount)
                    } else {
                        // Calculate values for any newly visible currencies
                        val currentValues = _currencyValues.value.toMutableMap()
                        sortedCurrencies.forEach { currency ->
                            if (!currentValues.containsKey(currency.code)) {
                                val baseCode = lastEditedCurrencyCode ?: sortedCurrencies.first().code
                                val baseCurrency = sortedCurrencies.find { it.code == baseCode }
                                if (baseCurrency != null) {
                                    currentValues[currency.code] = lastEditedAmount * (currency.rateToUsd / baseCurrency.rateToUsd)
                                }
                            }
                        }
                        _currencyValues.value = currentValues
                    }
                }
            }
        }
    }

    /**
     * Fetches fresh exchange rates from the API.
     */
    fun refreshRates() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            val result = exchangeRateRepository.refreshRates()
            if (result.isFailure) {
                _error.value = "Failed to refresh rates. Please check your connection."
            } else {
                _lastUpdatedTimestamp.value = exchangeRateRepository.getLastUpdateTimestamp()
                // Recalculate with new rates if there are visible currencies
                val currencies = exchangeRateRepository.getVisibleCurrencies().first()
                if (currencies.isNotEmpty()) {
                    val baseCurrency = lastEditedCurrencyCode ?: currencies.sortedBy { it.position }.first().code
                    recalculateAllValues(currencies, baseCurrency, lastEditedAmount)
                }
            }
            _isRefreshing.value = false
        }
    }

    /**
     * Recalculates all currency values based on a base currency and amount.
     */
    private fun recalculateAllValues(currencies: List<Currency>, baseCurrencyCode: String, baseAmount: Double) {
        val actualBaseCurrency = currencies.find { it.code == baseCurrencyCode } ?: currencies.firstOrNull() ?: return
        val baseRateToUsd = actualBaseCurrency.rateToUsd

        val newValues = currencies.associate { currency ->
            val value = if (currency.code == actualBaseCurrency.code) {
                baseAmount
            } else {
                baseAmount * (currency.rateToUsd / baseRateToUsd)
            }
            currency.code to value
        }
        _currencyValues.value = newValues
    }

    /**
     * Called when user edits a currency value.
     * Recalculates all other values based on the new input.
     */
    fun onValueChanged(currencyCode: String, newValue: Double) {
        val currentValue = _currencyValues.value[currencyCode] ?: 0.0

        // Avoid recalculating for tiny floating point differences
        val currentRounded = kotlin.math.round(currentValue * 10000) / 10000
        val newRounded = kotlin.math.round(newValue * 10000) / 10000
        if (currentRounded == newRounded) return

        lastEditedCurrencyCode = currencyCode
        lastEditedAmount = newValue

        viewModelScope.launch {
            val currencies = exchangeRateRepository.getVisibleCurrencies().first()
            recalculateAllValues(currencies, currencyCode, newValue)
            settingsRepository.saveLastConversion(currencyCode, newValue)
        }
    }

    fun setEditingCurrency(code: String?) {
        _editingCurrencyCode.value = code
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val container = CurrencyExchangeApp.instance.container
                return ConverterViewModel(
                    container.exchangeRateRepository,
                    container.settingsRepository
                ) as T
            }
        }
    }
}
