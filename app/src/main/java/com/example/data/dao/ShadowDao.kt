package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShadowUnit
import kotlinx.coroutines.flow.Flow

@Dao
interface ShadowDao {
    @Query("SELECT * FROM shadow_units ORDER BY attackPower DESC")
    fun getAllShadows(): Flow<List<ShadowUnit>>

    @Query("SELECT * FROM shadow_units WHERE isSummoned = 1")
    fun getSummonedShadows(): Flow<List<ShadowUnit>>

    @Query("SELECT * FROM shadow_units WHERE isDeployed = 1 ORDER BY attackPower DESC")
    fun getDeployedShadows(): Flow<List<ShadowUnit>>

    @Query("SELECT * FROM shadow_units WHERE id = :id")
    suspend fun getShadowById(id: Long): ShadowUnit?

    @Query("SELECT * FROM shadow_units")
    suspend fun getAllShadowsOnce(): List<ShadowUnit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShadow(unit: ShadowUnit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShadows(units: List<ShadowUnit>)

    @Update
    suspend fun updateShadow(unit: ShadowUnit)

    @Delete
    suspend fun deleteShadow(unit: ShadowUnit)

    @Query("SELECT COUNT(*) FROM shadow_units")
    suspend fun getShadowCount(): Int
}
