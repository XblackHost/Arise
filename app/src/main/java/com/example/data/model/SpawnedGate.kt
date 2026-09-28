package com.example.data.model

import com.example.data.DungeonBoss

data class SpawnedGate(
    val id: String,
    val name: String,
    val rank: String, // "E-Rank", "D-Rank", "C-Rank", "B-Rank", "A-Rank", "S-Rank"
    val boss: DungeonBoss,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Float,
    val distanceFeet: Float,
    val bearingDegrees: Float,
    val directionLabel: String,
    val isRaidable: Boolean,
    val manaCrystalReward: Int,
    val xpReward: Int,
    val goldReward: Int,
    val isCleared: Boolean = false
)
