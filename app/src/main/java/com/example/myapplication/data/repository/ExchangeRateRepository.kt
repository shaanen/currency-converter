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

class ExchangeRateRepository(
    private val api: ExchangeRateApi,
    private val exchangeRateDao: ExchangeRateDao,
    private val userCurrencyDao: UserCurrencyDao
) {
    suspend fun refreshRates(): Result<Unit> {
        return try {
            val response = api.getLatestRates()
            val fetchedAt = System.currentTimeMillis()
            val entities = response.rates.map { (code, rate) ->
                ExchangeRateEntity(
                    currencyCode = code,
                    rateToUsd = rate,
                    timestamp = response.timestamp * 1000,
                    fetchedAt = fetchedAt
                )
            }
            exchangeRateDao.insertAll(entities)

            if (userCurrencyDao.getCount() == 0) {
                initializeDefaultCurrencies(response.rates.keys.toList())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun initializeDefaultCurrencies(availableCodes: List<String>) {
        val defaultCodes = CurrencyInfo.defaultCurrencies.filter { it in availableCodes }
        val otherCodes = availableCodes.filter { it !in defaultCodes }.sorted()

        val entities = defaultCodes.mapIndexed { index, code ->
            UserCurrencyEntity(code, index, true)
        } + otherCodes.mapIndexed { index, code ->
            UserCurrencyEntity(code, defaultCodes.size + index, false)
        }

        userCurrencyDao.insertAll(entities)
    }

    fun getVisibleCurrencies(): Flow<List<Currency>> {
        return combine(
            exchangeRateDao.getAllRates(),
            userCurrencyDao.getVisibleCurrencies()
        ) { rates, userCurrencies ->
            val rateMap = rates.associateBy { it.currencyCode }
            userCurrencies.mapNotNull { userCurrency ->
                rateMap[userCurrency.currencyCode]?.let { rate ->
                    Currency(
                        code = rate.currencyCode,
                        name = CurrencyInfo.getName(rate.currencyCode),
                        flag = CurrencyInfo.getFlag(rate.currencyCode),
                        rateToUsd = rate.rateToUsd,
                        position = userCurrency.position,
                        isVisible = userCurrency.isVisible
                    )
                }
            }
        }
    }

    fun getAllCurrencies(): Flow<List<Currency>> {
        return combine(
            exchangeRateDao.getAllRates(),
            userCurrencyDao.getAllCurrencies()
        ) { rates, userCurrencies ->
            val rateMap = rates.associateBy { it.currencyCode }
            userCurrencies.mapNotNull { userCurrency ->
                rateMap[userCurrency.currencyCode]?.let { rate ->
                    Currency(
                        code = rate.currencyCode,
                        name = CurrencyInfo.getName(rate.currencyCode),
                        flag = CurrencyInfo.getFlag(rate.currencyCode),
                        rateToUsd = rate.rateToUsd,
                        position = userCurrency.position,
                        isVisible = userCurrency.isVisible
                    )
                }
            }
        }
    }

    suspend fun getLastUpdateTimestamp(): Long? {
        return exchangeRateDao.getLastFetchedTimestamp()
    }

    suspend fun updateCurrencyVisibility(code: String, isVisible: Boolean) {
        userCurrencyDao.updateVisibility(code, isVisible)
    }

    suspend fun updateCurrencyPositions(currencies: List<Currency>) {
        currencies.forEachIndexed { index, currency ->
            userCurrencyDao.updatePosition(currency.code, index)
        }
    }
}
