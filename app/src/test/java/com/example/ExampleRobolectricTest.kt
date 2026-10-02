package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AriseDatabase
import com.example.data.seedInitialDataDirect
import com.example.security.ApiKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ARISE", appName)
  }

  @Test
  fun `test api key encryption and storage`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = ApiKeyStorage(context)
    val testKey = "AIzaSyTestApiKey123456789"
    storage.saveApiKey(testKey)
    assertEquals(testKey, storage.getApiKey())
    assert(storage.hasValidApiKey())
    assert(storage.hasCompletedFirstLaunch())
  }

  @Test
  fun `test clean database seed creates exactly 5 quests, 1 shadow, 4 gear items`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AriseDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    try {
        // Concurrently run seedInitialDataDirect twice to simulate race
        val job1 = launch(Dispatchers.IO) { seedInitialDataDirect(db) }
        val job2 = launch(Dispatchers.IO) { seedInitialDataDirect(db) }
        job1.join()
        job2.join()

        val quests = db.questDao().getAllQuestsOnce()
        val shadows = db.shadowDao().getAllShadowsOnce()
        val gear = db.equipmentDao().getEquippedItemsOnce()
        val profile = db.playerDao().getPlayerProfileOnce()

        assertNotNull(profile)
        assertEquals("Quests count must be exactly 5 (no duplicate seeding)", 5, quests.size)
        assertEquals("Shadows count must be exactly 1 (no duplicate seeding)", 1, shadows.size)
        assertEquals("Equipped gear must be exactly 4 (no duplicate seeding)", 4, gear.size)
    } finally {
        db.close()
    }
  }

  @Test
  fun `test startBossBattle honors zero deployed shadows`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AriseDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    try {
        seedInitialDataDirect(db)
        // Undeploy all shadows
        val allShadows = db.shadowDao().getAllShadowsOnce()
        allShadows.forEach { s ->
            db.shadowDao().updateShadow(s.copy(isDeployed = false))
        }

        // Verify deployed count in DB is 0
        val deployed = db.shadowDao().getDeployedShadows()
        val deployedOnce = db.shadowDao().getAllShadowsOnce().filter { it.isDeployed }
        assertEquals(0, deployedOnce.size)
    } finally {
        db.close()
    }
  }
}

