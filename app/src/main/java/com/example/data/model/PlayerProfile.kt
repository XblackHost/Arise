package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "Hunter Jin",
    val title: String = "The Awakened",
    val rank: String = "E-Rank", // E-Rank, D-Rank, C-Rank, B-Rank, A-Rank, S-Rank, Shadow Monarch
    val level: Int = 1,
    val currentXp: Int = 0,
    val requiredXp: Int = 100,
    val hp: Int = 100,
    val maxHp: Int = 100,
    val mp: Int = 50,
    val maxMp: Int = 50,
    val gold: Int = 250,
    val manaCrystals: Int = 10,
    val unallocatedStatPoints: Int = 5,
    // Core attributes
    val strength: Int = 10,
    val endurance: Int = 10,
    val agility: Int = 10,
    val intelligence: Int = 10,
    val focus: Int = 10,
    val discipline: Int = 10,
    val vitality: Int = 10,
    val selectedClass: String = "Warrior",
    val streakDays: Int = 1,
    val totalQuestsCompleted: Int = 0,
    val totalWorkouts: Int = 0,
    val totalSteps: Int = 0,
    val totalDistanceMeters: Float = 0f,
    val shadowArmyCount: Int = 0,
    val isOnboardingComplete: Boolean = false
) {
    fun calculateLevelProgress(): Float {
        return (currentXp.toFloat() / requiredXp.toFloat()).coerceIn(0f, 1f)
    }

    fun calculateTotalPower(): Int {
        return (strength * 3) + (endurance * 2) + (agility * 2) + (intelligence * 3) + (focus * 2) + (vitality * 2) + (level * 15)
    }
}
