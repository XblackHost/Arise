package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EquipmentSlot {
    WEAPON,
    CHEST_ARMOR,
    HELMET,
    BOOTS,
    ACCESSORY
}

enum class ItemRarity(val label: String, val colorHex: Long) {
    COMMON("Common", 0xFF94A3B8),
    UNCOMMON("Uncommon", 0xFF22C55E),
    RARE("Rare", 0xFF38BDF8),
    EPIC("Epic", 0xFFA855F7),
    LEGENDARY("Legendary", 0xFFF59E0B),
    MYTHIC("Mythic", 0xFFEF4444)
}

@Entity(tableName = "equipment_items")
data class Equipment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val slot: EquipmentSlot,
    val rarity: ItemRarity,
    val attackBonus: Int = 0,
    val defenseBonus: Int = 0,
    val hpBonus: Int = 0,
    val mpBonus: Int = 0,
    val speedBonus: Int = 0,
    val isEquipped: Boolean = false,
    val description: String,
    val specialEffect: String? = null,
    val valueGold: Int = 100
)
