package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milestone_log")
data class MilestoneLog(
    @PrimaryKey val milestoneKey: String,
    val firedAt: Long = System.currentTimeMillis()
)
