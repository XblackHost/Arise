package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MilestoneLog

@Dao
interface MilestoneDao {
    @Query("SELECT EXISTS(SELECT 1 FROM milestone_log WHERE milestoneKey = :key)")
    suspend fun hasFired(key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun log(entry: MilestoneLog): Long
}
