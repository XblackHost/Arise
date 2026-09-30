package com.example

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.example.data.HunterRadarManager
import com.example.data.RadarEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HunterGpsRadarTest {

    private lateinit var context: Context
    private lateinit var radarManager: HunterRadarManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        radarManager = HunterRadarManager(context)
    }

    @Test
    fun `test initial radar engine defaults to online maplibre`() {
        assertEquals(RadarEngine.ONLINE_MAPLIBRE, radarManager.radarEngine.value)
        assertEquals("Online: MapLibre GL", radarManager.radarEngine.value.displayName)
        assertEquals(2000f, radarManager.radarRangeFeet.value)
    }

    @Test
    fun `test toggle radar engine switches to offline sqlite and watermelon db`() {
        radarManager.toggleRadarEngine()
        assertEquals(RadarEngine.OFFLINE_SQLITE, radarManager.radarEngine.value)
        assertEquals("Offline: SQLite / WatermelonDB", radarManager.radarEngine.value.displayName)

        radarManager.toggleRadarEngine()
        assertEquals(RadarEngine.ONLINE_MAPLIBRE, radarManager.radarEngine.value)
    }

    @Test
    fun `test set radar range changes zoom level`() {
        radarManager.setRadarRange(500f)
        assertEquals(500f, radarManager.radarRangeFeet.value)

        radarManager.setRadarRange(1000f)
        assertEquals(1000f, radarManager.radarRangeFeet.value)
    }

    @Test
    fun `test gate spawning creates multiple gates of various ranks`() {
        val gates = radarManager.spawnedGates.value
        assertTrue("Should have spawned at least 5 gates", gates.size >= 5)

        val ranks = gates.map { it.rank }.toSet()
        assertTrue("Should have E-Rank gates", ranks.contains("E-Rank"))
        assertNotNull(radarManager.selectedGate.value)
    }

    @Test
    fun `test gps location update updates coordinates and recalculates distances`() {
        assertFalse(radarManager.hasGpsFix.value)

        val testLoc = Location("gps").apply {
            latitude = 37.7800
            longitude = -122.4100
            accuracy = 5f
            time = System.currentTimeMillis()
        }

        radarManager.onLocationChanged(testLoc)

        assertTrue(radarManager.hasGpsFix.value)
        assertEquals(37.7800, radarManager.currentLatitude.value, 0.0001)
        assertEquals(-122.4100, radarManager.currentLongitude.value, 0.0001)

        val updatedGates = radarManager.spawnedGates.value
        assertTrue("Gates distances should be positive", updatedGates.all { it.distanceFeet > 0f })
    }

    @Test
    fun `test advancing towards gate reduces distance and unlocks raid range`() {
        val gates = radarManager.spawnedGates.value
        val targetGate = gates.first()
        radarManager.selectGate(targetGate)

        val initialDistance = targetGate.distanceFeet

        // Advance 100 feet toward target gate
        radarManager.addWalkedFeet(100f)

        val selectedAfterWalk = radarManager.selectedGate.value
        assertNotNull(selectedAfterWalk)
        assertTrue(
            "Distance after walk should decrease: before=$initialDistance, after=${selectedAfterWalk!!.distanceFeet}",
            selectedAfterWalk.distanceFeet < initialDistance
        )

        // Advance large distance to enter gate raid range
        radarManager.addWalkedFeet(selectedAfterWalk.distanceFeet + 50f)
        val finalGateState = radarManager.selectedGate.value
        assertNotNull(finalGateState)
        assertTrue("Gate should now be raidable within 165 ft", finalGateState!!.isRaidable)
    }

    @Test
    fun `test clear gate marks gate as cleared`() {
        val targetGate = radarManager.spawnedGates.value.first()
        radarManager.selectGate(targetGate)

        assertFalse(targetGate.isCleared)

        radarManager.clearGate(targetGate.id)

        val clearedGate = radarManager.spawnedGates.value.find { it.id == targetGate.id }
        assertNotNull(clearedGate)
        assertTrue(clearedGate!!.isCleared)
        assertTrue(radarManager.selectedGate.value!!.isCleared)
    }

    @Test
    fun `test walking steps and metrics calculation`() {
        radarManager.addSteps(100)

        assertEquals(100, radarManager.sessionSteps.value)
        assertEquals(250f, radarManager.sessionFeet.value, 1f) // 100 * 2.5 ft
        assertEquals(4f, radarManager.burnedCalories.value, 0.1f) // 100 * 0.04 cal
    }

    @Test
    fun `test auto step counter and walked feet accumulation`() {
        assertEquals(0, radarManager.sessionSteps.value)
        assertEquals(0f, radarManager.sessionFeet.value, 0.01f)

        // Simulate walking 50 feet
        radarManager.addWalkedFeet(50f)

        assertTrue("Steps should be counted from feet walked", radarManager.sessionSteps.value > 0)
        assertEquals(50f, radarManager.sessionFeet.value, 0.1f)
        assertTrue("Meters should be calculated from feet", radarManager.sessionMeters.value > 0f)
        assertTrue("Calories burned should be greater than zero", radarManager.burnedCalories.value > 0f)
    }

    @Test
    fun `test physical walking speed and bearing calculation from real-time GPS`() {
        // First location (user standing at point A)
        val loc1 = Location("gps").apply {
            latitude = 37.7749
            longitude = -122.4194
            accuracy = 4f
            time = 1000000L
        }
        radarManager.onLocationChanged(loc1)

        // Second location 2 seconds later (user walked ~3 meters North-East)
        val loc2 = Location("gps").apply {
            latitude = 37.7750
            longitude = -122.4193
            accuracy = 4f
            speed = 1.35f // ~1.35 m/s human walking speed
            bearing = 45f // heading North-East
            time = 1002000L
        }
        radarManager.onLocationChanged(loc2)

        assertTrue("GPS fix should be true", radarManager.hasGpsFix.value)
        assertTrue("Walking speed should be registered", radarManager.walkingSpeedMps.value > 0.5f)
        assertTrue("isWalking should be true when moving", radarManager.isWalking.value)
        assertEquals(45f, radarManager.playerBearing.value, 1f)
        assertTrue("Session steps should increment from physical GPS displacement", radarManager.sessionSteps.value > 0)
    }

    @Test
    fun `test extracted S-Rank boss scales down according to E-Rank hunter capacity`() {
        val sRankBoss = com.example.data.BossCatalog.allBosses.first { it.rank.contains("S-Rank") }

        // E-Rank Hunter Stats
        val hunterHp = 120
        val hunterStr = 10
        val hunterEnd = 10

        val scaled = com.example.viewmodel.AriseViewModel.calculateScaledShadowStats(sRankBoss, hunterHp, hunterStr, hunterEnd)

        // Verify it is OP compared to player HP (120 HP), but not 12,000 HP broken
        assertTrue("Scaled HP should be greater than player HP", scaled.maxHp > hunterHp * 2)
        assertTrue("Scaled HP should be bounded reasonably (< 1000 for early player)", scaled.maxHp < 1000)
        assertTrue("Scaled ATK should be formidable", scaled.attack >= 50)
        assertTrue("Reconstitution cost should require MP", scaled.mpReconstituteCost in 15..25)
    }

    @Test
    fun `test passive MP reconstitution mechanics on shadow fatal damage`() {
        val testShadow = com.example.viewmodel.ActiveBattleShadow(
            id = 1L,
            name = "Igris",
            title = "Knight of Blood & Shadows",
            rank = "Commander",
            currentHp = 100,
            maxHp = 300,
            attackPower = 80,
            defense = 50,
            signatureSkill = "Bloodred Sever",
            mpReconstituteCost = 20,
            isAlive = true
        )

        // Case 1: Fatal damage with sufficient Hunter MP (30 MP >= 20 MP cost)
        val resultWithMp = com.example.viewmodel.AriseViewModel.processShadowDamageAndReconstitution(
            shadow = testShadow,
            damage = 150, // exceeds currentHp (100) -> fatal blow
            currentHunterMp = 30
        )
        assertTrue("Shadow should automatically reconstitute from shadows", resultWithMp.didReconstitute)
        assertEquals("Hunter MP should be consumed for reconstitution", 20, resultWithMp.consumedMp)
        assertEquals("Shadow HP should be restored to max", 300, resultWithMp.updatedShadow.currentHp)
        assertTrue("Shadow should remain alive on battlefield", resultWithMp.updatedShadow.isAlive)

        // Case 2: Fatal damage with depleted Hunter MP (10 MP < 20 MP cost)
        val resultWithoutMp = com.example.viewmodel.AriseViewModel.processShadowDamageAndReconstitution(
            shadow = testShadow,
            damage = 150,
            currentHunterMp = 10
        )
        assertFalse("Shadow cannot reconstitute without sufficient MP", resultWithoutMp.didReconstitute)
        assertEquals("No MP consumed when insufficient", 0, resultWithoutMp.consumedMp)
        assertEquals("Shadow HP should drop to 0", 0, resultWithoutMp.updatedShadow.currentHp)
        assertFalse("Shadow should enter dormant state", resultWithoutMp.updatedShadow.isAlive)

        // Case 3: Non-fatal damage
        val nonFatalResult = com.example.viewmodel.AriseViewModel.processShadowDamageAndReconstitution(
            shadow = testShadow,
            damage = 40,
            currentHunterMp = 50
        )
        assertFalse("Non-fatal damage does not trigger reconstitution", nonFatalResult.didReconstitute)
        assertEquals("No MP consumed on non-fatal damage", 0, nonFatalResult.consumedMp)
        assertEquals("Shadow HP reduced by damage amount", 60, nonFatalResult.updatedShadow.currentHp)
        assertTrue("Shadow remains alive", nonFatalResult.updatedShadow.isAlive)
    }
}
