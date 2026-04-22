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

data class CurrencyWithValue(
    val currency: Currency,
    val value: Double
)

data class ConverterUiState(
    val currencies: List<CurrencyWithValue> = emptyList(),
    val lastUpdated: Long? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val decimalFormat: DecimalFormat = DecimalFormat.PERIOD_COMMA
)

class ConverterViewModel(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _lastUpdatedTimestamp = MutableStateFlow<Long?>(null)

    // Store the actual calculated values for each currency
    private val _currencyValues = MutableStateFlow<Map<String, Double>>(emptyMap())

    // Track which currency/amount was last edited (for persistence)
    // null means no saved conversion - will use first currency with value 1
    private var lastEditedCurrencyCode: String? = null
    private var lastEditedAmount: Double = 1.0
    private var hasLoadedSavedConversion = false

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

        val currenciesWithValues = currencies.map { currency ->
            CurrencyWithValue(
                currency = currency,
                value = currencyValues[currency.code] ?: 0.0
            )
        }

        ConverterUiState(
            currencies = currenciesWithValues,
            lastUpdated = lastUpdated,
            isLoading = currencies.isEmpty(),
            isRefreshing = isRefreshing,
            error = error,
            decimalFormat = decimalFormat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConverterUiState()
    )

    private val _editingCurrencyCode = MutableStateFlow<String?>(null)
    val editingCurrencyCode: StateFlow<String?> = _editingCurrencyCode.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            // First, load any saved conversion
            val savedConversion = settingsRepository.lastConversion.first()
            if (savedConversion != null) {
                lastEditedCurrencyCode = savedConversion.currencyCode
                lastEditedAmount = savedConversion.amount
            }
            hasLoadedSavedConversion = true

            // Check if we need to fetch rates
            val timestamp = exchangeRateRepository.getLastUpdateTimestamp()
            _lastUpdatedTimestamp.value = timestamp

            if (timestamp == null) {
                // Fresh install - fetch rates first
                _isRefreshing.value = true
                val result = exchangeRateRepository.refreshRates()
                if (result.isFailure) {
                    _error.value = "Failed to refresh rates. Please check your connection."
                }
                _lastUpdatedTimestamp.value = exchangeRateRepository.getLastUpdateTimestamp()
                _isRefreshing.value = false
            }

            // Now wait for currencies to be available and do initial calculation
            val currencies = exchangeRateRepository.getVisibleCurrencies().first { it.isNotEmpty() }
            val sortedCurrencies = currencies.sortedBy { it.position }

            // Use first currency (EUR) if no saved conversion
            val baseCurrency = lastEditedCurrencyCode ?: sortedCurrencies.first().code
            recalculateAllValues(sortedCurrencies, baseCurrency, lastEditedAmount)

            // Observe future currency changes
            observeCurrencyChanges()
        }
    }

    private fun observeCurrencyChanges() {
        viewModelScope.launch {
            exchangeRateRepository.getVisibleCurrencies().collect { currencies ->
                // Only handle subsequent changes, not the initial load
                if (currencies.isNotEmpty() && _currencyValues.value.isNotEmpty()) {
                    // Keep existing values, just ensure all visible currencies have values
                    val currentValues = _currencyValues.value.toMutableMap()
                    val sortedCurrencies = currencies.sortedBy { it.position }

                    // Add any new currencies that don't have values yet
                    sortedCurrencies.forEach { currency ->
                        if (!currentValues.containsKey(currency.code)) {
                            // Calculate value based on current base
                            val baseCode = lastEditedCurrencyCode ?: sortedCurrencies.first().code
                            val baseCurrency = sortedCurrencies.find { it.code == baseCode }
                            if (baseCurrency != null) {
                                val value = lastEditedAmount * (currency.rateToUsd / baseCurrency.rateToUsd)
                                currentValues[currency.code] = value
                            }
                        }
                    }
                    _currencyValues.value = currentValues
                }
            }
        }
    }

    fun refreshRates() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            val result = exchangeRateRepository.refreshRates()
            if (result.isFailure) {
                _error.value = "Failed to refresh rates. Please check your connection."
            } else {
                _lastUpdatedTimestamp.value = exchangeRateRepository.getLastUpdateTimestamp()
                // Wait for currencies to be available, then recalculate with new rates
                val currencies = exchangeRateRepository.getVisibleCurrencies().first { it.isNotEmpty() }.sortedBy { it.position }
                val baseCurrency = lastEditedCurrencyCode ?: currencies.first().code
                recalculateAllValues(currencies, baseCurrency, lastEditedAmount)
            }
            _isRefreshing.value = false
        }
    }

    private fun recalculateAllValues(currencies: List<Currency>, baseCurrencyCode: String, baseAmount: Double) {
        // If the base currency isn't in the list, use the first currency
        val actualBaseCurrency = currencies.find { it.code == baseCurrencyCode } ?: currencies.firstOrNull()
        val actualBaseCode = actualBaseCurrency?.code ?: return
        val baseRateToUsd = actualBaseCurrency.rateToUsd

        val newValues = currencies.associate { currency ->
            val convertedValue = if (currency.code == actualBaseCode) {
                baseAmount
            } else {
                baseAmount * (currency.rateToUsd / baseRateToUsd)
            }
            currency.code to convertedValue
        }
        _currencyValues.value = newValues
    }

    fun onValueChanged(currencyCode: String, newValue: Double) {
        // Get current value for this currency
        val currentValue = _currencyValues.value[currencyCode] ?: 0.0

        // Only recalculate if the value actually changed (compare rounded to avoid floating point issues)
        // Round to 4 decimal places for comparison
        val currentRounded = kotlin.math.round(currentValue * 10000) / 10000
        val newRounded = kotlin.math.round(newValue * 10000) / 10000
        if (currentRounded == newRounded) {
            return
        }

        // Update tracking for persistence
        lastEditedCurrencyCode = currencyCode
        lastEditedAmount = newValue

        // Recalculate all other values based on this new value
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
