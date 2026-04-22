package com.example.myapplication.ui.screens.editlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.CurrencyExchangeApp
import com.example.myapplication.data.repository.ExchangeRateRepository
import com.example.myapplication.domain.model.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the edit list screen.
 */
data class EditListUiState(
    val currencies: List<Currency> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

/**
 * ViewModel for managing the currency list editor.
 *
 * Handles:
 * - Searching/filtering currencies
 * - Toggling visibility (show/hide)
 * - Reordering visible currencies via drag-and-drop
 */
class EditListViewModel(
    private val repository: ExchangeRateRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<EditListUiState> = combine(
        repository.getAllCurrencies(),
        _searchQuery
    ) { currencies, query ->
        // Filter by search query
        val filteredCurrencies = if (query.isBlank()) {
            currencies
        } else {
            currencies.filter { currency ->
                currency.code.contains(query, ignoreCase = true) ||
                        currency.name.contains(query, ignoreCase = true)
            }
        }

        // Sort: visible currencies first (by position), then hidden (alphabetically)
        EditListUiState(
            currencies = filteredCurrencies.sortedWith(
                compareByDescending<Currency> { it.isVisible }
                    .thenBy { if (it.isVisible) it.position else Int.MAX_VALUE }
                    .thenBy { it.code }
            ),
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EditListUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleCurrencyVisibility(currency: Currency) {
        viewModelScope.launch {
            repository.updateCurrencyVisibility(currency.code, !currency.isVisible)
        }
    }

    /**
     * Moves a currency from one position to another in the visible list.
     */
    fun moveCurrency(from: Int, to: Int) {
        viewModelScope.launch {
            val currentList = uiState.value.currencies.filter { it.isVisible }.toMutableList()
            if (from in currentList.indices && to in currentList.indices) {
                val item = currentList.removeAt(from)
                currentList.add(to, item)
                repository.updateCurrencyPositions(currentList)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val container = CurrencyExchangeApp.instance.container
                return EditListViewModel(container.exchangeRateRepository) as T
            }
        }
    }
}
