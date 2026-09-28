package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class QuestCategory(val displayName: String, val iconEmoji: String) {
    FITNESS("Fitness & Strength", "⚔️"),
    PRODUCTIVITY("Productivity & Focus", "⚡"),
    LEARNING("Knowledge & Intellect", "📜"),
    EXPLORATION("Exploration & Agility", "🗺️"),
    PERSONAL_DEVELOPMENT("Discipline & Will", "🛡️"),
    CLASS("Class Specialization", "👑")
}

enum class QuestDifficulty(val rank: String, val colorHex: Long) {
    E("E-Rank", 0xFF94A3B8),
    D("D-Rank", 0xFF38BDF8),
    C("C-Rank", 0xFF34D399),
    B("B-Rank", 0xFFF59E0B),
    A("A-Rank", 0xFFA855F7),
    S("S-Rank", 0xFFEF4444)
}

enum class VerificationType {
    SELF_CONFIRMATION,
    TIMER,
    STEPS,
    OPTIONAL_PROOF
}

@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: QuestCategory,
    val difficulty: QuestDifficulty,
    val xpReward: Int,
    val goldReward: Int,
    val targetAttribute: String, // "STRENGTH", "ENDURANCE", "AGILITY", "INTELLIGENCE", "FOCUS", "DISCIPLINE"
    val attributeGain: Int = 1,
    val durationMinutes: Int = 15,
    val timerSecondsRemaining: Int = 15 * 60,
    val isTimerActive: Boolean = false,
    val verificationType: VerificationType = VerificationType.SELF_CONFIRMATION,
    val isCompleted: Boolean = false,
    val isDaily: Boolean = true,
    val bonusObjective: String? = null,
    val dateCreated: Long = System.currentTimeMillis()
)
