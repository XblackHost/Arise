package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Equipment
import com.example.data.model.EquipmentSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {
    @Query("SELECT * FROM equipment_items ORDER BY isEquipped DESC, rarity DESC")
    fun getAllEquipment(): Flow<List<Equipment>>

    @Query("SELECT * FROM equipment_items WHERE isEquipped = 1")
    fun getEquippedItems(): Flow<List<Equipment>>

    @Query("SELECT * FROM equipment_items WHERE isEquipped = 1")
    suspend fun getEquippedItemsOnce(): List<Equipment>

    @Query("SELECT * FROM equipment_items")
    suspend fun getAllEquipmentOnce(): List<Equipment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(item: Equipment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Equipment>)

    @Update
    suspend fun updateEquipment(item: Equipment)

    @Delete
    suspend fun deleteEquipment(item: Equipment)

    @Query("UPDATE equipment_items SET isEquipped = 0 WHERE slot = :slot")
    suspend fun unequipSlot(slot: EquipmentSlot)
}
