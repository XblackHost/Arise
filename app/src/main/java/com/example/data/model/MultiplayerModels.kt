package com.example.data.model

data class HunterParty(
    val roomId: String,
    val roomCode: String,
    val name: String,
    val leaderName: String,
    val targetBossId: String,
    val targetBossName: String,
    val targetBossRank: String,
    val maxMembers: Int = 4,
    val members: List<PartyMember> = emptyList(),
    val isRaidActive: Boolean = false,
    val bossMaxHp: Int = 1000,
    val bossCurrentHp: Int = 1000,
    val raidLogs: List<RaidLogEntry> = emptyList(),
    val isVictory: Boolean = false,
    val isDefeat: Boolean = false
)

data class PartyMember(
    val id: String,
    val name: String,
    val hunterClass: String,
    val rank: String,
    val level: Int,
    val power: Int,
    val maxHp: Int = 120,
    val currentHp: Int = 120,
    val isLeader: Boolean = false,
    val isReady: Boolean = true,
    val avatarEmoji: String = "⚔️"
)

data class RaidLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val action: String,
    val damage: Int = 0,
    val isBoss: Boolean = false
)

data class DiscoveredParty(
    val roomCode: String,
    val partyName: String,
    val leaderName: String,
    val leaderRank: String,
    val targetBoss: String,
    val memberCount: Int,
    val maxMembers: Int,
    val ipAddress: String = "192.168.1.x"
)
