package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val deadlineText: String = "Today",
    val priority: String = "Medium", // High, Medium, Low
    val targetStat: String = "DISCIPLINE", // STRENGTH, INTELLIGENCE, FOCUS, DISCIPLINE, VITALITY
    val isCompleted: Boolean = false,
    val xpReward: Int = 35,
    val goldReward: Int = 20,
    val createdAt: Long = System.currentTimeMillis()
)
