package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PurchaseLog

@Dao
interface PurchaseLogDao {
    @Query("SELECT EXISTS(SELECT 1 FROM purchase_log WHERE itemId = :id)")
    suspend fun isPurchased(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun log(entry: PurchaseLog)

    @Query("SELECT * FROM purchase_log")
    suspend fun getAllOnce(): List<PurchaseLog>

    @Query("SELECT * FROM purchase_log")
    fun getAllOnceFlow(): kotlinx.coroutines.flow.Flow<List<com.example.data.model.PurchaseLog>>
}
