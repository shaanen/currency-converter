package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user currency preferences.
 */
@Dao
interface UserCurrencyDao {

    @Query("SELECT * FROM user_currencies WHERE isVisible = 1 ORDER BY position ASC")
    fun getVisibleCurrencies(): Flow<List<UserCurrencyEntity>>

    @Query("SELECT * FROM user_currencies ORDER BY position ASC")
    fun getAllCurrencies(): Flow<List<UserCurrencyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(currencies: List<UserCurrencyEntity>)

    @Query("UPDATE user_currencies SET position = :position WHERE currencyCode = :code")
    suspend fun updatePosition(code: String, position: Int)

    @Query("UPDATE user_currencies SET isVisible = :isVisible WHERE currencyCode = :code")
    suspend fun updateVisibility(code: String, isVisible: Boolean)

    @Query("UPDATE user_currencies SET isVisible = :isVisible, position = :position WHERE currencyCode = :code")
    suspend fun updateVisibilityAndPosition(code: String, isVisible: Boolean, position: Int)

    @Query("SELECT COALESCE(MAX(position), -1) FROM user_currencies WHERE isVisible = 1")
    suspend fun getMaxVisiblePosition(): Int

    @Query("SELECT COUNT(*) FROM user_currencies")
    suspend fun getCount(): Int
}
