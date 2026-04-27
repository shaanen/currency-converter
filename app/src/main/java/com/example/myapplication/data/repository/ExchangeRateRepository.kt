package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ExchangeRateApi
import com.example.myapplication.data.db.ExchangeRateDao
import com.example.myapplication.data.db.ExchangeRateEntity
import com.example.myapplication.data.db.UserCurrencyDao
import com.example.myapplication.data.db.UserCurrencyEntity
import com.example.myapplication.domain.model.Currency
import com.example.myapplication.domain.model.CurrencyInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Repository for exchange rates and user currency preferences.
 *
 * Handles:
 * - Fetching rates from API (Worker or direct OpenExchangeRates) and caching in database
 * - Providing currencies with rates as observable Flows
 * - Managing user's visible/hidden currencies and their order
 */
class ExchangeRateRepository(
    private val api: ExchangeRateApi,
    private val apiKey: String,
    private val useWorkerMode: Boolean,
    private val exchangeRateDao: ExchangeRateDao,
    private val userCurrencyDao: UserCurrencyDao
) {
    /**
     * Fetches latest rates from API and stores in database.
     * On first run, also initializes default visible currencies.
     *
     * If Worker mode is enabled but fails, falls back to direct API if key is available.
     */
    suspend fun refreshRates(): Result<Unit> {
        return try {
            val response = if (useWorkerMode) {
                try {
                    api.getLatestRatesFromWorker()
                } catch (e: Exception) {
                    if (apiKey.isNotBlank()) {
                        api.getLatestRatesFromApi(apiKey)
                    } else {
                        throw e
                    }
                }
            } else {
                api.getLatestRatesFromApi(apiKey)
            }

            // Convert API response to database entities
            // fetched_at is only available in Worker mode, use current time for direct API
            val fetchedAt = response.fetched_at ?: System.currentTimeMillis()
            val entities = response.rates.map { (code, rate) ->
                ExchangeRateEntity(
                    currencyCode = code,
                    rateToUsd = rate,
                    timestamp = response.timestamp * 1000,  // Convert to millis
                    fetchedAt = fetchedAt
                )
            }
            exchangeRateDao.insertAll(entities)

            // Initialize default currencies on first run
            if (userCurrencyDao.getCount() == 0) {
                initializeDefaultCurrencies(response.rates.keys.toList())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sets up default visible currencies (EUR, GBP, USD, VND, INR, TRY)
     * and hides all other currencies.
     */
    private suspend fun initializeDefaultCurrencies(availableCodes: List<String>) {
        val defaultCodes = CurrencyInfo.defaultCurrencies.filter { it in availableCodes }
        val otherCodes = availableCodes.filter { it !in defaultCodes }.sorted()

        val entities = defaultCodes.mapIndexed { index, code ->
            UserCurrencyEntity(code, index, isVisible = true)
        } + otherCodes.mapIndexed { index, code ->
            UserCurrencyEntity(code, defaultCodes.size + index, isVisible = false)
        }

        userCurrencyDao.insertAll(entities)
    }

    /**
     * Returns visible currencies with their exchange rates, sorted by position.
     */
    fun getVisibleCurrencies(): Flow<List<Currency>> {
        return combine(
            exchangeRateDao.getAllRates(),
            userCurrencyDao.getVisibleCurrencies()
        ) { rates, userCurrencies ->
            val rateMap = rates.associateBy { it.currencyCode }
            userCurrencies.mapNotNull { userCurrency ->
                rateMap[userCurrency.currencyCode]?.toCurrency(userCurrency)
            }
        }
    }

    /**
     * Returns all currencies (visible and hidden) with their exchange rates.
     */
    fun getAllCurrencies(): Flow<List<Currency>> {
        return combine(
            exchangeRateDao.getAllRates(),
            userCurrencyDao.getAllCurrencies()
        ) { rates, userCurrencies ->
            val rateMap = rates.associateBy { it.currencyCode }
            userCurrencies.mapNotNull { userCurrency ->
                rateMap[userCurrency.currencyCode]?.toCurrency(userCurrency)
            }
        }
    }

    suspend fun getLastUpdateTimestamp(): Long? = exchangeRateDao.getLastFetchedTimestamp()

    suspend fun updateCurrencyVisibility(code: String, isVisible: Boolean) {
        userCurrencyDao.updateVisibility(code, isVisible)
    }

    suspend fun updateCurrencyPositions(currencies: List<Currency>) {
        currencies.forEachIndexed { index, currency ->
            userCurrencyDao.updatePosition(currency.code, index)
        }
    }

    /**
     * Converts database entity to domain model.
     */
    private fun ExchangeRateEntity.toCurrency(userCurrency: UserCurrencyEntity) = Currency(
        code = currencyCode,
        name = CurrencyInfo.getName(currencyCode),
        flag = CurrencyInfo.getFlag(currencyCode),
        rateToUsd = rateToUsd,
        position = userCurrency.position,
        isVisible = userCurrency.isVisible
    )
}
