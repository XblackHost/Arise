package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vow_state")
data class VowState(
    @PrimaryKey val id: Int = 1,
    val isActive: Boolean = false,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalRewards: Int = 0,
    val lastResetAt: Long = 0L,
    val lastRewardDate: Int = 0,
    val vowStartedAt: Long = 0L,
    val totalXpEarned: Int = 0
) {
    companion object {
        const val WINDOW_MS = 48L * 60L * 60L * 1000L
        fun dateCode(t: Long = System.currentTimeMillis()): Int {
            val c = java.util.Calendar.getInstance().apply { timeInMillis = t }
            return c.get(java.util.Calendar.YEAR) * 1000 + c.get(java.util.Calendar.DAY_OF_YEAR)
        }
    }
    fun isTimerExpired(now: Long = System.currentTimeMillis()): Boolean =
        isActive && (now - lastResetAt) >= WINDOW_MS
    fun hoursRemaining(now: Long = System.currentTimeMillis()): Long =
        if (!isActive) 0L else ((WINDOW_MS - (now - lastResetAt)) / 3_600_000L).coerceAtLeast(0L)
}
