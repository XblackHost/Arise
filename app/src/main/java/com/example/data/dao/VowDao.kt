package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VowState
import kotlinx.coroutines.flow.Flow

@Dao
interface VowDao {
    @Query("SELECT * FROM vow_state WHERE id = 1 LIMIT 1")
    fun getVowState(): Flow<VowState?>

    @Query("SELECT * FROM vow_state WHERE id = 1 LIMIT 1")
    suspend fun getVowStateOnce(): VowState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: VowState)
}
