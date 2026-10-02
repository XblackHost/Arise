package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Consumable
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsumableDao {
    @Query("SELECT * FROM consumables")
    fun getAllConsumables(): Flow<List<Consumable>>

    @Query("SELECT * FROM consumables WHERE itemId = :id LIMIT 1")
    suspend fun getOne(id: String): Consumable?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: Consumable)

    @Query("UPDATE consumables SET count = count - 1 WHERE itemId = :id AND count > 0")
    suspend fun decrement(id: String): Int
}
