package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Quest
import com.example.ui.components.NeonCard
import com.example.ui.components.RankBadge
import com.example.ui.components.StatProgressBar
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@Composable
fun DashboardScreen(
    viewModel: AriseViewModel,
    onNavigateToQuests: () -> Unit,
    onNavigateToStatus: () -> Unit,
    onNavigateToBosses: () -> Unit,
    onNavigateToExploration: () -> Unit,
    onNavigateToNyx: () -> Unit,
    onNavigateToShadows: () -> Unit,
    onNavigateToMultiplayer: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {}
) {
    val profile by viewModel.playerProfile.collectAsState()
    val allQuests by viewModel.quests.collectAsState()
    val activeQuests = allQuests.filter { !it.isCompleted }.take(3)
    val shadows by viewModel.shadowArmy.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()
    val pendingTasks = allTasks.filter { !it.isCompleted }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AriseVoidBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hunter Status Card
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
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(AriseCyanNeon.copy(alpha = 0.5f), Color(0xFF0F172A))
                                )
                            )
                            .border(2.dp, AriseCyanNeon, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lv.${profile?.level ?: 1}",
                            fontWeight = FontWeight.Black,
                            color = AriseCyanNeon,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = profile?.name ?: "Hunter Jin",
                                style = MaterialTheme.typography.titleMedium,
                                color = AriseTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            RankBadge(rank = profile?.rank ?: "E-Rank")
                        }

                        Text(
                            text = "Class: ${profile?.selectedClass ?: "Warrior"} • Power: ${profile?.calculateTotalPower() ?: 0}",
                            color = AriseTextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🪙 ${profile?.gold ?: 0} Gold",
                                color = AriseGoldRank,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "💎 ${profile?.manaCrystals ?: 0} Crystals",
                                color = AriseBlueMana,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFFB923C),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${profile?.streakDays ?: 1}d Streak",
                                    color = Color(0xFFFB923C),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // XP Progress Bar
                StatProgressBar(
                    label = "EXP PROGRESSION",
                    currentValue = profile?.currentXp ?: 0,
                    maxValue = profile?.requiredXp ?: 100,
                    fillColor = AriseCyanNeon
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatProgressBar(
                        label = "HP",
                        currentValue = profile?.hp ?: 100,
                        maxValue = profile?.maxHp ?: 100,
                        fillColor = AriseEmeraldHeal,
                        modifier = Modifier.weight(1f)
                    )
                    StatProgressBar(
                        label = "MP",
                        currentValue = profile?.mp ?: 50,
                        maxValue = profile?.maxMp ?: 50,
                        fillColor = AriseBlueMana,
                        modifier = Modifier.weight(1f)
                    )
                }

                if ((profile?.unallocatedStatPoints ?: 0) > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3F2B06))
                            .border(1.dp, AriseGoldRank, RoundedCornerShape(8.dp))
                            .clickable { onNavigateToStatus() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚡ +${profile?.unallocatedStatPoints} Stat Points Available!",
                            color = AriseGoldRank,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ALLOCATE >",
                            color = AriseGoldRank,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Quick Incursion Actions (2-row grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToBosses,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Boss Raids", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNavigateToExploration,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GPS Radar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToShadows,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C1D95)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shadow Army (${shadows.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNavigateToMultiplayer,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Co-Op Squad", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Row 3: Hunter Armory & Vault
                Button(
                    onClick = onNavigateToInventory,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F2B06)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AriseGoldRank.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = AriseGoldRank, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hunter Armory & Equipment Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AriseGoldRank)
                }
            }
        }

        // NYX AI Companion Prompt Banner
        item {
            NeonCard(
                borderColor = AriseShadowViolet,
                backgroundColor = Color(0xFF13142B),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToNyx() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AriseShadowViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NYX • AI System Guide",
                            color = AriseShadowViolet,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "\"Awaiting your command, Hunter. Ready to forge personalized training or tactical analysis.\"",
                            color = AriseTextPrimary,
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Active Daily Quests Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE DAILY MISSIONS",
                    color = AriseCyanNeon,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = onNavigateToQuests) {
                    Text("VIEW ALL (${allQuests.size})", color = AriseTextSecondary, fontSize = 12.sp)
                }
            }
        }

        // Quest items preview
        if (activeQuests.isEmpty()) {
            item {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "All daily missions conquered! Tap 'View All' to create custom challenges or ask NYX to materialize new quests.",
                        color = AriseTextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(activeQuests) { quest ->
                QuestMiniCard(quest = quest, onComplete = { viewModel.completeQuest(quest) })
            }
        }

        // STILL-15: Today's Discipline Tasks Section
        if (pendingTasks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DISCIPLINE CHECKLIST (${pendingTasks.size})",
                        color = AriseGoldRank,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            items(pendingTasks) { task ->
                TaskMiniCard(
                    task = task,
                    onComplete = { viewModel.completeTask(task) }
                )
            }
        }
    }
}

@Composable
private fun TaskMiniCard(task: com.example.data.model.TaskItem, onComplete: () -> Unit) {
    NeonCard(
        borderColor = AriseGoldRank.copy(alpha = 0.4f),
        backgroundColor = AriseSurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AriseGoldRank,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = AriseTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        color = AriseTextSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎯 ${task.targetStat}", color = AriseCyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("+$${task.xpReward} XP", color = AriseEmeraldHeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("🪙 +${task.goldReward}G", color = AriseGoldRank, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(containerColor = AriseGoldRank),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("DONE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun QuestMiniCard(quest: Quest, onComplete: () -> Unit) {
    NeonCard(
        borderColor = AriseBorderGlow,
        backgroundColor = AriseSurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = quest.category.iconEmoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = quest.title,
                        color = AriseTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[${quest.difficulty.rank}]",
                        color = Color(quest.difficulty.colorHex),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Verification Type Badge
                val (verifyLabel, verifyColor) = when (quest.verificationType) {
                    com.example.data.model.VerificationType.TIMER -> "⏱️ Timer Verified" to AriseCyanNeon
                    com.example.data.model.VerificationType.STEPS -> "👟 Step Sensor Verified" to AriseEmeraldHeal
                    com.example.data.model.VerificationType.OPTIONAL_PROOF -> "📸 Proof Audit" to AriseShadowViolet
                    com.example.data.model.VerificationType.SELF_CONFIRMATION -> "📋 Hunter Verified" to AriseGoldRank
                }
                Text(
                    text = verifyLabel,
                    color = verifyColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = quest.description,
                    color = AriseTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "+${quest.xpReward} XP",
                        color = AriseCyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "+${quest.goldReward} Gold",
                        color = AriseGoldRank,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "+1 ${quest.targetAttribute}",
                        color = AriseShadowViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onComplete,
                colors = IconButtonDefaults.iconButtonColors(contentColor = AriseEmeraldHeal)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Complete Quest",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
