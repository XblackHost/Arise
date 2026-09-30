package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BossCatalog
import com.example.data.DungeonBoss
import com.example.data.model.DiscoveredParty
import com.example.data.model.HunterParty
import com.example.data.model.PartyMember
import com.example.ui.components.NeonCard
import com.example.ui.components.RankBadge
import com.example.ui.components.StatProgressBar
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

enum class MultiplayerTab(val label: String) {
    SQUAD_LOBBY("Squadron Lobby"),
    LAN_DISCOVERY("Local Wi-Fi Radar"),
    GUILD_ROSTER("Simulated Rankings (Demo)")
}

@Composable
fun MultiplayerScreen(
    viewModel: AriseViewModel,
    onNavigateToBossRaid: () -> Unit = {}
) {
    val profile by viewModel.playerProfile.collectAsState()
    val party by viewModel.currentParty.collectAsState()
    val discoveredParties by viewModel.discoveredParties.collectAsState()
    val isBeaconActive by viewModel.isBeaconActive.collectAsState()

    var selectedTab by remember { mutableStateOf(MultiplayerTab.SQUAD_LOBBY) }
    var showCreatePartyDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AriseVoidBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hunter Profile Multiplayer Card
        item {
            NeonCard(
                borderColor = AriseCyanNeon,
                backgroundColor = AriseDeepNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0369A1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "HUNTER SQUADRON ALLIANCE",
                                color = AriseCyanNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            RankBadge(rank = profile?.rank ?: "E-Rank")
                        }
                        Text(
                            text = profile?.name ?: "Hunter Jin",
                            color = AriseTextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Level ${profile?.level ?: 1} ${profile?.selectedClass ?: "Warrior"} • Power: ${profile?.calculateTotalPower() ?: 0}",
                            color = AriseTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (isBeaconActive) AriseEmeraldHeal else AriseTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val beaconText = when {
                            party != null -> "SQUAD LOBBY: ${party?.roomCode}"
                            isBeaconActive -> "LOCAL NETWORK BEACON: ACTIVE"
                            else -> "LAN DISCOVERY: IDLE"
                        }
                        Text(
                            text = beaconText,
                            color = if (party != null) AriseCyanNeon else if (isBeaconActive) AriseEmeraldHeal else AriseTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (party != null) {
                        TextButton(
                            onClick = { viewModel.leaveMultiplayerParty() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("LEAVE SQUAD", color = AriseCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Sub-Navigation Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = AriseDeepNavy,
                contentColor = AriseCyanNeon
            ) {
                MultiplayerTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        when (selectedTab) {
            MultiplayerTab.SQUAD_LOBBY -> {
                if (party == null) {
                    // No Party State: Host, Join by Code, Quick Match
                    item {
                        NeonCard(
                            borderColor = AriseBorderGlow,
                            backgroundColor = AriseSurfaceDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "CO-OP STRIKE TEAM LOBBY",
                                color = AriseCyanNeon,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Form a 2-4 player raid squadron to take down high-tier Gate sovereigns with friends over local Wi-Fi or ally hunters.",
                                color = AriseTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showCreatePartyDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AriseCyanNeon),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CREATE PARTY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        val defaultBoss = BossCatalog.allBosses[0]
                                        viewModel.quickMatchMultiplayer(defaultBoss)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AriseEmeraldHeal),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("QUICK MATCH", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("JOIN BY ROOM CODE", color = AriseTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = joinCodeInput,
                                    onValueChange = { joinCodeInput = it },
                                    placeholder = { Text("e.g. ARISE-4081", color = AriseTextSecondary, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AriseCyanNeon,
                                        unfocusedBorderColor = AriseBorderGlow,
                                        focusedTextColor = AriseTextPrimary,
                                        unfocusedTextColor = AriseTextPrimary
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (joinCodeInput.isNotBlank()) {
                                            viewModel.joinPartyByCode(joinCodeInput) { result ->
                                                when (result) {
                                                    is com.example.data.JoinResult.Success -> {
                                                        joinCodeInput = ""
                                                        joinError = null
                                                    }
                                                    is com.example.data.JoinResult.NotFound -> {
                                                        joinError = "Squadron '${result.code}' not found on local network."
                                                    }
                                                    is com.example.data.JoinResult.Error -> {
                                                        joinError = result.message
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AriseDeepNavy),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("JOIN", color = AriseCyanNeon, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (joinError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "⚠️ $joinError",
                                    color = AriseCrimson,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // Party Assembled State: Members, Boss Target, Combat
                    party?.let { activeParty ->
                        item {
                            ActivePartyCard(
                                party = activeParty,
                                onStartRaid = { viewModel.startCoopRaid() },
                                onAttack = { skill -> viewModel.performCoopAttack(skill) },
                                onEmote = { emote -> viewModel.sendPartyEmote(emote) }
                            )
                        }
                    }
                }
            }

            MultiplayerTab.LAN_DISCOVERY -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DISCOVERED LOCAL WI-FI LOBBIES (${discoveredParties.size})",
                            color = AriseCyanNeon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = { viewModel.multiplayerManager.startListening() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = AriseCyanNeon)
                        }
                    }
                }

                if (discoveredParties.isEmpty()) {
                    item {
                        NeonCard(
                            borderColor = AriseBorderGlow,
                            backgroundColor = AriseSurfaceDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No other local parties detected on this Wi-Fi network yet.",
                                color = AriseTextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start a squad above or have your friend open the app on the same network to auto-connect!",
                                color = AriseCyanNeon,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                items(discoveredParties) { discovered ->
                    DiscoveredPartyCard(
                        discovered = discovered,
                        onJoin = { viewModel.joinDiscoveredParty(discovered) }
                    )
                }
            }

            MultiplayerTab.GUILD_ROSTER -> {
                item {
                    Text(
                        text = "GLOBAL ASSOCIATION HUNTER RANKINGS",
                        color = AriseCyanNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                val hunters = listOf(
                    Triple("Hunter Jin-Woo", "Shadow Monarch • Lv. 28 • Power 1,840", "S-Rank"),
                    Triple("Cha Hae-In", "Sword Dancer • Lv. 22 • Power 1,420", "S-Rank"),
                    Triple("Choi Jong-In", "Ultimate Flame Mage • Lv. 21 • Power 1,350", "S-Rank"),
                    Triple("Baek Yoonho", "White Tiger Beast • Lv. 19 • Power 1,180", "A-Rank"),
                    Triple("Go Gun-Hee", "Association Chairman • Lv. 30 • Power 2,100", "S-Rank"),
                    Triple("Woo Jin-Chul", "Chief Surveillance Inspector • Lv. 16 • Power 960", "A-Rank"),
                    Triple(profile?.name ?: "You", "Class: ${profile?.selectedClass ?: "Warrior"} • Lv. ${profile?.level ?: 1} • Power ${profile?.calculateTotalPower() ?: 0}", profile?.rank ?: "E-Rank")
                )

                items(hunters) { (name, details, rank) ->
                    val isUser = name == profile?.name || name == "You"
                    NeonCard(
                        borderColor = if (isUser) AriseCyanNeon else AriseBorderGlow,
                        backgroundColor = if (isUser) Color(0xFF1E293B) else AriseSurfaceDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isUser) "$name (YOU)" else name,
                                    color = if (isUser) AriseCyanNeon else AriseTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(text = details, color = AriseTextSecondary, fontSize = 11.sp)
                            }
                            RankBadge(rank = rank)
                        }
                    }
                }
            }
        }
    }

    // Create Party Dialog
    if (showCreatePartyDialog) {
        CreatePartyModal(
            onDismiss = { showCreatePartyDialog = false },
            onCreate = { name, boss ->
                viewModel.createMultiplayerParty(name, boss)
                showCreatePartyDialog = false
            }
        )
    }
}

@Composable
fun ActivePartyCard(
    party: HunterParty,
    onStartRaid: () -> Unit,
    onAttack: (String) -> Unit,
    onEmote: (String) -> Unit
) {
    NeonCard(
        borderColor = if (party.isRaidActive) Color(0xFFEF4444) else AriseCyanNeon,
        backgroundColor = Color(0xFF111827),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Party Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = party.name.uppercase(),
                    color = AriseCyanNeon,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Text(
                    text = "Room Code: ${party.roomCode} • Leader: ${party.leaderName}",
                    color = AriseTextSecondary,
                    fontSize = 11.sp
                )
            }
            RankBadge(rank = party.targetBossRank)
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFF1F2937))
        Spacer(modifier = Modifier.height(10.dp))

        // Target Boss Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3F1D1D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color(0xFFF87171))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TARGET RAID BOSS",
                    color = Color(0xFFF87171),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = party.targetBossName,
                    color = AriseTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Party Members List (Up to 4)
        Text(
            text = "SQUADRON OPERATIVES (${party.members.size}/${party.maxMembers})",
            color = AriseCyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        party.members.forEach { member ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = member.avatarEmoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = member.name + if (member.isLeader) " (Leader)" else "",
                            color = AriseTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${member.hunterClass} • Lv.${member.level}",
                            color = AriseCyanNeon,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    StatProgressBar(
                        label = "HP",
                        currentValue = member.currentHp,
                        maxValue = member.maxHp,
                        fillColor = AriseEmeraldHeal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Raid Controls
        if (!party.isRaidActive) {
            Button(
                onClick = onStartRaid,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AriseCrimson),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("👑 LAUNCH CO-OP GATE RAID", color = Color.White, fontWeight = FontWeight.Black)
            }
        } else {
            // Live Co-Op Combat Mode
            StatProgressBar(
                label = "RAID BOSS VITALITY",
                currentValue = party.bossCurrentHp,
                maxValue = party.bossMaxHp,
                fillColor = Color(0xFFEF4444)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!party.isVictory && !party.isDefeat) {
                Text("UNLEASH CO-OP SKILL:", color = AriseCyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onAttack("SHADOW_STRIKE") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AriseDeepNavy),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("🗡️ Shadow Slash", fontSize = 11.sp, color = AriseCyanNeon)
                    }

                    Button(
                        onClick = { onAttack("MONARCH_WRATH") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B0764)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("👑 Monarch Pulse", fontSize = 11.sp, color = AriseShadowViolet)
                    }

                    Button(
                        onClick = { onAttack("ALLIANCE_COMMAND") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("🛡️ Shield Command", fontSize = 11.sp, color = AriseEmeraldHeal)
                    }
                }
            } else if (party.isVictory) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF064E3B))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👑 SQUAD VICTORY! GATE CONQUERED! REWARDS CLAIMED!",
                        color = AriseEmeraldHeal,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            } else if (party.isDefeat) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF450A0A))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💀 SQUAD DEFEATED! RETREAT TO CITADEL!",
                        color = Color(0xFFF87171),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tactical Emotes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionChip(
                    onClick = { onEmote("⚔️ Focus all attacks on the boss!") },
                    label = { Text("⚔️ Attack!", fontSize = 10.sp, color = AriseTextPrimary) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = AriseDeepNavy)
                )
                SuggestionChip(
                    onClick = { onEmote("🛡️ Fall back & defensive stance!") },
                    label = { Text("🛡️ Defend!", fontSize = 10.sp, color = AriseTextPrimary) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = AriseDeepNavy)
                )
                SuggestionChip(
                    onClick = { onEmote("👑 ARISE! Let the shadows rise!") },
                    label = { Text("👑 ARISE!", fontSize = 10.sp, color = AriseGoldRank) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = AriseDeepNavy)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Co-Op Combat Feed
            Text("LIVE CO-OP SQUADRON FEED", color = AriseTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF090D16))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                party.raidLogs.takeLast(5).forEach { log ->
                    Text(
                        text = if (log.isBoss) "🔴 [BOSS] ${log.sender}: ${log.action}" else "🔵 [SQUAD] ${log.sender}: ${log.action}",
                        color = if (log.isBoss) Color(0xFFFCA5A5) else AriseCyanNeon,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoveredPartyCard(
    discovered: DiscoveredParty,
    onJoin: () -> Unit
) {
    NeonCard(
        borderColor = AriseBorderGlow,
        backgroundColor = AriseSurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = discovered.partyName,
                        color = AriseTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    RankBadge(rank = discovered.leaderRank)
                }
                Text(
                    text = "Leader: ${discovered.leaderName} • Boss: ${discovered.targetBoss}",
                    color = AriseCyanNeon,
                    fontSize = 11.sp
                )
                Text(
                    text = "Room: ${discovered.roomCode} • Members: ${discovered.memberCount}/${discovered.maxMembers}",
                    color = AriseTextSecondary,
                    fontSize = 10.sp
                )
            }

            Button(
                onClick = onJoin,
                colors = ButtonDefaults.buttonColors(containerColor = AriseCyanNeon),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("JOIN", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePartyModal(
    onDismiss: () -> Unit,
    onCreate: (name: String, boss: DungeonBoss) -> Unit
) {
    var partyName by remember { mutableStateOf("Shadow Strike Force") }
    var selectedBoss by remember { mutableStateOf(BossCatalog.allBosses[0]) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AriseSurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "CREATE MULTIPLAYER SQUADRON",
                color = AriseCyanNeon,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )

            OutlinedTextField(
                value = partyName,
                onValueChange = { partyName = it },
                label = { Text("Squadron Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriseCyanNeon,
                    unfocusedBorderColor = AriseBorderGlow
                )
            )

            Text("SELECT TARGET GATE BOSS:", color = AriseTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            BossCatalog.allBosses.take(5).forEach { boss ->
                val isSelected = selectedBoss.id == boss.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AriseDeepNavy else Color.Transparent)
                        .border(1.dp, if (isSelected) AriseCyanNeon else Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .clickable { selectedBoss = boss }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = boss.iconEmoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = boss.name, color = AriseTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "${boss.rank} • Rec. Lv ${boss.recommendedLevel}", color = AriseTextSecondary, fontSize = 11.sp)
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { selectedBoss = boss },
                        colors = RadioButtonDefaults.colors(selectedColor = AriseCyanNeon)
                    )
                }
            }

            Button(
                onClick = { onCreate(partyName, selectedBoss) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AriseCyanNeon),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("HOST SQUADRON & BROADCAST", color = Color.Black, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
