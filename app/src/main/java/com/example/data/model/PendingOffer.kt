package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_offers")
data class PendingOffer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val milestoneKey: String,
    val title: String,
    val description: String,
    val goldCost: Int,
    val crystalCost: Int,
    val rewardType: String,        // "EQUIPMENT" | "CONSUMABLE_BUNDLE" | "CRYSTALS"
    val rewardPayload: String,
    val generatedAt: Long,
    val expiresAt: Long,
    val isClaimed: Boolean = false,
    val isDeclined: Boolean = false
) {
    companion object { const val WINDOW_MS = 24L * 60 * 60 * 1000L }
}
