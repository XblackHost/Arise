package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.model.PendingOffer
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOfferDao {
    @Query("SELECT * FROM pending_offers WHERE isClaimed = 0 AND isDeclined = 0 AND expiresAt > :now ORDER BY generatedAt ASC")
    fun getActiveOffers(now: Long = System.currentTimeMillis()): Flow<List<PendingOffer>>

    @Query("SELECT * FROM pending_offers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PendingOffer?

    @Insert
    suspend fun insert(offer: PendingOffer): Long

    @Query("UPDATE pending_offers SET isClaimed = 1 WHERE id = :id")
    suspend fun markClaimed(id: Long)

    @Query("UPDATE pending_offers SET isDeclined = 1 WHERE id = :id")
    suspend fun markDeclined(id: Long)

    @Query("DELETE FROM pending_offers WHERE expiresAt < :now AND isClaimed = 0 AND isDeclined = 0")
    suspend fun purgeExpired(now: Long = System.currentTimeMillis())
}
