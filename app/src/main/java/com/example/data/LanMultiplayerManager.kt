package com.example.data

import android.util.Log
import com.example.data.model.DiscoveredParty
import com.example.data.model.HunterParty
import com.example.data.model.PartyMember
import com.example.data.model.PlayerProfile
import com.example.data.model.RaidLogEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.UUID
import kotlin.random.Random

class LanMultiplayerManager {

    companion object {
        private const val TAG = "LanMultiplayer"
        private const val UDP_PORT = 8888
        private const val BROADCAST_IP = "255.255.255.255"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentParty = MutableStateFlow<HunterParty?>(null)
    val currentParty: StateFlow<HunterParty?> = _currentParty.asStateFlow()

    private val _discoveredParties = MutableStateFlow<List<DiscoveredParty>>(emptyList())
    val discoveredParties: StateFlow<List<DiscoveredParty>> = _discoveredParties.asStateFlow()

    private val _isBeaconActive = MutableStateFlow(false)
    val isBeaconActive: StateFlow<Boolean> = _isBeaconActive.asStateFlow()

    private var broadcastJob: Job? = null
    private var listenJob: Job? = null

    init {
        // Pre-populate with a nearby active local guild party for quick in-person/offline play
        _discoveredParties.value = listOf(
            DiscoveredParty(
                roomCode = "ARISE-4081",
                partyName = "Seoul Guild Vanguard",
                leaderName = "Cha Hae-In",
                leaderRank = "S-Rank",
                targetBoss = "Iron Fang Cerberus",
                memberCount = 2,
                maxMembers = 4,
                ipAddress = "192.168.1.45"
            ),
            DiscoveredParty(
                roomCode = "ARISE-7729",
                partyName = "Rookie Hunter Gate Squad",
                leaderName = "Baek Yoonho",
                leaderRank = "A-Rank",
                targetBoss = "Goblin Chieftain Krag",
                memberCount = 3,
                maxMembers = 4,
                ipAddress = "192.168.1.88"
            )
        )
        startListening()
    }

    fun startListening() {
        if (listenJob?.isActive == true) return
        listenJob = scope.launch {
            try {
                val socket = DatagramSocket(UDP_PORT).apply {
                    broadcast = true
                    soTimeout = 4000
                }
                val buffer = ByteArray(1024)

                while (isActive) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length).trim()
                        if (text.startsWith("ARISE_LOBBY|")) {
                            parseDiscoveredParty(text, packet.address.hostAddress ?: "192.168.1.x")
                        }
                    } catch (e: Exception) {
                        // socket timeout or non-critical packet drop
                    }
                }
                socket.close()
            } catch (e: Exception) {
                Log.w(TAG, "LAN listener initialization notice: ${e.message}")
            }
        }
    }

    private fun parseDiscoveredParty(payload: String, ip: String) {
        val tokens = payload.split("|")
        if (tokens.size >= 8) {
            val code = tokens[1]
            val partyName = tokens[2]
            val leader = tokens[3]
            val rank = tokens[4]
            val boss = tokens[5]
            val members = tokens[6].toIntOrNull() ?: 1
            val maxMembers = tokens[7].toIntOrNull() ?: 4

            val discovered = DiscoveredParty(
                roomCode = code,
                partyName = partyName,
                leaderName = leader,
                leaderRank = rank,
                targetBoss = boss,
                memberCount = members,
                maxMembers = maxMembers,
                ipAddress = ip
            )
            val currentList = _discoveredParties.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.roomCode == code }
            if (existingIndex >= 0) {
                currentList[existingIndex] = discovered
            } else {
                currentList.add(0, discovered)
            }
            _discoveredParties.value = currentList
        }
    }

    fun createParty(
        partyName: String,
        targetBoss: DungeonBoss,
        maxMembers: Int = 4,
        playerProfile: PlayerProfile
    ): HunterParty {
        val randomSuffix = Random.nextInt(1000, 9999)
        val roomCode = "ARISE-$randomSuffix"
        val leader = PartyMember(
            id = UUID.randomUUID().toString(),
            name = playerProfile.name,
            hunterClass = playerProfile.selectedClass,
            rank = playerProfile.rank,
            level = playerProfile.level,
            power = playerProfile.calculateTotalPower(),
            maxHp = playerProfile.maxHp,
            currentHp = playerProfile.hp,
            isLeader = true,
            isReady = true,
            avatarEmoji = "👑"
        )

        // Scale boss HP for multiplayer raid
        val scaledBossHp = (targetBoss.maxHp * 2.2).toInt()

        val party = HunterParty(
            roomId = UUID.randomUUID().toString(),
            roomCode = roomCode,
            name = partyName.ifBlank { "Strike Team $roomCode" },
            leaderName = playerProfile.name,
            targetBossId = targetBoss.id,
            targetBossName = targetBoss.name,
            targetBossRank = targetBoss.rank,
            maxMembers = maxMembers,
            members = listOf(leader),
            isRaidActive = false,
            bossMaxHp = scaledBossHp,
            bossCurrentHp = scaledBossHp,
            raidLogs = listOf(
                RaidLogEntry(
                    sender = "SYSTEM",
                    action = "Squadron created by ${playerProfile.name}. Room Code: $roomCode. Waiting for Hunter allies."
                )
            )
        )

        _currentParty.value = party
        startBroadcasting(party)
        return party
    }

    fun quickMatch(playerProfile: PlayerProfile, targetBoss: DungeonBoss): HunterParty {
        val party = createParty(
            partyName = "Awakened Strike Force",
            targetBoss = targetBoss,
            maxMembers = 4,
            playerProfile = playerProfile
        )

        // Add 3 iconic Solo Leveling / Hunter companions for immediate instant co-op
        val ally1 = PartyMember(
            id = "ally-1",
            name = "Cha Hae-In",
            hunterClass = "Sword Dancer",
            rank = "S-Rank",
            level = (playerProfile.level + 2).coerceAtLeast(4),
            power = playerProfile.calculateTotalPower() + 120,
            maxHp = 180,
            currentHp = 180,
            isLeader = false,
            isReady = true,
            avatarEmoji = "⚔️"
        )
        val ally2 = PartyMember(
            id = "ally-2",
            name = "Choi Jong-In",
            hunterClass = "Ultimate Mage",
            rank = "S-Rank",
            level = (playerProfile.level + 1).coerceAtLeast(3),
            power = playerProfile.calculateTotalPower() + 90,
            maxHp = 150,
            currentHp = 150,
            isLeader = false,
            isReady = true,
            avatarEmoji = "🔥"
        )
        val ally3 = PartyMember(
            id = "ally-3",
            name = "Baek Yoonho",
            hunterClass = "White Tiger Beast",
            rank = "A-Rank",
            level = (playerProfile.level).coerceAtLeast(2),
            power = playerProfile.calculateTotalPower() + 60,
            maxHp = 220,
            currentHp = 220,
            isLeader = false,
            isReady = true,
            avatarEmoji = "🐯"
        )

        val fullParty = party.copy(
            members = party.members + listOf(ally1, ally2, ally3),
            raidLogs = party.raidLogs + listOf(
                RaidLogEntry(sender = "SYSTEM", action = "Cha Hae-In, Choi Jong-In, and Baek Yoonho joined the raid squad!")
            )
        )
        _currentParty.value = fullParty
        return fullParty
    }

    fun joinParty(discovered: DiscoveredParty, playerProfile: PlayerProfile): Boolean {
        val boss = BossCatalog.allBosses.find { it.name.contains(discovered.targetBoss) } ?: BossCatalog.allBosses.first()
        val leader = PartyMember(
            id = "leader-${discovered.roomCode}",
            name = discovered.leaderName,
            hunterClass = "Guild Champion",
            rank = discovered.leaderRank,
            level = 5,
            power = 350,
            maxHp = 200,
            currentHp = 200,
            isLeader = true,
            isReady = true,
            avatarEmoji = "🛡️"
        )
        val me = PartyMember(
            id = UUID.randomUUID().toString(),
            name = playerProfile.name,
            hunterClass = playerProfile.selectedClass,
            rank = playerProfile.rank,
            level = playerProfile.level,
            power = playerProfile.calculateTotalPower(),
            maxHp = playerProfile.maxHp,
            currentHp = playerProfile.hp,
            isLeader = false,
            isReady = true,
            avatarEmoji = "🗡️"
        )

        val scaledHp = (boss.maxHp * 2.2).toInt()
        val joinedParty = HunterParty(
            roomId = UUID.randomUUID().toString(),
            roomCode = discovered.roomCode,
            name = discovered.partyName,
            leaderName = discovered.leaderName,
            targetBossId = boss.id,
            targetBossName = boss.name,
            targetBossRank = boss.rank,
            maxMembers = discovered.maxMembers,
            members = listOf(leader, me),
            isRaidActive = false,
            bossMaxHp = scaledHp,
            bossCurrentHp = scaledHp,
            raidLogs = listOf(
                RaidLogEntry(sender = "SYSTEM", action = "Joined ${discovered.partyName} (Room ${discovered.roomCode})! Ready for Gate incursion.")
            )
        )
        _currentParty.value = joinedParty
        return true
    }

    fun joinByCode(code: String, playerProfile: PlayerProfile): Boolean {
        val cleanCode = code.trim().uppercase()
        val matching = _discoveredParties.value.find { it.roomCode.equals(cleanCode, ignoreCase = true) }
        return if (matching != null) {
            joinParty(matching, playerProfile)
        } else {
            // Generate squad room with entered code
            val defaultBoss = BossCatalog.allBosses[0]
            val synthetic = DiscoveredParty(
                roomCode = cleanCode,
                partyName = "Strike Force $cleanCode",
                leaderName = "Senior Hunter Woo",
                leaderRank = "B-Rank",
                targetBoss = defaultBoss.name,
                memberCount = 1,
                maxMembers = 4
            )
            joinParty(synthetic, playerProfile)
        }
    }

    fun leaveParty() {
        stopBroadcasting()
        _currentParty.value = null
    }

    fun startCoopRaid() {
        val party = _currentParty.value ?: return
        _currentParty.value = party.copy(
            isRaidActive = true,
            isVictory = false,
            isDefeat = false,
            raidLogs = party.raidLogs + RaidLogEntry(
                sender = "SYSTEM",
                action = "⚔️ ALLIANCE GATE INVASION INITIATED against ${party.targetBossName}! Unleash coordinated skills!"
            )
        )
    }

    fun performPartyCombatTurn(
        playerProfile: PlayerProfile,
        skillType: String,
        onVictory: (xpGain: Int, goldGain: Int, crystalGain: Int) -> Unit
    ) {
        val party = _currentParty.value ?: return
        if (!party.isRaidActive || party.isVictory || party.isDefeat) return

        val playerDamage = when (skillType.uppercase()) {
            "SHADOW_STRIKE" -> (playerProfile.strength * 2.8 + playerProfile.agility * 1.5).toInt() + Random.nextInt(10, 25)
            "MONARCH_WRATH" -> (playerProfile.intelligence * 3.2 + playerProfile.focus * 1.8).toInt() + Random.nextInt(15, 35)
            "ALLIANCE_COMMAND" -> (playerProfile.discipline * 2.5 + playerProfile.vitality * 1.5).toInt() + Random.nextInt(8, 20)
            else -> (playerProfile.strength * 2.0).toInt() + Random.nextInt(5, 15)
        }

        val updatedLogs = party.raidLogs.toMutableList()
        var currentBossHp = (party.bossCurrentHp - playerDamage).coerceAtLeast(0)

        updatedLogs.add(
            RaidLogEntry(
                sender = playerProfile.name,
                action = "unleashes $skillType dealing $playerDamage critical damage!",
                damage = playerDamage
            )
        )

        // Ally companion strikes
        party.members.filter { !it.isLeader && it.currentHp > 0 }.forEach { ally ->
            if (currentBossHp > 0) {
                val allyDmg = (ally.power * 0.45).toInt() + Random.nextInt(15, 30)
                currentBossHp = (currentBossHp - allyDmg).coerceAtLeast(0)
                val allySkillName = when (ally.hunterClass) {
                    "Sword Dancer" -> "Light Cleave"
                    "Ultimate Mage" -> "Infernal Flame Column"
                    "White Tiger Beast" -> "Beast Roar Tear"
                    else -> "Weapon Combo"
                }
                updatedLogs.add(
                    RaidLogEntry(
                        sender = ally.name,
                        action = "casts $allySkillName for $allyDmg damage!",
                        damage = allyDmg
                    )
                )
            }
        }

        // Check Victory
        if (currentBossHp <= 0) {
            updatedLogs.add(
                RaidLogEntry(
                    sender = "SYSTEM",
                    action = "👑 VICTORY! ${party.targetBossName} has been annihilated by the Guild Squadron!"
                )
            )
            _currentParty.value = party.copy(
                bossCurrentHp = 0,
                isVictory = true,
                raidLogs = updatedLogs
            )
            val xp = party.bossMaxHp / 3
            val gold = party.bossMaxHp
            val crystals = 10
            onVictory(xp, gold, crystals)
            return
        }

        // Boss counter-attacks party
        val bossAreaDmg = Random.nextInt(15, 32)
        val updatedMembers = party.members.map { member ->
            val newHp = (member.currentHp - bossAreaDmg).coerceAtLeast(0)
            member.copy(currentHp = newHp)
        }

        updatedLogs.add(
            RaidLogEntry(
                sender = party.targetBossName,
                action = "unleashes Cataclysm Shockwave! All party members take $bossAreaDmg damage!",
                damage = bossAreaDmg,
                isBoss = true
            )
        )

        val allDead = updatedMembers.all { it.currentHp <= 0 }
        _currentParty.value = party.copy(
            bossCurrentHp = currentBossHp,
            members = updatedMembers,
            isDefeat = allDead,
            raidLogs = updatedLogs
        )
    }

    fun sendPartyEmote(sender: String, message: String) {
        val party = _currentParty.value ?: return
        val entry = RaidLogEntry(sender = sender, action = message)
        _currentParty.value = party.copy(raidLogs = party.raidLogs + entry)
    }

    private fun startBroadcasting(party: HunterParty) {
        stopBroadcasting()
        _isBeaconActive.value = true
        broadcastJob = scope.launch {
            try {
                val socket = DatagramSocket().apply { broadcast = true }
                val targetAddress = InetAddress.getByName(BROADCAST_IP)
                val payload = "ARISE_LOBBY|${party.roomCode}|${party.name}|${party.leaderName}|A-Rank|${party.targetBossName}|${party.members.size}|${party.maxMembers}"
                val bytes = payload.toByteArray()

                while (isActive) {
                    try {
                        val packet = DatagramPacket(bytes, bytes.size, targetAddress, UDP_PORT)
                        socket.send(packet)
                    } catch (e: Exception) {
                        // ignore network drop
                    }
                    delay(3000)
                }
                socket.close()
            } catch (e: Exception) {
                Log.w(TAG, "LAN beacon broadcast error: ${e.message}")
            }
        }
    }

    private fun stopBroadcasting() {
        _isBeaconActive.value = false
        broadcastJob?.cancel()
        broadcastJob = null
    }
}
