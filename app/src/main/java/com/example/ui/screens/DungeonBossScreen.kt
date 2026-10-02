package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BossCatalog
import com.example.data.DungeonBoss
import com.example.ui.components.NeonCard
import com.example.ui.components.StatProgressBar
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@Composable
fun DungeonBossScreen(
    viewModel: AriseViewModel
) {
    val battleState by viewModel.battleState.collectAsState()
    val profile by viewModel.playerProfile.collectAsState()
    val allBosses = remember { BossCatalog.allBosses }

    if (battleState.inBattle && battleState.currentBoss != null) {
        ActiveBossCombatView(viewModel = viewModel)
    } else {
        BossSelectionView(
            bosses = allBosses,
            playerLevel = profile?.level ?: 1,
            onSelectBoss = { boss -> viewModel.startBossBattle(boss) }
        )
    }
}

@Composable
fun BossSelectionView(
    bosses: List<DungeonBoss>,
    playerLevel: Int = 1,
    onSelectBoss: (DungeonBoss) -> Unit
) {
    var warningBossToEnter by remember { mutableStateOf<DungeonBoss?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AriseVoidBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NeonCard(
                borderColor = Color(0xFFEF4444),
                backgroundColor = Color(0xFF1F0D0D),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "DIMENSIONAL GATE INVASIONS",
                    color = Color(0xFFFCA5A5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Dungeon Boss Raids",
                    color = AriseTextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Conquer high-tier Gate sovereigns. Defeated bosses can be extracted into your Shadow Army via the command 'ARISE!'.",
                    color = AriseTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        item {
            Text(
                text = "AVAILABLE GATES & MONARCH TRIALS",
                color = AriseCyanNeon,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }

        items(bosses) { boss ->
            NeonCard(
                borderColor = Color(0xFF7F1D1D),
                backgroundColor = AriseSurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = boss.iconEmoji, fontSize = 34.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = boss.name,
                                color = AriseTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF7F1D1D))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = boss.rank,
                                    color = Color(0xFFFECACA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(text = boss.subtitle, color = AriseTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("HP: ${boss.maxHp}", color = AriseCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("ATK: ${boss.attack}", color = AriseGoldRank, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Weak: ${boss.weakness}", color = AriseCyanNeon, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extracts as: ${boss.shadowUnitTitle}",
                                color = AriseShadowViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val isUnderleveled = playerLevel < boss.recommendedLevel
                            Text(
                                text = if (isUnderleveled) "⚠️ Req. Lv.${boss.recommendedLevel}" else "✓ Req. Lv.${boss.recommendedLevel}",
                                color = if (isUnderleveled) Color(0xFFF59E0B) else AriseEmeraldHeal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val isUnderleveled = playerLevel < boss.recommendedLevel
                Button(
                    onClick = {
                        if (isUnderleveled) {
                            warningBossToEnter = boss
                        } else {
                            onSelectBoss(boss)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUnderleveled) Color(0xFF7F1D1D) else Color(0xFF991B1B)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isUnderleveled) "⚠️ CHALLENGE GATE (UNDERLEVELED)" else "ENTER DUNGEON GATE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    warningBossToEnter?.let { boss ->
        AlertDialog(
            onDismissRequest = { warningBossToEnter = null },
            title = {
                Text(
                    text = "⚠️ HIGH-RANK INCURSION WARNING",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = "Gate: '${boss.name}' (${boss.rank})\nRecommended Level: Lv.${boss.recommendedLevel}\nYour Hunter Level: Lv.$playerLevel\n\nFacing this sovereign while underleveled will deal extreme damage. Proceed into the gate?",
                    color = AriseTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosen = boss
                        warningBossToEnter = null
                        onSelectBoss(chosen)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("ENTER ANYWAY", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { warningBossToEnter = null }) {
                    Text("ABORT", color = AriseTextSecondary)
                }
            },
            containerColor = AriseDeepNavy
        )
    }
}

@Composable
fun ActiveBossCombatView(
    viewModel: AriseViewModel
) {
    val state by viewModel.battleState.collectAsState()
    val boss = state.currentBoss ?: return

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val listState = rememberLazyListState()

    LaunchedEffect(state.logMessages.size) {
        if (state.logMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.logMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AriseVoidBlack)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "GATE RAID IN PROGRESS", color = AriseCrimson, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(text = boss.name, color = AriseTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { viewModel.endBattle() }) {
                Icon(Icons.Default.Close, contentDescription = "Retreat", tint = AriseTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Boss Status Box
        NeonCard(
            borderColor = if (state.isBossEnraged) Color(0xFFFF0055) else Color(0xFFEF4444),
            backgroundColor = if (state.isBossEnraged) Color(0xFF29080E) else Color(0xFF1B0C0C),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = boss.iconEmoji, fontSize = 42.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = boss.subtitle, color = AriseTextSecondary, fontSize = 11.sp)
                        if (state.isBossEnraged) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFDC2626))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🔥 ENRAGED (+35% ATK)",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    if (state.isBossChargingUltimate) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF7F1D1D))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⚠️ CHARGING ULTIMATE: '${boss.bossSpecialAttackName}'! PARRY NOW!",
                                color = Color(0xFFFECACA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    StatProgressBar(
                        label = "BOSS HP: ${state.bossCurrentHp}/${boss.maxHp}",
                        currentValue = state.bossCurrentHp,
                        maxValue = boss.maxHp,
                        fillColor = if (state.isBossEnraged) Color(0xFFFF0055) else AriseCrimson
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hunter Status Box
        NeonCard(
            borderColor = AriseCyanNeon,
            backgroundColor = AriseDeepNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatProgressBar(
                    label = "HUNTER HP (${state.playerCurrentHp}/${state.playerMaxHp})",
                    currentValue = state.playerCurrentHp,
                    maxValue = state.playerMaxHp,
                    fillColor = AriseEmeraldHeal,
                    modifier = Modifier.weight(1f)
                )
                StatProgressBar(
                    label = "MANA MP (${state.playerCurrentMp}/${state.playerMaxMp})",
                    currentValue = state.playerCurrentMp,
                    maxValue = state.playerMaxMp,
                    fillColor = AriseBlueMana,
                    modifier = Modifier.weight(1f)
                )
            }
            if (state.isPlayerDefending) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🛡️ PARRY STANCE ACTIVE: Next incoming damage reduced by 70%!",
                    color = AriseCyanNeon,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Shadow Squadron on the Battlefield
        if (state.activeShadows.isNotEmpty()) {
            NeonCard(
                borderColor = AriseShadowViolet,
                backgroundColor = Color(0xFF140D26),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👑", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SHADOW SQUADRON (${state.activeShadows.count { it.isAlive }}/${state.activeShadows.size} ALIVE)",
                            color = Color(0xFFD8B4FE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "PASSIVE RECONSTITUTION VIA MP",
                        color = AriseGoldRank,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.activeShadows.forEach { shadow ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (shadow.isAlive) Color(0xFF1E1438) else Color(0xFF261019))
                                .border(
                                    1.dp,
                                    if (shadow.isAlive) AriseShadowViolet else Color(0xFF7F1D1D),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = shadow.iconEmoji, fontSize = 16.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (shadow.isAlive) Color(0xFF4C1D95) else Color(0xFF581C28))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (shadow.isAlive) "FIGHTING" else "DORMANT",
                                            color = if (shadow.isAlive) Color(0xFFE9D5FF) else Color(0xFFFCA5A5),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = shadow.name,
                                    color = if (shadow.isAlive) AriseTextPrimary else Color(0xFF9CA3AF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Origin: ${shadow.originRank}",
                                    color = Color(0xFFA78BFA),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                StatProgressBar(
                                    label = "${shadow.currentHp}/${shadow.maxHp}",
                                    currentValue = shadow.currentHp,
                                    maxValue = shadow.maxHp,
                                    fillColor = if (shadow.isAlive) AriseShadowViolet else Color(0xFF6B7280)
                                )

                                if (!shadow.isAlive) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = { viewModel.reconstituteShadowInBattle(shadow.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(26.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AriseShadowViolet),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = "⚡ ARISE (${shadow.mpReconstituteCost} MP)",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0A0F1A))
                .border(1.dp, AriseBorderGlow, RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.logMessages) { log ->
                    Text(
                        text = "• $log",
                        color = when {
                            log.contains("ARISE") -> AriseShadowViolet
                            log.contains("Hunter uses") -> AriseCyanNeon
                            log.contains("counterattacks") -> Color(0xFFFCA5A5)
                            log.contains("fallen") -> AriseGoldRank
                            else -> AriseTextSecondary
                        },
                        fontSize = 11.sp,
                        fontWeight = if (log.contains("ARISE") || log.contains("fallen")) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // EXTRACTION CEREMONY: "ARISE"
        if (state.extractionEligible && !state.isExtracted) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF2E1065), Color(0xFF0F0B1A))
                        )
                    )
                    .border(2.dp, AriseShadowViolet, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A DARK SHADOW ESSENCE GATHERS...",
                    color = Color(0xFFD8B4FE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Speak the Monarch's Sovereign Command",
                    color = AriseTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.performAriseExtraction() },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(54.dp)
                        .scale(pulseScale),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AriseShadowViolet
                    )
                ) {
                    Text(
                        text = "ARISE!",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                }
            }
        } else if (state.isExtracted) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "EXTRACTION COMPLETE! SOUL BOUND TO SHADOW ARMY",
                    color = AriseShadowViolet,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.endBattle() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AriseCyanNeon)
                ) {
                    Text("RETURN TO CITADEL", color = AriseVoidBlack, fontWeight = FontWeight.Bold)
                }
            }
        } else if (state.isDefeat) {
            Button(
                onClick = { viewModel.endBattle() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AriseCrimson)
            ) {
                Text("RETREAT & RESTORE STRENGTH", color = Color.White, fontWeight = FontWeight.Bold)
            }
        } else {
            // Action Grid (3-row tactical suite)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.executePlayerAttack("BASIC") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("⚔️ Basic Strike (0 MP)", color = AriseTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.executePlayerAttack("CLASS_SKILL") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("⚡ Class Skill (15 MP)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.executePlayerAttack("DEFEND") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🛡️ Steel Parry (-70% Dmg)", color = AriseEmeraldHeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.executePlayerAttack("HEAL") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14532D)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🧪 Elixir (+60 HP) [${state.healthPotionsRemaining}]", color = Color(0xFF86EFAC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.executePlayerAttack("SHADOW_SUMMON") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF581C87)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("👑 Shadows (25 MP)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.executePlayerAttack("ULTIMATE") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("💥 Monarch (40 MP)", color = AriseGoldRank, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (viewModel.hasConsumable("cons_elixir")) {
                    Button(
                        onClick = {
                            viewModel.executePlayerAttack("HEAL")
                            viewModel.consumeOne("cons_elixir")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14532D)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🧪 USE SHOP ELIXIR (+60 HP)", color = Color(0xFF86EFAC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (viewModel.hasConsumable("cons_tonic")) {
                    Button(
                        onClick = {
                            viewModel.executePlayerAttack("TONIC")
                            viewModel.consumeOne("cons_tonic")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🧪 USE MANA TONIC (+30 MP)", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
