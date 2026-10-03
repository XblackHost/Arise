package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.data.AriseDatabase
import com.example.data.BossCatalog
import com.example.data.DungeonBoss
import com.example.data.HunterRadarManager
import com.example.data.JoinResult
import com.example.data.LanMultiplayerManager
import com.example.data.seedInitialDataDirect
import com.example.data.model.*
import com.example.security.ApiKeyStorage
import androidx.room.withTransaction
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class ActiveBattleShadow(
    val id: Long,
    val name: String,
    val title: String,
    val rank: String,
    val iconEmoji: String = "👥",
    val currentHp: Int,
    val maxHp: Int,
    val attackPower: Int,
    val defense: Int,
    val signatureSkill: String,
    val mpReconstituteCost: Int = 15,
    val isAlive: Boolean = true,
    val originRank: String = "E-Rank"
)

data class BattleState(
    val inBattle: Boolean = false,
    val currentBoss: DungeonBoss? = null,
    val bossCurrentHp: Int = 100,
    val playerCurrentHp: Int = 120,
    val playerMaxHp: Int = 120,
    val playerCurrentMp: Int = 60,
    val playerMaxMp: Int = 60,
    val activeShadows: List<ActiveBattleShadow> = emptyList(),
    val logMessages: List<String> = emptyList(),
    val isVictory: Boolean = false,
    val isDefeat: Boolean = false,
    val extractionEligible: Boolean = false,
    val isExtracted: Boolean = false,
    val isAiThinking: Boolean = false,
    val isBossEnraged: Boolean = false,
    val isBossChargingUltimate: Boolean = false,
    val isPlayerDefending: Boolean = false,
    val healthPotionsRemaining: Int = 2
)

class AriseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AriseDatabase.getDatabase(application)
    val apiKeyStorage = ApiKeyStorage(application)
    val geminiService = GeminiService(apiKeyStorage)

    // First-launch & Key State
    private val _hasCompletedFirstLaunch = MutableStateFlow(apiKeyStorage.hasCompletedFirstLaunch())
    val hasCompletedFirstLaunch: StateFlow<Boolean> = _hasCompletedFirstLaunch.asStateFlow()

    private val _hasValidApiKey = MutableStateFlow(apiKeyStorage.hasValidApiKey())
    val hasValidApiKey: StateFlow<Boolean> = _hasValidApiKey.asStateFlow()

    private val _maskedApiKey = MutableStateFlow(apiKeyStorage.getMaskedApiKey())
    val maskedApiKey: StateFlow<String> = _maskedApiKey.asStateFlow()

    private val _keyValidationStatus = MutableStateFlow<String?>(null)
    val keyValidationStatus: StateFlow<String?> = _keyValidationStatus.asStateFlow()

    // Level-Up Celebration Banner
    private val _celebrationEvent = MutableStateFlow<String?>(null)
    val celebrationEvent: StateFlow<String?> = _celebrationEvent.asStateFlow()

    // Database Flows
    val playerProfile: StateFlow<PlayerProfile?> = db.playerDao().getPlayerProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val quests: StateFlow<List<Quest>> = db.questDao().getAllQuests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shadowArmy: StateFlow<List<ShadowUnit>> = db.shadowDao().getAllShadows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equipment: StateFlow<List<Equipment>> = db.equipmentDao().getAllEquipment()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskItem>> = db.taskDao().getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = db.chatDao().getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Battle State
    private val _battleState = MutableStateFlow(BattleState())
    val battleState: StateFlow<BattleState> = _battleState.asStateFlow()

    // Radar & GPS Exploration Manager
    val radarManager = HunterRadarManager(application)
    val isTrackingSteps: StateFlow<Boolean> = radarManager.isTracking
    val sessionSteps: StateFlow<Int> = radarManager.sessionSteps
    val sessionDistanceFeet: StateFlow<Float> = radarManager.sessionFeet
    val sessionDistanceMeters: StateFlow<Float> = radarManager.sessionMeters
    val burnedCalories: StateFlow<Float> = radarManager.burnedCalories
    val hasGpsFix: StateFlow<Boolean> = radarManager.hasGpsFix
    val isLocationServiceEnabled: StateFlow<Boolean> = radarManager.isLocationServiceEnabled
    val currentLatitude: StateFlow<Double> = radarManager.currentLatitude
    val currentLongitude: StateFlow<Double> = radarManager.currentLongitude
    val spawnedGates: StateFlow<List<SpawnedGate>> = radarManager.spawnedGates
    val selectedGate: StateFlow<SpawnedGate?> = radarManager.selectedGate
    val radarEngine = radarManager.radarEngine
    val radarRangeFeet = radarManager.radarRangeFeet
    val sqliteCachedGateCount = radarManager.sqliteCachedGateCount
    val playerBearing: StateFlow<Float> = radarManager.playerBearing
    val walkingSpeedMps: StateFlow<Float> = radarManager.walkingSpeedMps
    val isWalking: StateFlow<Boolean> = radarManager.isWalking

    // LAN / Wi-Fi Multiplayer Squadron Manager
    val multiplayerManager = LanMultiplayerManager(application)
    val currentParty: StateFlow<HunterParty?> = multiplayerManager.currentParty
    val discoveredParties: StateFlow<List<DiscoveredParty>> = multiplayerManager.discoveredParties
    val isBeaconActive: StateFlow<Boolean> = multiplayerManager.isBeaconActive

    // AI companion loading
    private val _isNyxReplying = MutableStateFlow(false)
    val isNyxReplying: StateFlow<Boolean> = _isNyxReplying.asStateFlow()

    // Concurrency Mutexes to prevent race conditions
    private val stepWriteMutex = Mutex()
    private val questCompletionMutex = Mutex()
    private val extractionMutex = Mutex()
    private val equipMutex = Mutex()
    private val statAllocationMutex = Mutex()
    private val vowMutex = Mutex()
    private val shopMutex = Mutex()
    private val milestoneMutex = Mutex()

    val vowState: StateFlow<VowState?> = db.vowDao().getVowState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val consumables: StateFlow<List<Consumable>> = db.consumableDao()
        .getAllConsumables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ownedCosmeticIds: StateFlow<Set<String>> = db.purchaseLogDao()
        .getAllOnceFlow()
        .map { list -> list.map { it.itemId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val activeOffers: StateFlow<List<PendingOffer>> = db.pendingOfferDao()
        .getActiveOffers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastPersistedSteps = 0
    private var lastPersistedMeters = 0f

    init {
        // M6: Database seed verification on startup
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (db.playerDao().getPlayerProfileOnce() == null) {
                    seedInitialDataDirect(db)
                }
            } catch (e: Exception) {
                Log.w("AriseViewModel", "Database initial seed error: ${e.message}", e)
            }
        }

        // Vow of Discipline midnight check
        viewModelScope.launch(Dispatchers.IO) {
            vowMutex.withLock { processMidnightVowCheck() }
        }

        // Milestone purge expired & check pending offers
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.pendingOfferDao().purgeExpired()
                milestoneMutex.withLock { checkMilestones() }
            } catch (e: Exception) {
                Log.w("AriseViewModel", "Milestone check error: ${e.message}", e)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(5 * 60_000L)   // every 5 minutes
                try {
                    db.pendingOfferDao().purgeExpired()
                } catch (_: Exception) { }
            }
        }

        // STILL-16 & m7: Daily Quest Reset on New Day
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = getApplication<Application>().getSharedPreferences("arise_daily_tracker", Context.MODE_PRIVATE)
                val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val todayCode = currentYear * 1000 + currentDay
                val lastResetCode = prefs.getInt("last_daily_reset_code", -1)
                if (lastResetCode != todayCode) {
                    db.questDao().resetDailyQuests()
                    prefs.edit().putInt("last_daily_reset_code", todayCode).apply()
                }
            } catch (e: Exception) {
                Log.w("AriseViewModel", "Daily reset error: ${e.message}", e)
            }
        }

        // C-01: Sensor & physical exploration steps persistence (single source of truth)
        viewModelScope.launch {
            radarManager.sessionSteps.collect { total ->
                val delta = total - lastPersistedSteps
                if (delta >= 10 || (total > 0 && delta >= 5)) {
                    lastPersistedSteps = total
                    stepWriteMutex.withLock {
                        withContext(Dispatchers.IO) {
                            val p = db.playerDao().getPlayerProfileOnce() ?: return@withContext
                            db.playerDao().updateProfile(p.copy(totalSteps = p.totalSteps + delta))
                        }
                    }
                }
            }
        }

        // M-01: GPS & exploration walked meters persistence (single source of truth)
        viewModelScope.launch {
            radarManager.sessionMeters.collect { total ->
                val delta = total - lastPersistedMeters
                if (delta >= 5f) {
                    lastPersistedMeters = total
                    stepWriteMutex.withLock {
                        withContext(Dispatchers.IO) {
                            val p = db.playerDao().getPlayerProfileOnce() ?: return@withContext
                            db.playerDao().updateProfile(p.copy(totalDistanceMeters = p.totalDistanceMeters + delta))
                        }
                    }
                }
            }
        }

        // C-02 & M-02: Auto-complete physical walking / steps quests when distance threshold is reached
        viewModelScope.launch {
            combine(radarManager.sessionMeters, quests) { m, q -> m to q }
                .distinctUntilChanged { old, new -> old.first == new.first && old.second == new.second }
                .collect { (meters, list) ->
                    list.filter { !it.isCompleted && it.verificationType == VerificationType.STEPS }
                        .forEach { q ->
                            val t = q.title.lowercase()
                            val threshold = when {
                                t.contains("1.5 km") -> 1500f
                                t.contains("1 km") -> 1000f
                                t.contains("500 m") -> 500f
                                else -> null
                            }
                            if (threshold != null && meters >= threshold) {
                                completeQuest(q)
                            }
                        }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        radarManager.stopTracking()
        multiplayerManager.shutdown()
        val remainingSteps = radarManager.sessionSteps.value - lastPersistedSteps
        val remainingMeters = radarManager.sessionMeters.value - lastPersistedMeters
        if (remainingSteps > 0 || remainingMeters > 0f) {
            CoroutineScope(Dispatchers.IO).launch {
                stepWriteMutex.withLock {
                    val p = db.playerDao().getPlayerProfileOnce() ?: return@withLock
                    val finalSteps = if (remainingSteps > 0) p.totalSteps + remainingSteps else p.totalSteps
                    val finalMeters = if (remainingMeters > 0f) p.totalDistanceMeters + remainingMeters else p.totalDistanceMeters
                    db.playerDao().updateProfile(p.copy(totalSteps = finalSteps, totalDistanceMeters = finalMeters))
                }
            }
        }
    }

    // ----------------------------------------------------
    // API KEY & FIRST LAUNCH MANAGEMENT
    // ----------------------------------------------------

    fun saveApiKey(rawKey: String, onDone: (Boolean) -> Unit) {
        val isCheatCode = rawKey.trim() == com.example.security.ApiKeyStorage.CHEAT_CODE_API_KEY
        val effectiveKey = if (isCheatCode) com.example.security.ApiKeyStorage.CHEAT_RESOLVED_KEY else rawKey.trim()
        viewModelScope.launch(Dispatchers.IO) {
            _keyValidationStatus.value = "Encrypting and verifying key with Google Gemini..."
            val verifyResult = geminiService.verifyKey(effectiveKey)
            if (verifyResult.isSuccess || isCheatCode) {
                val saveResult = apiKeyStorage.saveApiKey(effectiveKey)
                if (saveResult.isFailure) {
                    _keyValidationStatus.value = "Encryption failed: ${saveResult.exceptionOrNull()?.message}"
                    onDone(false)
                    return@launch
                }
                _maskedApiKey.value = apiKeyStorage.getMaskedApiKey()
                _hasValidApiKey.value = true
                _hasCompletedFirstLaunch.value = true
                _keyValidationStatus.value = "Gemini Key verified & locked in local secure vault!"
                onDone(true)
            } else {
                val errMsg = verifyResult.exceptionOrNull()?.message ?: "Verification failed."
                val isNetworkError = errMsg.contains("UnknownHost", ignoreCase = true) ||
                        errMsg.contains("ConnectException", ignoreCase = true) ||
                        errMsg.contains("timeout", ignoreCase = true)
                if (isNetworkError) {
                    val saveResult = apiKeyStorage.saveApiKey(effectiveKey)
                    if (saveResult.isFailure) {
                        _keyValidationStatus.value = "Encryption failed: ${saveResult.exceptionOrNull()?.message}"
                        onDone(false)
                        return@launch
                    }
                    _maskedApiKey.value = apiKeyStorage.getMaskedApiKey()
                    _hasValidApiKey.value = true
                    _hasCompletedFirstLaunch.value = true
                    _keyValidationStatus.value = "Notice: Saved in offline mode (Network unreachable)."
                    onDone(true)
                } else {
                    _keyValidationStatus.value = "Error: Invalid Gemini API key ($errMsg)."
                    onDone(false)
                }
            }
        }
    }

    fun skipApiKeyFirstLaunch() {
        apiKeyStorage.markFirstLaunchCompleted()
        _hasCompletedFirstLaunch.value = true
    }

    fun clearApiKey() {
        apiKeyStorage.clearApiKey()
        _maskedApiKey.value = apiKeyStorage.getMaskedApiKey()
        _hasValidApiKey.value = false
    }

    fun addCustomQuest(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) {
            db.questDao().insertQuest(quest)
            _celebrationEvent.value = "NEW MISSION REGISTERED: '${quest.title}'"
        }
    }

    fun toggleEquip(equipment: Equipment) = viewModelScope.launch(Dispatchers.IO) {
        equipMutex.withLock {
            try {
                db.withTransaction {
                    if (equipment.isEquipped) {
                        db.equipmentDao().unequipSlot(equipment.slot)
                    } else {
                        db.equipmentDao().unequipSlot(equipment.slot)
                        db.equipmentDao().updateEquipment(equipment.copy(isEquipped = true))
                    }
                }
                _celebrationEvent.value = if (equipment.isEquipped) "UNEQUIPPED: ${equipment.name}"
                else "EQUIPPED: ${equipment.name} (${equipment.slot.name})"
            } catch (e: Exception) {
                _celebrationEvent.value = "Equipment swap failed: ${e.message}"
            }
        }
    }

    // ----------------------------------------------------
    // PLAYER & PROGRESSION
    // ----------------------------------------------------

    fun allocateStat(statName: String) = viewModelScope.launch(Dispatchers.IO) {
        statAllocationMutex.withLock {
            db.withTransaction {
                val current = db.playerDao().getPlayerProfileOnce() ?: return@withTransaction
                if (current.unallocatedStatPoints <= 0) return@withTransaction

                val updated = when (statName.uppercase()) {
                    "STRENGTH" -> current.copy(
                        strength = current.strength + 1,
                        unallocatedStatPoints = current.unallocatedStatPoints - 1
                    )
                    "ENDURANCE" -> current.copy(
                        endurance = current.endurance + 1,
                        unallocatedStatPoints = current.unallocatedStatPoints - 1
                    )
                    "AGILITY" -> current.copy(
                        agility = current.agility + 1,
                        unallocatedStatPoints = current.unallocatedStatPoints - 1
                    )
                    "INTELLIGENCE" -> current.copy(
                        intelligence = current.intelligence + 1,
                        mp = current.mp + 5,
                        maxMp = current.maxMp + 5,
                        unallocatedStatPoints = current.unallocatedStatPoints - 1
                    )
                    "FOCUS" -> current.copy(
                        focus = current.focus + 1,
                        unallocatedStatPoints = current.unallocatedStatPoints - 1
                    )
                    "DISCIPLINE" -> current.copy(
                        discipline = current.discipline + 1,
                        unallocatedStatPoints = current.unallocatedStatPoints - 1
                    )
                    "VITALITY" -> current.copy(
                        vitality = current.vitality + 1,
                        hp = current.hp + 10,
                        maxHp = current.maxHp + 10,
                        unallocatedStatPoints = current.unallocatedStatPoints - 1
                    )
                    else -> current
                }

                db.playerDao().updateProfile(updated)
            }
        }
    }

    fun completeQuest(quest: Quest) = viewModelScope.launch(Dispatchers.IO) {
        questCompletionMutex.withLock {
            var didLevelUp = false
            var newLevel = 1
            var hadVigor = false

            db.withTransaction {
                val fresh = db.questDao().getQuestById(quest.id) ?: return@withTransaction
                if (fresh.isCompleted) return@withTransaction

                val profile = db.playerDao().getPlayerProfileOnce() ?: return@withTransaction

                hadVigor = db.consumableDao().getOne("cons_questboost")?.count?.let { it > 0 } == true
                val xpMultiplier = if (hadVigor) 1.5f else 1.0f
                val awardedXp = (fresh.xpReward * xpMultiplier).toInt()

                var newXp = profile.currentXp + awardedXp
                newLevel = profile.level
                var newReqXp = profile.requiredXp
                var newStatPoints = profile.unallocatedStatPoints
                var newHp = profile.hp
                var newMaxHp = profile.maxHp

                while (newXp >= newReqXp) {
                    newXp -= newReqXp
                    newLevel += 1
                    newReqXp = maxOf((newReqXp * 1.35).toInt(), newReqXp + 1).coerceAtMost(Int.MAX_VALUE / 2)
                    newStatPoints += 3
                    newMaxHp += 20
                    newHp = newMaxHp
                    didLevelUp = true
                }

                // Stat gain from quest
                val updated = when (fresh.targetAttribute.uppercase()) {
                    "STRENGTH" -> profile.copy(strength = profile.strength + fresh.attributeGain)
                    "ENDURANCE" -> profile.copy(endurance = profile.endurance + fresh.attributeGain)
                    "AGILITY" -> profile.copy(agility = profile.agility + fresh.attributeGain)
                    "INTELLIGENCE" -> profile.copy(intelligence = profile.intelligence + fresh.attributeGain)
                    "FOCUS" -> profile.copy(focus = profile.focus + fresh.attributeGain)
                    "DISCIPLINE" -> profile.copy(discipline = profile.discipline + fresh.attributeGain)
                    "VITALITY" -> profile.copy(vitality = profile.vitality + fresh.attributeGain)
                    else -> profile
                }.copy(
                    level = newLevel,
                    currentXp = newXp,
                    requiredXp = newReqXp,
                    hp = newHp,
                    maxHp = newMaxHp,
                    unallocatedStatPoints = newStatPoints,
                    gold = profile.gold + fresh.goldReward,
                    totalQuestsCompleted = profile.totalQuestsCompleted + 1,
                    totalWorkouts = if (fresh.category == QuestCategory.FITNESS) profile.totalWorkouts + 1 else profile.totalWorkouts,
                    rank = determineRank(newLevel),
                    title = if (newLevel >= 25) "Shadow Monarch" else if (newLevel >= 15) "Vanguard Commander" else profile.title
                )

                db.questDao().markQuestCompleted(fresh.id)
                db.playerDao().updateProfile(updated)
            }

            if (hadVigor) {
                db.consumableDao().decrement("cons_questboost")
            }

            if (didLevelUp) {
                _celebrationEvent.value = "LEVEL UP! Hunter reached Level $newLevel! +3 Stat Points awarded!"
                checkMilestonesAfterLevelUp()
            }
        }
    }

    fun updateQuestTimer(questId: Long, secondsLeft: Int) = viewModelScope.launch(Dispatchers.IO) {
        val q = db.questDao().getQuestById(questId) ?: return@launch
        db.questDao().updateQuest(q.copy(timerSecondsRemaining = secondsLeft, isTimerActive = true))
    }

    private fun determineRank(level: Int): String {
        return when {
            level >= 25 -> "Shadow Monarch"
            level >= 20 -> "S-Rank"
            level >= 15 -> "A-Rank"
            level >= 10 -> "B-Rank"
            level >= 6 -> "C-Rank"
            level >= 3 -> "D-Rank"
            else -> "E-Rank"
        }
    }

    fun dismissCelebration() {
        _celebrationEvent.value = null
    }

    fun setPlayerClass(newClass: String) = viewModelScope.launch(Dispatchers.IO) {
        val current = db.playerDao().getPlayerProfileOnce() ?: return@launch
        db.playerDao().updateProfile(current.copy(selectedClass = newClass))
    }

    fun completeOnboarding(name: String, chosenClass: String) = viewModelScope.launch(Dispatchers.IO) {
        val current = db.playerDao().getPlayerProfileOnce() ?: return@launch
        db.playerDao().updateProfile(
            current.copy(
                name = name.ifBlank { "Awakened Hunter" },
                selectedClass = chosenClass,
                isOnboardingComplete = true
            )
        )
    }

    // ----------------------------------------------------
    // SHADOW ARMY & DUNGEON COMBAT WITH SCALING & MP RECONSTITUTION
    // ----------------------------------------------------

    companion object {
        /**
         * Dynamically scales extracted boss stats according to the Hunter's level and rank.
         * Prevents high-tier (e.g. S-Rank) bosses from breaking balance when defeated by
         * early-tier (e.g. E-Rank) hunters, while remaining remarkably powerful ("pretty OP but not broken").
         */
        fun calculateScaledShadowStats(
            boss: DungeonBoss,
            hunterMaxHp: Int,
            hunterStrength: Int,
            hunterEndurance: Int
        ): ScaledShadowStats {
            val bossRankWeight = when {
                boss.rank.contains("Monarch", ignoreCase = true) || boss.rank.contains("S-Rank", ignoreCase = true) -> 1.95f
                boss.rank.contains("A-Rank", ignoreCase = true) -> 1.65f
                boss.rank.contains("B-Rank", ignoreCase = true) -> 1.45f
                boss.rank.contains("C-Rank", ignoreCase = true) -> 1.30f
                boss.rank.contains("D-Rank", ignoreCase = true) -> 1.18f
                else -> 1.05f
            }

            val baseHunterAtk = (hunterStrength * 2.2f + 14f).coerceAtLeast(20f)
            val baseHunterDef = (hunterEndurance * 1.5f + 10f).coerceAtLeast(14f)

            val scaledMaxHp = (hunterMaxHp * bossRankWeight * 1.45f).toInt()
            val scaledAtk = (baseHunterAtk * bossRankWeight * 1.38f).toInt().coerceAtLeast(28)
            val scaledDef = (baseHunterDef * bossRankWeight * 1.25f).toInt().coerceAtLeast(16)
            val mpReconstitutionCost = (12 + (bossRankWeight * 4.5f)).toInt()

            return ScaledShadowStats(
                maxHp = scaledMaxHp,
                attack = scaledAtk,
                defense = scaledDef,
                mpReconstituteCost = mpReconstitutionCost,
                weight = bossRankWeight
            )
        }

        fun processShadowDamageAndReconstitution(
            shadow: ActiveBattleShadow,
            damage: Int,
            currentHunterMp: Int
        ): ReconstitutionResult {
            val remainingHp = shadow.currentHp - damage
            return if (remainingHp <= 0) {
                if (currentHunterMp >= shadow.mpReconstituteCost) {
                    ReconstitutionResult(
                        updatedShadow = shadow.copy(currentHp = shadow.maxHp, isAlive = true),
                        consumedMp = shadow.mpReconstituteCost,
                        didReconstitute = true
                    )
                } else {
                    ReconstitutionResult(
                        updatedShadow = shadow.copy(currentHp = 0, isAlive = false),
                        consumedMp = 0,
                        didReconstitute = false
                    )
                }
            } else {
                ReconstitutionResult(
                    updatedShadow = shadow.copy(currentHp = remainingHp),
                    consumedMp = 0,
                    didReconstitute = false
                )
            }
        }

        fun computeSynergyPercent(isMonarch: Boolean, deployedCount: Int): Int {
            val base = when {
                deployedCount >= 3 -> 25
                deployedCount == 2 -> 15
                deployedCount == 1 -> 10
                else -> 0
            }
            return base + (if (isMonarch) 30 else 0)
        }
    }

    data class ReconstitutionResult(
        val updatedShadow: ActiveBattleShadow,
        val consumedMp: Int,
        val didReconstitute: Boolean
    )

    data class ScaledShadowStats(
        val maxHp: Int,
        val attack: Int,
        val defense: Int,
        val mpReconstituteCost: Int,
        val weight: Float
    )

    fun startBossBattle(boss: DungeonBoss) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = db.playerDao().getPlayerProfileOnce()
            val allShadows = db.shadowDao().getAllShadowsOnce()
            val deployedShadows = allShadows
                .filter { it.isDeployed }
                .take(3)

            val activeShadowsList = deployedShadows.map { shadow ->
                ActiveBattleShadow(
                    id = shadow.id,
                    name = shadow.name,
                    title = shadow.title,
                    rank = shadow.rank,
                    iconEmoji = shadow.iconEmoji,
                    currentHp = shadow.maxHp,
                    maxHp = shadow.maxHp,
                    attackPower = shadow.attackPower,
                    defense = shadow.defense,
                    signatureSkill = shadow.signatureSkill,
                    mpReconstituteCost = shadow.mpUpkeepCost,
                    isAlive = true,
                    originRank = shadow.originRank
                )
            }

            val initialLogs = mutableListOf(
                "⚔️ Raid commenced! ${boss.name} (${boss.rank}) emerges with murderous intent!",
                "Boss Weakness: ${boss.weakness}. Watch out for '${boss.bossSpecialAttackName}'!"
            )

            if (activeShadowsList.isNotEmpty()) {
                initialLogs.add("👑 SHADOW SQUADRON DEPLOYED: [${activeShadowsList.joinToString { it.name }}] emerge from the dark mist to fight at your command!")
            } else {
                initialLogs.add("ℹ️ No shadows deployed. Slay this boss to extract its soul with 'ARISE'!")
            }

            val equippedGear = db.equipmentDao().getEquippedItemsOnce()
            val gearHpBonus = equippedGear.sumOf { it.hpBonus }
            val gearMpBonus = equippedGear.sumOf { it.mpBonus }

            val maxHp = (p?.maxHp ?: 120) + gearHpBonus
            val maxMp = (p?.maxMp ?: 60) + gearMpBonus
            val currentHp = ((p?.hp ?: maxHp) + gearHpBonus).coerceAtMost(maxHp)
            val currentMp = ((p?.mp ?: maxMp) + gearMpBonus).coerceAtMost(maxMp)

            _battleState.value = BattleState(
                inBattle = true,
                currentBoss = boss,
                bossCurrentHp = boss.maxHp,
                playerCurrentHp = currentHp,
                playerMaxHp = maxHp,
                playerCurrentMp = currentMp,
                playerMaxMp = maxMp,
                activeShadows = activeShadowsList,
                logMessages = initialLogs,
                isVictory = false,
                isDefeat = false,
                extractionEligible = false,
                isExtracted = false,
                isBossEnraged = false,
                isBossChargingUltimate = false,
                isPlayerDefending = false,
                healthPotionsRemaining = 2
            )
        }
    }

    /**
     * Manually reconstitutes a fallen shadow in battle by spending Hunter's MP.
     */
    fun reconstituteShadowInBattle(shadowId: Long) {
        val current = _battleState.value
        if (!current.inBattle || current.isVictory || current.isDefeat) return

        val shadow = current.activeShadows.find { it.id == shadowId } ?: return
        if (shadow.isAlive) return

        if (current.playerCurrentMp < shadow.mpReconstituteCost) {
            _battleState.value = current.copy(
                logMessages = current.logMessages + "⚠️ Insufficient MP! Needs ${shadow.mpReconstituteCost} MP to reconstitute ${shadow.name}."
            )
            return
        }

        val newMp = current.playerCurrentMp - shadow.mpReconstituteCost
        val updatedShadows = current.activeShadows.map {
            if (it.id == shadowId) it.copy(currentHp = it.maxHp, isAlive = true) else it
        }

        _battleState.value = current.copy(
            playerCurrentMp = newMp,
            activeShadows = updatedShadows,
            logMessages = current.logMessages + "🌑 RECONSTITUTION: Hunter channeled ${shadow.mpReconstituteCost} MP! [${shadow.name}] rises from the abyss with full HP!"
        )
    }

    fun executePlayerAttack(actionType: String) {
        val current = _battleState.value
        if (!current.inBattle || current.isVictory || current.isDefeat) return

        viewModelScope.launch {
            val profile = playerProfile.value ?: withContext(Dispatchers.IO) { db.playerDao().getPlayerProfileOnce() } ?: return@launch
            val boss = current.currentBoss ?: return@launch

            var damageDealt = 0
            var mpCost = 0
            var playerDefendingThisTurn = false
            var potionsLeft = current.healthPotionsRemaining
            var playerHpAfterHeal = current.playerCurrentHp
            var playerMpAfterAction = current.playerCurrentMp

            val actionLogs = mutableListOf<String>()
            val shadowList = current.activeShadows.toMutableList()

            val equippedGear = equipment.value.ifEmpty { withContext(Dispatchers.IO) { db.equipmentDao().getEquippedItemsOnce() } }.filter { it.isEquipped }
            val gearAtkBonus = equippedGear.sumOf { it.attackBonus }
            val gearDefBonus = equippedGear.sumOf { it.defenseBonus }

        when (actionType) {
            "HEAL" -> {
                if (potionsLeft <= 0) {
                    _battleState.value = current.copy(
                        logMessages = current.logMessages + "⚠️ No Elixirs of Life remaining!"
                    )
                    return@launch
                }
                potionsLeft -= 1
                val healAmount = 60
                playerHpAfterHeal = (playerHpAfterHeal + healAmount).coerceAtMost(profile.maxHp)
                actionLogs.add("🧪 Hunter drinks Elixir of Life! Restores +$healAmount HP! ($potionsLeft potions remaining)")
            }
            "DEFEND" -> {
                playerDefendingThisTurn = true
                playerMpAfterAction = (playerMpAfterAction + 15).coerceAtMost(profile.maxMp)
                actionLogs.add("🛡️ Hunter assumes Steel Parry Stance! +15 MP restored. Incoming damage reduced by 70%!")
            }
            "TONIC" -> {
                playerMpAfterAction = (playerMpAfterAction + 30).coerceAtMost(profile.maxMp)
                actionLogs.add("🧪 Hunter drinks Mana Tonic! +30 MP restored!")
            }
            "BASIC" -> {
                damageDealt = ((profile.strength * 2.4 + profile.agility * 1.0 + gearAtkBonus * 1.2) - boss.defense * 0.4).toInt().coerceAtLeast(18)
                actionLogs.add("Hunter strikes with Physical Precision for $damageDealt damage!${if (gearAtkBonus > 0) " (+${gearAtkBonus} Gear ATK)" else ""}")
            }
            "CLASS_SKILL" -> {
                mpCost = 15
                if (playerMpAfterAction < mpCost) {
                    _battleState.value = current.copy(
                        logMessages = current.logMessages + "⚠️ Not enough MP for Class Skill (Needs 15 MP)!"
                    )
                    return@launch
                }
                playerMpAfterAction -= mpCost
                damageDealt = ((profile.strength * 3.4 + profile.agility * 2.2 + gearAtkBonus * 1.8) - boss.defense * 0.5).toInt().coerceAtLeast(35)
                actionLogs.add("⚡ Hunter unleashes Class Vanguard Technique for $damageDealt critical damage!")
            }
            "SHADOW_SUMMON" -> {
                mpCost = 25
                if (playerMpAfterAction < mpCost) {
                    _battleState.value = current.copy(
                        logMessages = current.logMessages + "⚠️ Not enough MP to command Shadows (Needs 25 MP)!"
                    )
                    return@launch
                }
                playerMpAfterAction -= mpCost

                // Overcharged shadow assault + reconstitute 1 fallen shadow if any
                val deadShadow = shadowList.indexOfFirst { !it.isAlive }
                if (deadShadow != -1) {
                    val revived = shadowList[deadShadow].copy(currentHp = shadowList[deadShadow].maxHp, isAlive = true)
                    shadowList[deadShadow] = revived
                    actionLogs.add("🌑 SOVEREIGN RECALL: [${revived.name}] was summoned back to life from the shadow realm!")
                }

                val aliveShadowPower = shadowList.filter { it.isAlive }.sumOf { it.attackPower }
                damageDealt = (aliveShadowPower * 1.6 + profile.intelligence * 2.2 + gearAtkBonus * 1.5 - boss.defense * 0.3).toInt().coerceAtLeast(50)
                actionLogs.add("👑 SHADOW MONARCH OVERLOAD: Shadows converge into dark astral vortex dealing $damageDealt catastrophic damage!")
            }
            "ULTIMATE" -> {
                mpCost = 40
                if (playerMpAfterAction < mpCost) {
                    _battleState.value = current.copy(
                        logMessages = current.logMessages + "⚠️ Not enough MP for Monarch Wrath (Needs 40 MP)!"
                    )
                    return@launch
                }
                playerMpAfterAction -= mpCost
                damageDealt = ((profile.strength * 5.2 + profile.intelligence * 3.8 + gearAtkBonus * 2.5) - boss.defense * 0.4).toInt().coerceAtLeast(75)
                actionLogs.add("💥 MONARCH'S WRATH: Dark astral shockwaves annihilate the battlefield for $damageDealt damage!")
            }
        }

        // --- SHADOW SQUADRON ATTACK TURN WITH SYNERGY & MONARCH BOOST ---
        val aliveShadows = shadowList.filter { it.isAlive }
        val deployedCount = aliveShadows.size
        val isMonarch = profile.selectedClass.contains("Monarch", ignoreCase = true) || profile.rank.contains("Monarch", ignoreCase = true)
        val totalSynergyPercent = computeSynergyPercent(isMonarch, deployedCount)
        val synergyMultiplier = 1.0f + (totalSynergyPercent / 100f)

        var totalShadowDmg = 0
        aliveShadows.forEach { shadow ->
            val isSkill = (1..100).random() <= 40
            val baseDmg = if (isSkill) {
                ((shadow.attackPower * 1.6f) - boss.defense * 0.25f).toInt().coerceAtLeast(18)
            } else {
                ((shadow.attackPower * 1.15f) - boss.defense * 0.3f).toInt().coerceAtLeast(12)
            }
            val shadowDmg = (baseDmg * synergyMultiplier).toInt()
            totalShadowDmg += shadowDmg
            if (isSkill) {
                actionLogs.add("👥 [${shadow.name}] executes '${shadow.signatureSkill}' for $shadowDmg damage! ${if (totalSynergyPercent > 0) "(+${totalSynergyPercent}% Synergy Boost)" else ""}")
            } else {
                actionLogs.add("👥 [${shadow.name}] strikes with dark shadow blade for $shadowDmg damage!")
            }
        }

        val totalCombinedDamage = damageDealt + totalShadowDmg
        val newBossHp = (current.bossCurrentHp - totalCombinedDamage).coerceAtLeast(0)

        // Check Boss Defeat
        if (newBossHp <= 0) {
            val victoryLogs = current.logMessages + actionLogs +
                    "${boss.name} has fallen! A dark aura gathers... Speak 'ARISE' to extract its soul!"
            _battleState.value = current.copy(
                bossCurrentHp = 0,
                playerCurrentHp = playerHpAfterHeal,
                playerCurrentMp = playerMpAfterAction,
                activeShadows = shadowList,
                logMessages = victoryLogs,
                isVictory = true,
                extractionEligible = true,
                healthPotionsRemaining = potionsLeft
            )
            rewardDungeonVictory(boss)
            return@launch
        }

        // Check Boss Enrage Trigger (< 35% HP)
        var isEnraged = current.isBossEnraged
        if (newBossHp <= (boss.maxHp * 0.35f) && !isEnraged) {
            isEnraged = true
            actionLogs.add("⚠️ CRITICAL WARNING: ${boss.name} has ENTERED ENRAGED PHASE! (+35% ATK)!")
        }

        // --- BOSS COMBAT AI TURN ---
        val atkMultiplier = if (isEnraged) 1.35f else 1.0f
        var willChargeNext = false

        // 60% chance boss attacks an active shadow (drawing aggro), 40% chance it targets the hunter
        val targetShadowIndex = if (aliveShadows.isNotEmpty() && (1..100).random() <= 60 && !current.isBossChargingUltimate) {
            val chosen = aliveShadows.random()
            shadowList.indexOfFirst { it.id == chosen.id }
        } else {
            -1
        }

        if (targetShadowIndex != -1) {
            // Boss attacks Shadow
            val target = shadowList[targetShadowIndex]
            val bossDmgToShadow = (boss.attack * atkMultiplier * 1.15f - target.defense * 0.35f).toInt().coerceAtLeast(16)
            
            // N2: Delegate fatal blow & passive MP reconstitution to tested helper
            val reconResult = processShadowDamageAndReconstitution(target, bossDmgToShadow, playerMpAfterAction)
            shadowList[targetShadowIndex] = reconResult.updatedShadow
            playerMpAfterAction -= reconResult.consumedMp

            val useReviveCharm = (consumables.value.find { it.itemId == "cons_revive" }?.count ?: 0) > 0
            if (reconResult.didReconstitute) {
                actionLogs.add("💥 ${boss.name} delivered a fatal strike of $bossDmgToShadow damage to [${target.name}]!")
                actionLogs.add("🌑 PASSIVE RECONSTITUTION: [${target.name}] consumed ${reconResult.consumedMp} MP from Hunter and immediately regenerated from the dark mist with full HP! 'ARISE!'")
            } else if (!reconResult.updatedShadow.isAlive) {
                if (useReviveCharm) {
                    shadowList[targetShadowIndex] = target.copy(currentHp = target.maxHp, isAlive = true)
                    viewModelScope.launch { consumeOne("cons_revive") }
                    actionLogs.add("💎 REVIVE CHARM: [${target.name}] shatters the charm and rises fully restored!")
                } else {
                    actionLogs.add("💀 [${target.name}] was destroyed by $bossDmgToShadow damage! Hunter lacked ${target.mpReconstituteCost} MP to passively reconstitute it! Shadow is now dormant.")
                }
            } else {
                actionLogs.add("🛡️ [${target.name}] tanks ${boss.name}'s blow, taking $bossDmgToShadow damage! (${reconResult.updatedShadow.currentHp}/${target.maxHp} HP remaining)")
            }
        } else {
            // Boss attacks Hunter directly
            var rawBossDmg = 0
            if (current.isBossChargingUltimate) {
                rawBossDmg = ((boss.attack * atkMultiplier * 2.2f) - (profile.endurance * 0.5f) - gearDefBonus).toInt().coerceAtLeast(20)
                actionLogs.add("🔴 CATACLYSM: ${boss.name} unleashes '${boss.bossSpecialAttackName}' directly upon Hunter!")
            } else {
                val roll = (1..100).random()
                if (roll <= 25 && !isEnraged) {
                    willChargeNext = true
                    rawBossDmg = ((boss.attack * atkMultiplier * 0.6f) - (gearDefBonus * 0.5f)).toInt().coerceAtLeast(6)
                    actionLogs.add("⚠️ ALERT: ${boss.name} gathers catastrophic energy for '${boss.bossSpecialAttackName}'! [PARRY/DEFEND] next turn!")
                } else {
                    rawBossDmg = ((boss.attack * atkMultiplier * 1.25f) - (profile.endurance * 0.45f) - gearDefBonus).toInt().coerceAtLeast(10)
                    actionLogs.add("${boss.name} strikes Hunter with ferocious assault for $rawBossDmg damage!")
                }
            }

            val finalBossDmg = if (playerDefendingThisTurn) {
                val reduced = (rawBossDmg * 0.30f).toInt().coerceAtLeast(4)
                actionLogs.add("🛡️ PARRY EFFECTIVE! Absorbed 70% damage, taking only $reduced damage!")
                reduced
            } else {
                rawBossDmg
            }

            playerHpAfterHeal = (playerHpAfterHeal - finalBossDmg).coerceAtLeast(0)
        }

        if (playerHpAfterHeal <= 0) {
            actionLogs.add("💀 Hunter was incapacitated by ${boss.name}! Gate collapse imminent!")
            _battleState.value = current.copy(
                bossCurrentHp = newBossHp,
                playerCurrentHp = 0,
                playerCurrentMp = playerMpAfterAction,
                activeShadows = shadowList,
                logMessages = current.logMessages + actionLogs,
                isDefeat = true,
                isBossEnraged = isEnraged,
                isBossChargingUltimate = false,
                isPlayerDefending = false,
                healthPotionsRemaining = potionsLeft
            )
        } else {
            _battleState.value = current.copy(
                bossCurrentHp = newBossHp,
                playerCurrentHp = playerHpAfterHeal,
                playerCurrentMp = playerMpAfterAction,
                activeShadows = shadowList,
                logMessages = current.logMessages + actionLogs,
                isBossEnraged = isEnraged,
                isBossChargingUltimate = willChargeNext,
                isPlayerDefending = playerDefendingThisTurn,
                healthPotionsRemaining = potionsLeft
            )
        }
        }
    }

    fun useShopElixir() = viewModelScope.launch(Dispatchers.IO) {
        val current = _battleState.value
        if (!current.inBattle || current.isVictory || current.isDefeat) return@launch
        if (db.consumableDao().decrement("cons_elixir") <= 0) return@launch

        val healAmount = 60
        val newHp = (current.playerCurrentHp + healAmount).coerceAtMost(current.playerMaxHp)
        _battleState.value = current.copy(
            playerCurrentHp = newHp,
            logMessages = current.logMessages + "🧪 Shop Elixir consumed! Restored +$healAmount HP."
        )
    }

    fun useShopTonic() = viewModelScope.launch(Dispatchers.IO) {
        val current = _battleState.value
        if (!current.inBattle || current.isVictory || current.isDefeat) return@launch
        if (db.consumableDao().decrement("cons_tonic") <= 0) return@launch

        val restoreAmount = 30
        val newMp = (current.playerCurrentMp + restoreAmount).coerceAtMost(current.playerMaxMp)
        _battleState.value = current.copy(
            playerCurrentMp = newMp,
            logMessages = current.logMessages + "🧪 Shop Mana Tonic consumed! Restored +$restoreAmount MP."
        )
    }

    /**
     * Executes the iconic "ARISE" extraction command with dynamic hunter scaling.
     */
    fun performAriseExtraction() {
        val state = _battleState.value
        val boss = state.currentBoss ?: return
        if (!state.extractionEligible || state.isExtracted) return

        viewModelScope.launch(Dispatchers.IO) {
            extractionMutex.withLock {
                val fresh = _battleState.value
                val currentBoss = fresh.currentBoss ?: return@withLock
                if (!fresh.extractionEligible || fresh.isExtracted) return@withLock

                // Optimistically mark extracted
                _battleState.value = fresh.copy(isExtracted = true)

                try {
                    var unitName = currentBoss.shadowUnitName
                    db.withTransaction {
                        val p = db.playerDao().getPlayerProfileOnce()
                        val hunterLevel = p?.level ?: 1
                        val hunterMaxHp = p?.maxHp ?: 120
                        val hunterStr = p?.strength ?: 10
                        val hunterEnd = p?.endurance ?: 10

                        val scaled = calculateScaledShadowStats(currentBoss, hunterMaxHp, hunterStr, hunterEnd)
                        unitName = currentBoss.shadowUnitName

                        val newShadow = ShadowUnit(
                            name = currentBoss.shadowUnitName,
                            title = currentBoss.shadowUnitTitle,
                            rank = currentBoss.shadowRank,
                            level = hunterLevel,
                            maxHp = scaled.maxHp,
                            currentHp = scaled.maxHp,
                            attackPower = scaled.attack,
                            defense = scaled.defense,
                            speed = (20 * scaled.weight).toInt(),
                            loyalty = 100,
                            signatureSkill = currentBoss.shadowSignatureSkill,
                            skillDescription = currentBoss.shadowSkillDesc,
                            isSummoned = true,
                            isDeployed = true,
                            originRank = currentBoss.rank,
                            mpUpkeepCost = scaled.mpReconstituteCost,
                            iconEmoji = currentBoss.iconEmoji,
                            lore = "Defeated in battle. Extracted via 'ARISE'. Scaled to Hunter Rank (${p?.rank ?: "E-Rank"}) from origin ${currentBoss.rank}. Loyally serves in the Undying Legion."
                        )
                        db.shadowDao().insertShadow(newShadow)

                        if (p != null) {
                            db.playerDao().updateProfile(
                                p.copy(
                                    shadowArmyCount = p.shadowArmyCount + 1,
                                    manaCrystals = p.manaCrystals + 5
                                )
                            )
                        }
                    }

                    _battleState.value = _battleState.value.copy(
                        logMessages = _battleState.value.logMessages +
                                "COMMAND UTTERED: 'ARISE!'" +
                                "Shadow Extraction SUCCESSFUL! $unitName has joined your Shadow Army!"
                    )
                    _celebrationEvent.value = "SHADOW EXTRACTION: 'ARISE!' $unitName joined your Legion!"
                    viewModelScope.launch(Dispatchers.IO) {
                        milestoneMutex.withLock {
                            if (!db.milestoneDao().hasFired("first_arise")) {
                                val tpl = com.example.data.MilestoneOffers.byKey["first_arise"]
                                if (tpl != null) {
                                    val now = System.currentTimeMillis()
                                    db.milestoneDao().log(com.example.data.model.MilestoneLog("first_arise", now))
                                    db.pendingOfferDao().insert(
                                        com.example.data.model.PendingOffer(
                                            milestoneKey = "first_arise",
                                            title = tpl.title, description = tpl.description,
                                            goldCost = tpl.goldCost, crystalCost = tpl.crystalCost,
                                            rewardType = tpl.rewardType, rewardPayload = tpl.payload,
                                            generatedAt = now, expiresAt = now + com.example.data.model.PendingOffer.WINDOW_MS
                                        )
                                    )
                                }
                            }
                            checkMilestones()
                        }
                    }
                } catch (e: Exception) {
                    _battleState.value = _battleState.value.copy(isExtracted = false)
                    _celebrationEvent.value = "Extraction failed: ${e.message}"
                }
            }
        }
    }

    /**
     * Toggles whether a shadow unit is deployed into the active raid squadron (max 3).
     */
    fun toggleDeployShadow(shadowId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.withTransaction {
                val shadow = db.shadowDao().getShadowById(shadowId) ?: return@withTransaction
                val currentlyDeployed = db.shadowDao().getAllShadowsOnce()
                    .filter { it.isDeployed }
                    .sortedBy { it.extractionDate } // Deterministic: oldest first

                if (!shadow.isDeployed) {
                    if (currentlyDeployed.size >= 3) {
                        val oldest = currentlyDeployed.first()
                        db.shadowDao().updateShadow(oldest.copy(isDeployed = false))
                        _celebrationEvent.value = "SQUADRON REASSIGNMENT: ${oldest.name} returned to reserves. ${shadow.name} deployed to Vanguard!"
                    } else {
                        _celebrationEvent.value = "SQUADRON DEPLOYMENT: ${shadow.name} deployed to Vanguard!"
                    }
                    db.shadowDao().updateShadow(shadow.copy(isDeployed = true))
                } else {
                    db.shadowDao().updateShadow(shadow.copy(isDeployed = false))
                    _celebrationEvent.value = "${shadow.name} recalled from active Vanguard to reserves."
                }
            }
        }
    }

    /**
     * Upgrades a shadow's rank and combat capabilities using Mana Crystals or Gold.
     */
    fun upgradeShadow(shadowId: Long, useManaCrystals: Boolean? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = db.playerDao().getPlayerProfileOnce() ?: return@launch
            val shadow = db.shadowDao().getShadowById(shadowId) ?: return@launch

            val spendCrystals = when (useManaCrystals) {
                true -> {
                    if (p.manaCrystals < 2) {
                        _celebrationEvent.value = "Insufficient Mana Crystals! Upgrade requires 2 💎."
                        return@launch
                    }
                    true
                }
                false -> {
                    if (p.gold < 150) {
                        _celebrationEvent.value = "Insufficient Gold! Upgrade requires 150 🪙."
                        return@launch
                    }
                    false
                }
                null -> {
                    if (p.manaCrystals >= 2) true
                    else if (p.gold >= 150) false
                    else {
                        _celebrationEvent.value = "Insufficient resources! Upgrade requires 2 Mana Crystals or 150 Gold."
                        return@launch
                    }
                }
            }

            val newCrystals = if (spendCrystals) p.manaCrystals - 2 else p.manaCrystals
            val newGold = if (!spendCrystals) p.gold - 150 else p.gold

            val newLevel = shadow.level + 1
            val newReqXp = maxOf((shadow.requiredXp * 1.35f).toInt(), shadow.requiredXp + 1)
            val newMaxHp = (shadow.maxHp * 1.12f).toInt()
            val newAtk = (shadow.attackPower * 1.10f).toInt()
            val newDef = (shadow.defense * 1.08f).toInt()

            db.withTransaction {
                db.playerDao().updateProfile(p.copy(manaCrystals = newCrystals, gold = newGold))
                db.shadowDao().updateShadow(
                    shadow.copy(
                        level = newLevel,
                        currentXp = 0,
                        requiredXp = newReqXp,
                        maxHp = newMaxHp,
                        currentHp = newMaxHp,
                        attackPower = newAtk,
                        defense = newDef
                    )
                )
            }

            val paidDesc = if (spendCrystals) "2 💎" else "150 🪙"
            _celebrationEvent.value = "SHADOW ASCENSION: ${shadow.name} reached Level $newLevel! (Paid $paidDesc, +12% HP, +10% ATK)"
        }
    }

    private fun rewardDungeonVictory(boss: DungeonBoss) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = db.playerDao().getPlayerProfileOnce() ?: return@launch

            // Daily first-clear: full reward only once per boss per calendar day.
            // Repeat kills the same day give a small consolation reward to prevent infinite farming.
            val todayCode = com.example.data.model.VowState.dateCode()
            val dailyKey = "boss_clear_${boss.id}_$todayCode"
            val isFirstClearToday = !db.milestoneDao().hasFired(dailyKey)
            if (isFirstClearToday) {
                db.milestoneDao().log(com.example.data.model.MilestoneLog(dailyKey, System.currentTimeMillis()))
            }

            val xpGain = if (isFirstClearToday) boss.maxHp / 3 else boss.maxHp / 20
            val goldGain = if (isFirstClearToday) boss.attack * 4 else boss.attack / 2

            var newXp = p.currentXp + xpGain
            var newLevel = p.level
            var newReqXp = p.requiredXp
            var newStatPoints = p.unallocatedStatPoints
            var didLevelUp = false

            while (newXp >= newReqXp) {
                newXp -= newReqXp
                newLevel += 1
                newReqXp = maxOf((newReqXp * 1.35).toInt(), newReqXp + 1).coerceAtMost(Int.MAX_VALUE / 2)
                newStatPoints += 3
                didLevelUp = true
            }

            db.playerDao().updateProfile(
                p.copy(
                    level = newLevel,
                    currentXp = newXp,
                    requiredXp = newReqXp,
                    gold = p.gold + goldGain,
                    unallocatedStatPoints = newStatPoints,
                    totalWorkouts = p.totalWorkouts + 1,
                    rank = determineRank(newLevel)
                )
            )

            // Award combat XP to deployed shadows
            val allShadows = db.shadowDao().getAllShadowsOnce()
            val deployed = allShadows.filter { it.isDeployed }
            val sXpGain = if (isFirstClearToday) {
                (boss.maxHp * 0.20f).toInt().coerceAtLeast(30)
            } else {
                (boss.maxHp * 0.03f).toInt().coerceAtLeast(5)
            }
            deployed.forEach { s ->
                var currXp = s.currentXp + sXpGain
                var sLevel = s.level
                var sReq = s.requiredXp
                var sMaxHp = s.maxHp
                var sAtk = s.attackPower
                var sDef = s.defense
                while (currXp >= sReq) {
                    currXp -= sReq
                    sLevel += 1
                    sReq = maxOf((sReq * 1.35f).toInt(), sReq + 1).coerceAtMost(Int.MAX_VALUE / 2)
                    sMaxHp = (sMaxHp * 1.10f).toInt()
                    sAtk = (sAtk * 1.08f).toInt()
                    sDef = (sDef * 1.06f).toInt()
                }
                db.shadowDao().updateShadow(
                    s.copy(
                        level = sLevel,
                        currentXp = currXp,
                        requiredXp = sReq,
                        maxHp = sMaxHp,
                        currentHp = sMaxHp,
                        attackPower = sAtk,
                        defense = sDef
                    )
                )
            }

            if (didLevelUp) {
                _celebrationEvent.value = "DUNGEON CLEARED & LEVEL UP! Hunter reached Level $newLevel!"
            } else if (!isFirstClearToday) {
                _celebrationEvent.value = "Dungeon cleared again. +$xpGain XP, +$goldGain Gold (daily first-clear bonus for ${boss.name} already used)."
            }

            viewModelScope.launch(Dispatchers.IO) {
                milestoneMutex.withLock {
                    if (!db.milestoneDao().hasFired("first_boss")) {
                        val tpl = com.example.data.MilestoneOffers.byKey["first_boss"]
                        if (tpl != null) {
                            val now = System.currentTimeMillis()
                            db.milestoneDao().log(com.example.data.model.MilestoneLog("first_boss", now))
                            db.pendingOfferDao().insert(
                                com.example.data.model.PendingOffer(
                                    milestoneKey = "first_boss",
                                    title = tpl.title, description = tpl.description,
                                    goldCost = tpl.goldCost, crystalCost = tpl.crystalCost,
                                    rewardType = tpl.rewardType, rewardPayload = tpl.payload,
                                    generatedAt = now, expiresAt = now + com.example.data.model.PendingOffer.WINDOW_MS
                                )
                            )
                        }
                    }
                    checkMilestones()
                }
            }
        }
    }

    fun endBattle() {
        val state = _battleState.value
        if (!state.inBattle) {
            _battleState.value = BattleState()
            return
        }
        val playerHp = state.playerCurrentHp
        val playerMp = state.playerCurrentMp
        val isDefeat = state.isDefeat
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val p = db.playerDao().getPlayerProfileOnce()
                if (p != null) {
                    val endHp = if (isDefeat) 1.coerceAtMost(p.maxHp) else playerHp.coerceIn(1, p.maxHp)
                    val endMp = playerMp.coerceIn(0, p.maxMp)
                    db.playerDao().updateProfile(p.copy(hp = endHp, mp = endMp))
                }
            }
            // Reset battle state only AFTER database profile write completes to prevent race conditions
            _battleState.value = BattleState()
        }
    }

    // ----------------------------------------------------
    // EXPLORATION & STEP / FOOT TRACKING
    // ----------------------------------------------------

    fun toggleExplorationTracking() {
        if (radarManager.isTracking.value) {
            radarManager.stopTracking()
        } else {
            radarManager.startTracking()
        }
    }

    fun addWalkedFeet(feet: Float) {
        // C1: Single source of truth — radarManager updates sessionSteps & sessionMeters which are persisted by init collectors
        radarManager.addWalkedFeet(feet)
    }

    fun addSimulatedDistance(meters: Float) {
        addWalkedFeet(meters * HunterRadarManager.FEET_PER_METER)
    }

    fun selectRadarGate(gate: SpawnedGate?) {
        radarManager.selectGate(gate)
    }

    fun respawnNearbyGates() {
        radarManager.refreshSpawnedGates()
    }

    fun advanceTowardsSelectedGate(feet: Float) {
        addWalkedFeet(feet)
    }

    fun toggleRadarEngine() {
        radarManager.toggleRadarEngine()
    }

    fun setRadarEngine(engine: com.example.data.RadarEngine) {
        radarManager.setRadarEngine(engine)
    }

    fun setRadarRange(rangeFeet: Float) {
        radarManager.setRadarRange(rangeFeet)
    }

    fun advanceTowardsGate(gate: SpawnedGate, feet: Float) {
        radarManager.selectGate(gate)
        addWalkedFeet(feet)
    }

    fun raidSpawnedGate(gate: SpawnedGate, onStartBattle: () -> Unit) {
        radarManager.clearGate(gate.id)
        startBossBattle(gate.boss)
        onStartBattle()
    }

    // ----------------------------------------------------
    // MULTIPLAYER SQUADRON & CO-OP RAIDS
    // ----------------------------------------------------

    fun createMultiplayerParty(name: String, boss: DungeonBoss) {
        val p = playerProfile.value ?: return
        multiplayerManager.createParty(name, boss, 4, p)
    }

    fun quickMatchMultiplayer(boss: DungeonBoss) {
        val p = playerProfile.value ?: return
        multiplayerManager.quickMatch(p, boss)
    }

    fun joinDiscoveredParty(discovered: DiscoveredParty) {
        val p = playerProfile.value ?: return
        multiplayerManager.joinParty(discovered, p)
    }

    fun joinPartyByCode(code: String, onResult: (JoinResult) -> Unit = {}) {
        val p = playerProfile.value ?: return
        val result = multiplayerManager.joinByCode(code, p)
        onResult(result)
    }

    fun leaveMultiplayerParty() {
        multiplayerManager.leaveParty()
    }

    fun startCoopRaid(forceSolo: Boolean = false): Boolean {
        return multiplayerManager.startCoopRaid(forceSolo)
    }

    // m2: Expose multiplayer listener via ViewModel
    fun refreshLanParties() {
        multiplayerManager.startListening()
    }

    fun performCoopAttack(skillType: String) {
        viewModelScope.launch {
            val p = db.playerDao().getPlayerProfileOnce() ?: playerProfile.value ?: return@launch
            multiplayerManager.performPartyCombatTurn(p, skillType) { xp, gold, crystals ->
                viewModelScope.launch(Dispatchers.IO) {
                    var didLevelUp = false
                    var newLevel = 1
                    db.withTransaction {
                        val prof = db.playerDao().getPlayerProfileOnce() ?: return@withTransaction
                        var newXp = prof.currentXp + xp
                        newLevel = prof.level
                        var newReq = prof.requiredXp
                        var newStatPoints = prof.unallocatedStatPoints

                        while (newXp >= newReq) {
                            newXp -= newReq
                            newLevel += 1
                            newReq = (newReq * 1.35).toInt().coerceAtMost(Int.MAX_VALUE / 2)
                            newStatPoints += 3
                            didLevelUp = true
                        }

                        db.playerDao().updateProfile(
                            prof.copy(
                                level = newLevel,
                                currentXp = newXp,
                                requiredXp = newReq,
                                unallocatedStatPoints = newStatPoints,
                                gold = prof.gold + gold,
                                manaCrystals = prof.manaCrystals + crystals
                            )
                        )

                        _celebrationEvent.value = if (didLevelUp) {
                            "CO-OP RAID CLEARED & LEVEL UP! Hunter reached Level $newLevel! (+$gold Gold, +$crystals Crystals)"
                        } else {
                            "CO-OP RAID CLEARED! +$xp XP, +$gold Gold, +$crystals Crystals awarded to squadron!"
                        }
                    }
                    if (didLevelUp) {
                        checkMilestonesAfterLevelUp()
                    }
                }
            }
        }
    }

    fun sendPartyEmote(message: String) = viewModelScope.launch(Dispatchers.IO) {
        val p = db.playerDao().getPlayerProfileOnce() ?: return@launch
        multiplayerManager.sendPartyEmote(p.name, message)
    }

    // ----------------------------------------------------
    // NYX AI CHAT COMPANION
    // ----------------------------------------------------

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            // Save user message
            db.chatDao().insertMessage(
                ChatMessage(sender = "USER", content = userText)
            )

            _isNyxReplying.value = true

            val profile = db.playerDao().getPlayerProfileOnce()
            val contextSummary = "Hunter Level: ${profile?.level ?: 1}, Class: ${profile?.selectedClass ?: "Warrior"}, Rank: ${profile?.rank ?: "E-Rank"}, STR: ${profile?.strength ?: 10}, INT: ${profile?.intelligence ?: 10}, Quests Done: ${profile?.totalQuestsCompleted ?: 0}, Shadows in Legion: ${profile?.shadowArmyCount ?: 0}"

            val result = geminiService.chatWithNyx(userText, contextSummary)

            _isNyxReplying.value = false

            val reply = if (result.isSuccess) {
                result.getOrNull() ?: "The System confirms your message, Hunter."
            } else {
                "System Notice: [Offline Mode Active] ${result.exceptionOrNull()?.message ?: "Connect your Gemini API Key in Settings to unlock real-time neural responses."}"
            }

            db.chatDao().insertMessage(
                ChatMessage(sender = "NYX", content = reply)
            )
        }
    }

    fun generateAiDailyQuest(interest: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = db.playerDao().getPlayerProfileOnce()
            val res = geminiService.generateDynamicQuest(
                preference = interest,
                playerClass = p?.selectedClass ?: "Warrior",
                playerLevel = p?.level ?: 1
            )
            val quest = res.getOrNull()
            if (quest != null) {
                db.questDao().insertQuest(quest)
                db.chatDao().insertMessage(
                    ChatMessage(
                        sender = "NYX",
                        content = "A new personalized quest has materialized in your Quest Log: '${quest.title}' (+${quest.xpReward} XP). Conquer it with absolute dedication."
                    )
                )
            }
        }
    }

    // ----------------------------------------------------
    // TASK MANAGEMENT
    // ----------------------------------------------------

    fun addCustomTask(title: String, note: String, stat: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.taskDao().insertTask(
                TaskItem(
                    title = title,
                    notes = note,
                    targetStat = stat
                )
            )
        }
    }

    fun completeTask(task: TaskItem) {
        viewModelScope.launch(Dispatchers.IO) {
            var didLevelUp = false
            var newLevel = 1
            db.withTransaction {
                db.taskDao().markTaskCompleted(task.id)
                val p = db.playerDao().getPlayerProfileOnce() ?: return@withTransaction

                var newXp = p.currentXp + task.xpReward
                newLevel = p.level
                var newReq = p.requiredXp
                var newPts = p.unallocatedStatPoints
                while (newXp >= newReq) {
                    newXp -= newReq
                    newLevel++
                    newReq = maxOf((newReq * 1.35).toInt(), newReq + 1).coerceAtMost(Int.MAX_VALUE / 2)
                    newPts += 3
                    didLevelUp = true
                }

                db.playerDao().updateProfile(
                    p.copy(
                        level = newLevel,
                        currentXp = newXp,
                        requiredXp = newReq,
                        unallocatedStatPoints = newPts,
                        gold = p.gold + task.goldReward,
                        rank = determineRank(newLevel)
                    )
                )
            }
            if (didLevelUp) {
                _celebrationEvent.value = "LEVEL UP! Hunter reached Level $newLevel!"
                checkMilestonesAfterLevelUp()
            }
        }
    }

    // ----------------------------------------------------
    // VOW OF DISCIPLINE (48H COVENANT)
    // ----------------------------------------------------

    private suspend fun processMidnightVowCheck() = db.withTransaction {
        val vow = db.vowDao().getVowStateOnce() ?: return@withTransaction
        if (!vow.isActive) return@withTransaction

        val now = System.currentTimeMillis()
        if (vow.lastResetAt > now) {
            db.vowDao().upsert(vow.copy(lastResetAt = now))
            return@withTransaction
        }

        val today = VowState.dateCode()
        if (today == vow.lastRewardDate) return@withTransaction

        if (vow.isTimerExpired(now)) {
            val p = db.playerDao().getPlayerProfileOnce()
            if (p != null && (p.currentXp > 0 || p.gold > 0)) {
                // Tithe is taken only from CURRENT progress (XP toward next level, unspent gold).
                // Never removes levels, allocated stats, gear, crystals, or the Shadow Army.
                val xpTithe = (p.currentXp * 0.10f).toInt()
                val goldTithe = (p.gold * 0.10f).toInt()
                db.playerDao().updateProfile(
                    p.copy(
                        currentXp = (p.currentXp - xpTithe).coerceAtLeast(0),
                        gold = (p.gold - goldTithe).coerceAtLeast(0)
                    )
                )
                db.vowDao().upsert(vow.copy(currentStreak = 0, lastRewardDate = today))
                _celebrationEvent.value =
                    "⚠️ VOW LAPSED. The System claims its tithe: -$xpTithe XP (current level only), -$goldTithe Gold.\nStreak reset to 0. Take the Vow again to rebuild."
            } else {
                db.vowDao().upsert(vow.copy(currentStreak = 0, lastRewardDate = today))
                _celebrationEvent.value = "Vow timer lapsed. Streak reset to 0. Reset anytime to rebuild."
            }
            return@withTransaction
        }

        val p = db.playerDao().getPlayerProfileOnce() ?: return@withTransaction
        val newStreak = vow.currentStreak + 1

        val multiplier = (1.0f + (newStreak - 1) * 0.05f).coerceAtMost(3.0f)
        val xpGain = (30 * multiplier).toInt()
        val goldGain = (20 * multiplier).toInt()
        val milestoneBonus = when (newStreak) {
            3 -> 50; 7 -> 150; 14 -> 300; 30 -> 750; 60 -> 1500; 100 -> 3000; else -> 0
        }
        val crystalGain = when (newStreak) {
            7 -> 2; 30 -> 5; 100 -> 15; else -> 0
        }

        var newXp = p.currentXp + xpGain + milestoneBonus
        var newLevel = p.level
        var newReq = p.requiredXp
        var newPts = p.unallocatedStatPoints
        var didLevel = false
        while (newXp >= newReq) {
            newXp -= newReq
            newLevel++
            newReq = (newReq * 1.35).toInt().coerceAtMost(Int.MAX_VALUE / 2)
            newPts += 3
            didLevel = true
        }

        db.playerDao().updateProfile(
            p.copy(
                level = newLevel,
                currentXp = newXp,
                requiredXp = newReq,
                unallocatedStatPoints = newPts,
                gold = p.gold + goldGain,
                manaCrystals = p.manaCrystals + crystalGain,
                discipline = p.discipline + 1,
                rank = determineRank(newLevel)
            )
        )
        db.vowDao().upsert(
            vow.copy(
                currentStreak = newStreak,
                longestStreak = maxOf(vow.longestStreak, newStreak),
                totalRewards = vow.totalRewards + 1,
                lastRewardDate = today,
                totalXpEarned = vow.totalXpEarned + xpGain + milestoneBonus
            )
        )

        _celebrationEvent.value = buildString {
            append("VOW HELD • Day $newStreak • +$xpGain XP, +$goldGain 🪙, +1 Discipline")
            if (milestoneBonus > 0) append("\n🏆 Milestone! +$milestoneBonus bonus XP")
            if (crystalGain > 0) append(" • +$crystalGain 💎")
            if (didLevel) append("\n⚡ LEVEL UP → $newLevel!")
        }
    }

    fun takeVow() = viewModelScope.launch(Dispatchers.IO) {
        vowMutex.withLock {
            val now = System.currentTimeMillis()
            val existing = db.vowDao().getVowStateOnce() ?: VowState()
            db.vowDao().upsert(
                existing.copy(
                    isActive = true,
                    vowStartedAt = if (existing.vowStartedAt == 0L) now else existing.vowStartedAt,
                    lastResetAt = now,
                    lastRewardDate = 0
                )
            )
            _celebrationEvent.value = "VOW OF DISCIPLINE ACTIVE. Reset every 48h to hold the streak."
        }
    }

    fun resetVowTimer() = viewModelScope.launch(Dispatchers.IO) {
        vowMutex.withLock {
            val vow = db.vowDao().getVowStateOnce() ?: return@withLock
            if (!vow.isActive) return@withLock
            db.vowDao().upsert(vow.copy(lastResetAt = System.currentTimeMillis()))
            _celebrationEvent.value = "Vow timer reset. 48h window renewed."
        }
    }

    fun abandonVow() = viewModelScope.launch(Dispatchers.IO) {
        vowMutex.withLock {
            val vow = db.vowDao().getVowStateOnce() ?: return@withLock
            db.vowDao().upsert(vow.copy(isActive = false, currentStreak = 0, lastRewardDate = 0))
            _celebrationEvent.value = "Vow released. No penalty."
        }
    }

    // ----------------------------------------------------
    // SHOP & BLACKSMITH SYSTEM
    // ----------------------------------------------------

    fun vowDiscountPercent(streak: Int): Int = when {
        streak >= 100 -> 15
        streak >= 30 -> 10
        streak >= 7 -> 5
        else -> 0
    }

    fun purchaseShopItem(item: com.example.data.ShopItem) = viewModelScope.launch(Dispatchers.IO) {
        shopMutex.withLock {
            try {
                db.withTransaction {
                    val p = db.playerDao().getPlayerProfileOnce() ?: return@withTransaction

                    if (item.className != null &&
                        !p.selectedClass.equals(item.className, ignoreCase = true)) {
                        _celebrationEvent.value = "Exclusive to ${item.className}."
                        return@withTransaction
                    }
                    if (p.level < item.requiredLevel) {
                        _celebrationEvent.value = "Requires Level ${item.requiredLevel}."
                        return@withTransaction
                    }

                    if (item.isCosmetic) {
                        if (db.purchaseLogDao().isPurchased(item.id)) {
                            _celebrationEvent.value = "Already owned."
                            return@withTransaction
                        }
                    } else if (!item.isConsumable) {
                        val ownedByName = db.equipmentDao().getAllEquipmentOnce().any { it.name == item.equipment.name }
                        val ownedByLog = db.purchaseLogDao().isPurchased(item.id)
                        if (ownedByName || ownedByLog) {
                            _celebrationEvent.value = "Already owned."
                            return@withTransaction
                        }
                    }

                    val vow = db.vowDao().getVowStateOnce()
                    val discount = vowDiscountPercent(vow?.longestStreak ?: 0)
                    val finalGold = item.goldPrice * (100 - discount) / 100

                    if (p.gold < finalGold || p.manaCrystals < item.crystalPrice) {
                        _celebrationEvent.value = "Insufficient currency."
                        return@withTransaction
                    }

                    db.playerDao().updateProfile(
                        p.copy(
                            gold = p.gold - finalGold,
                            manaCrystals = p.manaCrystals - item.crystalPrice
                        )
                    )

                    when {
                        item.isConsumable && item.consumableId != null -> {
                            val existing = db.consumableDao().getOne(item.consumableId)
                            db.consumableDao().upsert(
                                Consumable(item.consumableId, (existing?.count ?: 0) + 1)
                            )
                        }
                        item.isCosmetic -> db.purchaseLogDao().log(PurchaseLog(item.id))
                        else -> {
                            db.equipmentDao().insertEquipment(item.equipment.copy(id = 0))
                            db.purchaseLogDao().log(PurchaseLog(item.id))
                        }
                    }
                }
                _celebrationEvent.value = "PURCHASED: ${item.equipment.name}"
            } catch (e: Exception) {
                _celebrationEvent.value = "Purchase failed: ${e.message}"
            }
        }
    }

    fun consumeOne(consumableId: String, onConsumed: (() -> Unit)? = null) = viewModelScope.launch(Dispatchers.IO) {
        val success = db.consumableDao().decrement(consumableId) > 0
        if (success && onConsumed != null) {
            withContext(Dispatchers.Main) {
                onConsumed()
            }
        }
    }

    // Vow-streak based shop discount that equipped gear can boost
    fun permanentShopDiscount(): Int {
        val gear = equipment.value.filter { it.isEquipped }
        val bandBonus = if (gear.any { it.name == "Unbroken Band" }) 5 else 0
        val crownBonus = if (gear.any { it.name == "Sovereign's Crown" }) 10 else 0
        return bandBonus + crownBonus
    }

    fun teleportToGate(gate: SpawnedGate) = viewModelScope.launch(Dispatchers.IO) {
        radarManager.selectGate(gate)
        radarManager.advanceTowardsGate(gate, gate.distanceFeet)
    }

    // ----------------------------------------------------
    // MILESTONE OFFERS & TRANSCENDENCE
    // ----------------------------------------------------

    private suspend fun checkMilestones() = db.withTransaction {
        if (_battleState.value.inBattle) return@withTransaction

        val p = db.playerDao().getPlayerProfileOnce() ?: return@withTransaction
        val vow = db.vowDao().getVowStateOnce()

        val candidates = buildList {
            listOf(5, 10, 25, 50, 100).forEach { if (p.level >= it) add("level_$it") }
            val longest = vow?.longestStreak ?: 0
            listOf(10, 30, 60, 100).forEach { if (longest >= it) add("streak_$it") }
            if (p.totalQuestsCompleted >= 50) add("quests_50")
            if (p.shadowArmyCount >= 5) add("shadows_5")
        }

        for (key in candidates) {
            if (db.milestoneDao().hasFired(key)) continue
            val tpl = com.example.data.MilestoneOffers.byKey[key] ?: continue
            val now = System.currentTimeMillis()
            db.milestoneDao().log(MilestoneLog(key, now))
            db.pendingOfferDao().insert(
                PendingOffer(
                    milestoneKey = key,
                    title = tpl.title, description = tpl.description,
                    goldCost = tpl.goldCost, crystalCost = tpl.crystalCost,
                    rewardType = tpl.rewardType, rewardPayload = tpl.payload,
                    generatedAt = now, expiresAt = now + PendingOffer.WINDOW_MS
                )
            )
        }
    }

    fun claimOffer(offerId: Long) = viewModelScope.launch(Dispatchers.IO) {
        milestoneMutex.withLock {
            db.withTransaction {
                val offer = db.pendingOfferDao().getById(offerId) ?: return@withTransaction
                if (offer.isClaimed || offer.isDeclined) return@withTransaction
                val now = System.currentTimeMillis()
                if (offer.expiresAt < now) return@withTransaction
                if (offer.expiresAt > now + PendingOffer.WINDOW_MS + 3_600_000L) {
                    db.pendingOfferDao().markDeclined(offer.id)
                    return@withTransaction
                }

                val p = db.playerDao().getPlayerProfileOnce() ?: return@withTransaction
                if (p.gold < offer.goldCost || p.manaCrystals < offer.crystalCost) {
                    _celebrationEvent.value = "Insufficient currency."
                    return@withTransaction
                }

                db.playerDao().updateProfile(
                    p.copy(
                        gold = p.gold - offer.goldCost,
                        manaCrystals = p.manaCrystals - offer.crystalCost
                    )
                )

                when (offer.rewardType) {
                    "EQUIPMENT" -> {
                        com.example.data.MilestoneRewards.buildEquipment(offer.rewardPayload)?.let {
                            db.equipmentDao().insertEquipment(it.copy(id = 0))
                        }
                    }
                    "CONSUMABLE_BUNDLE" -> {
                        offer.rewardPayload.split(",").forEach { part ->
                            val (itemId, countStr) = part.split(":")
                            val count = countStr.toIntOrNull() ?: 1
                            val existing = db.consumableDao().getOne(itemId)
                            db.consumableDao().upsert(
                                Consumable(itemId, (existing?.count ?: 0) + count)
                            )
                        }
                    }
                    "CRYSTALS" -> {
                        val amount = offer.rewardPayload.toIntOrNull() ?: 0
                        db.playerDao().updateProfile(p.copy(manaCrystals = p.manaCrystals + amount))
                    }
                }

                db.pendingOfferDao().markClaimed(offer.id)
                _celebrationEvent.value = "CLAIMED: ${offer.title}"
            }
        }
    }

    fun declineOffer(offerId: Long) = viewModelScope.launch(Dispatchers.IO) {
        milestoneMutex.withLock { db.pendingOfferDao().markDeclined(offerId) }
    }

    private fun checkMilestonesAfterLevelUp() = viewModelScope.launch(Dispatchers.IO) {
        milestoneMutex.withLock { checkMilestones() }
    }
}
