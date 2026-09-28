package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shadow_units")
data class ShadowUnit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val title: String, // e.g. "Bloodred Commander", "Cerberus Hound", "Shadow Magus"
    val rank: String, // "Infantry", "Elite", "Knight", "Commander", "General", "Marshal"
    val level: Int = 1,
    val currentXp: Int = 0,
    val requiredXp: Int = 150,
    val maxHp: Int = 250,
    val currentHp: Int = 250,
    val attackPower: Int,
    val defense: Int,
    val speed: Int,
    val loyalty: Int = 100, // 0 - 100%
    val signatureSkill: String,
    val skillDescription: String,
    val isSummoned: Boolean = true,
    val isDeployed: Boolean = true, // Whether assigned to active raid squadron
    val originRank: String = "E-Rank", // The original dungeon boss rank (e.g. "S-Rank Gate", "A-Rank Red Gate")
    val mpUpkeepCost: Int = 15, // MP consumed from Hunter to reconstitute when defeated
    val iconEmoji: String = "👥",
    val extractionDate: Long = System.currentTimeMillis(),
    val lore: String
)
