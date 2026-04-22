package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCurrencyDao {
    @Query("SELECT * FROM user_currencies WHERE isVisible = 1 ORDER BY position ASC")
    fun getVisibleCurrencies(): Flow<List<UserCurrencyEntity>>

    @Query("SELECT * FROM user_currencies ORDER BY position ASC")
    fun getAllCurrencies(): Flow<List<UserCurrencyEntity>>

    @Query("SELECT * FROM user_currencies WHERE currencyCode = :code")
    suspend fun getCurrency(code: String): UserCurrencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(currencies: List<UserCurrencyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(currency: UserCurrencyEntity)

    @Update
    suspend fun update(currency: UserCurrencyEntity)

    @Query("UPDATE user_currencies SET position = :position WHERE currencyCode = :code")
    suspend fun updatePosition(code: String, position: Int)

    @Query("UPDATE user_currencies SET isVisible = :isVisible WHERE currencyCode = :code")
    suspend fun updateVisibility(code: String, isVisible: Boolean)

    @Query("SELECT COUNT(*) FROM user_currencies")
    suspend fun getCount(): Int
}
