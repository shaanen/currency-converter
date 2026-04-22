package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_currencies")
data class UserCurrencyEntity(
    @PrimaryKey
    val currencyCode: String,
    val position: Int,
    val isVisible: Boolean = true
)
