package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "consumables")
data class Consumable(
    @PrimaryKey val itemId: String,
    val count: Int = 0
)
