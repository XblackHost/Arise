package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.data.AriseDatabase
import com.example.data.BossCatalog
import com.example.data.DungeonBoss
import com.example.data.HunterRadarManager
import com.example.data.LanMultiplayerManager
import com.example.data.model.*
import com.example.security.ApiKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BattleState(
    val inBattle: Boolean = false,
    val currentBoss: DungeonBoss? = null,
    val bossCurrentHp: Int = 100,
    val playerCurrentHp: Int = 120,
    val playerCurrentMp: Int = 60,
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
    val multiplayerManager = LanMultiplayerManager()
    val currentParty: StateFlow<HunterParty?> = multiplayerManager.currentParty
    val discoveredParties: StateFlow<List<DiscoveredParty>> = multiplayerManager.discoveredParties
    val isBeaconActive: StateFlow<Boolean> = multiplayerManager.isBeaconActive

    // AI companion loading
    private val _isNyxReplying = MutableStateFlow(false)
    val isNyxReplying: StateFlow<Boolean> = _isNyxReplying.asStateFlow()

    // ----------------------------------------------------
    // API KEY & FIRST LAUNCH MANAGEMENT
    // ----------------------------------------------------

    fun saveApiKey(rawKey: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _keyValidationStatus.value = "Encrypting and verifying key with Google Gemini..."
            val verifyResult = geminiService.verifyKey(rawKey)
            if (verifyResult.isSuccess) {
                apiKeyStorage.saveApiKey(rawKey)
                _maskedApiKey.value = apiKeyStorage.getMaskedApiKey()
                _hasCompletedFirstLaunch.value = true
                _keyValidationStatus.value = "Gemini Key verified & locked in local secure vault!"
                onDone(true)
            } else {
                // If network failure or invalid, give the user the option to save anyway or report error
                val errMsg = verifyResult.exceptionOrNull()?.message ?: "Verification failed."
                _keyValidationStatus.value = "Notice: $errMsg. (Saving in local storage anyway)"
                apiKeyStorage.saveApiKey(rawKey)
                _maskedApiKey.value = apiKeyStorage.getMaskedApiKey()
                _hasCompletedFirstLaunch.value = true
                onDone(true)
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
    }

    // ----------------------------------------------------
    // PLAYER & PROGRESSION
    // ----------------------------------------------------

    fun allocateStat(statName: String) {
        val current = playerProfile.value ?: return
        if (current.unallocatedStatPoints <= 0) return

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

        viewModelScope.launch(Dispatchers.IO) {
            db.playerDao().updateProfile(updated)
        }
    }

    fun completeQuest(quest: Quest) {
        if (quest.isCompleted) return
        viewModelScope.launch(Dispatchers.IO) {
            db.questDao().markQuestCompleted(quest.id)
            val profile = playerProfile.value ?: return@launch

            var newXp = profile.currentXp + quest.xpReward
            var newLevel = profile.level
            var newReqXp = profile.requiredXp
            var newStatPoints = profile.unallocatedStatPoints
            var newHp = profile.hp
            var newMaxHp = profile.maxHp
            var didLevelUp = false

            while (newXp >= newReqXp) {
                newXp -= newReqXp
                newLevel += 1
                newReqXp = (newReqXp * 1.35).toInt()
                newStatPoints += 3
                newMaxHp += 20
                newHp = newMaxHp
                didLevelUp = true
            }

            // Stat gain from quest
            val updated = when (quest.targetAttribute.uppercase()) {
                "STRENGTH" -> profile.copy(strength = profile.strength + quest.attributeGain)
                "ENDURANCE" -> profile.copy(endurance = profile.endurance + quest.attributeGain)
                "AGILITY" -> profile.copy(agility = profile.agility + quest.attributeGain)
                "INTELLIGENCE" -> profile.copy(intelligence = profile.intelligence + quest.attributeGain)
                "FOCUS" -> profile.copy(focus = profile.focus + quest.attributeGain)
                "DISCIPLINE" -> profile.copy(discipline = profile.discipline + quest.attributeGain)
                "VITALITY" -> profile.copy(vitality = profile.vitality + quest.attributeGain)
                else -> profile
            }.copy(
                level = newLevel,
                currentXp = newXp,
                requiredXp = newReqXp,
                hp = newHp,
                maxHp = newMaxHp,
                unallocatedStatPoints = newStatPoints,
                gold = profile.gold + quest.goldReward,
                totalQuestsCompleted = profile.totalQuestsCompleted + 1,
                rank = determineRank(newLevel)
            )

            db.playerDao().updateProfile(updated)

            if (didLevelUp) {
                _celebrationEvent.value = "LEVEL UP! Hunter reached Level $newLevel! +3 Stat Points awarded!"
            }
        }
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

    fun setPlayerClass(newClass: String) {
        val current = playerProfile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            db.playerDao().updateProfile(current.copy(selectedClass = newClass))
        }
    }

    fun completeOnboarding(name: String, chosenClass: String) {
        val current = playerProfile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            db.playerDao().updateProfile(
                current.copy(
                    name = name.ifBlank { "Awakened Hunter" },
                    selectedClass = chosenClass,
                    isOnboardingComplete = true
                )
            )
        }
    }

    // ----------------------------------------------------
    // SHADOW ARMY & DUNGEON COMBAT
    // ----------------------------------------------------

    fun startBossBattle(boss: DungeonBoss) {
        val p = playerProfile.value
        _battleState.value = BattleState(
            inBattle = true,
            currentBoss = boss,
            bossCurrentHp = boss.maxHp,
            playerCurrentHp = p?.maxHp ?: 120,
            playerCurrentMp = p?.maxMp ?: 60,
            logMessages = listOf(
                "⚔️ Encounter commenced! ${boss.name} (${boss.rank}) emerges with menacing intent!",
                "Boss Weakness: ${boss.weakness}. Watch out for '${boss.bossSpecialAttackName}'!"
            ),
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

    fun executePlayerAttack(actionType: String) {
        val current = _battleState.value
        if (!current.inBattle || current.isVictory || current.isDefeat) return

        val profile = playerProfile.value ?: return
        val boss = current.currentBoss ?: return

        var damageDealt = 0
        var mpCost = 0
        var playerDefendingThisTurn = false
        var potionsLeft = current.healthPotionsRemaining
        var playerHpAfterHeal = current.playerCurrentHp
        var playerMpAfterAction = current.playerCurrentMp

        val actionLogs = mutableListOf<String>()

        when (actionType) {
            "HEAL" -> {
                if (potionsLeft <= 0) {
                    _battleState.value = current.copy(
                        logMessages = current.logMessages + "⚠️ No Elixirs of Life remaining!"
                    )
                    return
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
            "BASIC" -> {
                damageDealt = (profile.strength * 2.4 + profile.agility * 1.0 - boss.defense * 0.4).toInt().coerceAtLeast(18)
                actionLogs.add("Hunter strikes with Physical Precision for $damageDealt damage!")
            }
            "CLASS_SKILL" -> {
                mpCost = 15
                if (playerMpAfterAction < mpCost) {
                    _battleState.value = current.copy(
                        logMessages = current.logMessages + "⚠️ Not enough MP for Class Skill (Needs 15 MP)!"
                    )
                    return
                }
                playerMpAfterAction -= mpCost
                damageDealt = (profile.strength * 3.4 + profile.agility * 2.2 - boss.defense * 0.5).toInt().coerceAtLeast(35)
                actionLogs.add("⚡ Hunter unleashes Class Vanguard Technique for $damageDealt critical damage!")
            }
            "SHADOW_SUMMON" -> {
                mpCost = 25
                if (playerMpAfterAction < mpCost) {
                    _battleState.value = current.copy(
                        logMessages = current.logMessages + "⚠️ Not enough MP to command Shadows (Needs 25 MP)!"
                    )
                    return
                }
                playerMpAfterAction -= mpCost
                val shadowPower = shadowArmy.value.sumOf { it.attackPower }
                damageDealt = (shadowPower * 1.5 + profile.intelligence * 2.0 - boss.defense * 0.3).toInt().coerceAtLeast(45)
                actionLogs.add("👑 Shadow Legion swarms the boss, tearing through defenses for $damageDealt damage!")
            }
            "ULTIMATE" -> {
                mpCost = 40
                if (playerMpAfterAction < mpCost) {
                    _battleState.value = current.copy(
                        logMessages = current.logMessages + "⚠️ Not enough MP for Monarch Wrath (Needs 40 MP)!"
                    )
                    return
                }
                playerMpAfterAction -= mpCost
                damageDealt = (profile.strength * 5.2 + profile.intelligence * 3.8 - boss.defense * 0.4).toInt().coerceAtLeast(75)
                actionLogs.add("💥 MONARCH'S WRATH: Dark astral shockwaves annihilate the battlefield for $damageDealt damage!")
            }
        }

        val newBossHp = (current.bossCurrentHp - damageDealt).coerceAtLeast(0)

        // Check Boss Defeat
        if (newBossHp <= 0) {
            val victoryLogs = current.logMessages + actionLogs +
                    "${boss.name} has fallen! A dark aura gathers... Extraction window is OPEN!"
            _battleState.value = current.copy(
                bossCurrentHp = 0,
                playerCurrentHp = playerHpAfterHeal,
                playerCurrentMp = playerMpAfterAction,
                logMessages = victoryLogs,
                isVictory = true,
                extractionEligible = true,
                healthPotionsRemaining = potionsLeft
            )
            rewardDungeonVictory(boss)
            return
        }

        // Check Boss Enrage Trigger (< 35% HP)
        var isEnraged = current.isBossEnraged
        if (newBossHp <= (boss.maxHp * 0.35f) && !isEnraged) {
            isEnraged = true
            actionLogs.add("⚠️ CRITICAL WARNING: ${boss.name} has ENTERED ENRAGED PHASE! Eyes gleam with crimson slaughter (+35% ATK)!")
        }

        // Boss Combat AI Turn
        val atkMultiplier = if (isEnraged) 1.35f else 1.0f
        var rawBossDmg = 0
        var willChargeNext = false

        if (current.isBossChargingUltimate) {
            // Boss releases catastrophic ultimate!
            rawBossDmg = (boss.attack * atkMultiplier * 2.2f - profile.endurance * 0.5f).toInt().coerceAtLeast(25)
            actionLogs.add("🔴 CATACLYSM: ${boss.name} unleashes '${boss.bossSpecialAttackName}'!")
        } else {
            // Roll boss behavior: 25% chance to start charging ultimate, 75% standard or special hit
            val roll = (1..100).random()
            if (roll <= 25 && !isEnraged) {
                willChargeNext = true
                rawBossDmg = (boss.attack * atkMultiplier * 0.6f).toInt().coerceAtLeast(8)
                actionLogs.add("⚠️ ALERT: ${boss.name} begins gathering catastrophic power for '${boss.bossSpecialAttackName}'! [PARRY/DEFEND] next turn!")
            } else {
                rawBossDmg = (boss.attack * atkMultiplier * 1.25f - profile.endurance * 0.45f).toInt().coerceAtLeast(14)
                actionLogs.add("${boss.name} counterattacks with ferocious strikes for $rawBossDmg damage!")
            }
        }

        // Apply Player Parry/Defend Damage Reduction
        val finalBossDmg = if (playerDefendingThisTurn) {
            val reduced = (rawBossDmg * 0.30f).toInt().coerceAtLeast(4)
            actionLogs.add("🛡️ PARRY EFFECTIVE! Absorbed 70% damage, taking only $reduced damage!")
            reduced
        } else {
            rawBossDmg
        }

        val newPlayerHp = (playerHpAfterHeal - finalBossDmg).coerceAtLeast(0)

        if (newPlayerHp <= 0) {
            actionLogs.add("💀 Hunter was incapacitated by ${boss.name}! Gate collapse imminent!")
            _battleState.value = current.copy(
                bossCurrentHp = newBossHp,
                playerCurrentHp = 0,
                playerCurrentMp = playerMpAfterAction,
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
                playerCurrentHp = newPlayerHp,
                playerCurrentMp = playerMpAfterAction,
                logMessages = current.logMessages + actionLogs,
                isBossEnraged = isEnraged,
                isBossChargingUltimate = willChargeNext,
                isPlayerDefending = playerDefendingThisTurn,
                healthPotionsRemaining = potionsLeft
            )
        }
    }

    /**
     * Executes the iconic "ARISE" extraction command.
     */
    fun performAriseExtraction() {
        val state = _battleState.value
        val boss = state.currentBoss ?: return
        if (!state.extractionEligible || state.isExtracted) return

        viewModelScope.launch(Dispatchers.IO) {
            // Add shadow boss to Army
            val newShadow = ShadowUnit(
                name = boss.shadowUnitName,
                title = boss.shadowUnitTitle,
                rank = boss.shadowRank,
                level = 1,
                attackPower = boss.attack + 20,
                defense = boss.defense + 10,
                speed = 22,
                loyalty = 100,
                signatureSkill = boss.shadowSignatureSkill,
                skillDescription = boss.shadowSkillDesc,
                isSummoned = true,
                lore = "Defeated in combat. Extracted through the supreme command 'ARISE'. Now loyally serves the Hunter."
            )
            db.shadowDao().insertShadow(newShadow)

            val p = db.playerDao().getPlayerProfileOnce()
            if (p != null) {
                db.playerDao().updateProfile(
                    p.copy(
                        shadowArmyCount = p.shadowArmyCount + 1,
                        manaCrystals = p.manaCrystals + 5
                    )
                )
            }

            _battleState.value = state.copy(
                isExtracted = true,
                logMessages = state.logMessages +
                        "COMMAND UTTERED: 'ARISE!'" +
                        "Shadow Extraction SUCCESSFUL! ${boss.shadowUnitName} has joined your Shadow Army!"
            )

            _celebrationEvent.value = "SHADOW EXTRACTION: 'ARISE!' ${boss.shadowUnitName} has joined your Legion!"
        }
    }

    private fun rewardDungeonVictory(boss: DungeonBoss) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = db.playerDao().getPlayerProfileOnce() ?: return@launch
            val xpGain = boss.maxHp / 2
            val goldGain = boss.attack * 8

            var newXp = p.currentXp + xpGain
            var newLevel = p.level
            var newReqXp = p.requiredXp
            var newStatPoints = p.unallocatedStatPoints
            var didLevelUp = false

            while (newXp >= newReqXp) {
                newXp -= newReqXp
                newLevel += 1
                newReqXp = (newReqXp * 1.35).toInt()
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
                    rank = determineRank(newLevel)
                )
            )

            if (didLevelUp) {
                _celebrationEvent.value = "DUNGEON CLEARED & LEVEL UP! Hunter reached Level $newLevel!"
            }
        }
    }

    fun endBattle() {
        _battleState.value = BattleState()
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
        radarManager.addWalkedFeet(feet)
        viewModelScope.launch(Dispatchers.IO) {
            val p = db.playerDao().getPlayerProfileOnce() ?: return@launch
            val newSteps = p.totalSteps + (feet / 2.5f).toInt()
            val newMeters = p.totalDistanceMeters + (feet / HunterRadarManager.FEET_PER_METER)
            db.playerDao().updateProfile(p.copy(totalSteps = newSteps, totalDistanceMeters = newMeters))
        }
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

    fun joinPartyByCode(code: String) {
        val p = playerProfile.value ?: return
        multiplayerManager.joinByCode(code, p)
    }

    fun leaveMultiplayerParty() {
        multiplayerManager.leaveParty()
    }

    fun startCoopRaid() {
        multiplayerManager.startCoopRaid()
    }

    fun performCoopAttack(skillType: String) {
        val p = playerProfile.value ?: return
        multiplayerManager.performPartyCombatTurn(p, skillType) { xp, gold, crystals ->
            viewModelScope.launch(Dispatchers.IO) {
                val prof = db.playerDao().getPlayerProfileOnce() ?: return@launch
                var newXp = prof.currentXp + xp
                var newLevel = prof.level
                var newReq = prof.requiredXp
                var newStatPoints = prof.unallocatedStatPoints
                var didLevelUp = false

                while (newXp >= newReq) {
                    newXp -= newReq
                    newLevel += 1
                    newReq = (newReq * 1.35).toInt()
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
        }
    }

    fun sendPartyEmote(message: String) {
        val p = playerProfile.value ?: return
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

            val profile = playerProfile.value
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
            val p = playerProfile.value
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
            db.taskDao().markTaskCompleted(task.id)
            val p = playerProfile.value ?: return@launch
            db.playerDao().updateProfile(
                p.copy(
                    currentXp = p.currentXp + task.xpReward,
                    gold = p.gold + task.goldReward
                )
            )
        }
    }
}
