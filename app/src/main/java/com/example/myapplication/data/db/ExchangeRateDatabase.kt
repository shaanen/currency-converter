package com.example.myapplication.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ExchangeRateEntity::class, UserCurrencyEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ExchangeRateDatabase : RoomDatabase() {
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun userCurrencyDao(): UserCurrencyDao
}
