package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_log")
data class PurchaseLog(
    @PrimaryKey val itemId: String,
    val purchasedAt: Long = System.currentTimeMillis()
)
