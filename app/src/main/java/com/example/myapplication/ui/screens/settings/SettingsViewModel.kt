package com.example.myapplication.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.CurrencyExchangeApp
import com.example.myapplication.data.repository.DecimalFormat
import com.example.myapplication.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the settings screen.
 * Currently only handles number format preferences.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val decimalFormat: StateFlow<DecimalFormat> = settingsRepository.decimalFormat
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DecimalFormat.PERIOD_COMMA
        )

    fun setDecimalFormat(format: DecimalFormat) {
        viewModelScope.launch {
            settingsRepository.setDecimalFormat(format)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val container = CurrencyExchangeApp.instance.container
                return SettingsViewModel(container.settingsRepository) as T
            }
        }
    }
}
