package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for exchange rates.
 */
@Dao
interface ExchangeRateDao {

    @Query("SELECT * FROM exchange_rates")
    fun getAllRates(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates WHERE currencyCode = :code")
    suspend fun getRate(code: String): ExchangeRateEntity?

    @Query("SELECT MAX(fetchedAt) FROM exchange_rates")
    suspend fun getLastFetchedTimestamp(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<ExchangeRateEntity>)
}
