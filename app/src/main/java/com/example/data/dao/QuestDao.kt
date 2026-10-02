package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Quest
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY isCompleted ASC, dateCreated DESC")
    fun getAllQuests(): Flow<List<Quest>>

    @Query("SELECT * FROM quests WHERE isCompleted = 0 ORDER BY dateCreated DESC")
    fun getActiveQuests(): Flow<List<Quest>>

    @Query("SELECT * FROM quests WHERE isCompleted = 1 ORDER BY dateCreated DESC")
    fun getCompletedQuests(): Flow<List<Quest>>

    @Query("SELECT * FROM quests WHERE id = :id LIMIT 1")
    suspend fun getQuestById(id: Long): Quest?

    @Query("SELECT * FROM quests")
    suspend fun getAllQuestsOnce(): List<Quest>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: Quest): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<Quest>)

    @Update
    suspend fun updateQuest(quest: Quest)

    @Delete
    suspend fun deleteQuest(quest: Quest)

    @Query("UPDATE quests SET isCompleted = 1 WHERE id = :id")
    suspend fun markQuestCompleted(id: Long)

    @Query("UPDATE quests SET isCompleted = 0, isTimerActive = 0, timerSecondsRemaining = durationMinutes * 60 WHERE isDaily = 1")
    suspend fun resetDailyQuests()

    @Query("DELETE FROM quests WHERE isCompleted = 1 AND isDaily = 0")
    suspend fun clearCompletedNonDailyQuests()
}
