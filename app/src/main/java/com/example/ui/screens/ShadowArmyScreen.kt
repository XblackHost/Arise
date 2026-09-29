package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShadowUnit
import com.example.ui.components.NeonCard
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@Composable
fun ShadowArmyScreen(
    viewModel: AriseViewModel,
    onNavigateToBossRaid: () -> Unit
) {
    val shadows by viewModel.shadowArmy.collectAsState()
    val totalPower = shadows.sumOf { it.attackPower + it.defense }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AriseVoidBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NeonCard(
                borderColor = AriseShadowViolet,
                backgroundColor = Color(0xFF140F26),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E1065))
                            .border(2.dp, AriseShadowViolet, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AriseShadowViolet,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SHADOW MONARCH'S LEGION",
                            color = AriseShadowViolet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "The Undying Army",
                            color = AriseTextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Recruits extracted via the command 'ARISE!'",
                            color = AriseTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF3B1D70))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL RECRUITS", color = AriseTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${shadows.size}", color = AriseShadowViolet, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LEGION POWER", color = AriseTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$totalPower", color = AriseCyanNeon, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ARMY SYNERGY", color = AriseTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("+30% Boost", color = AriseGoldRank, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Action banner to conquer more bosses
        item {
            Button(
                onClick = onNavigateToBossRaid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("BATTLE BOSSES & EXTRACT MORE SHADOWS", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Text(
                text = "ACTIVE SHADOW COMPANIONS",
                color = AriseShadowViolet,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }

        if (shadows.isEmpty()) {
            item {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "You haven't extracted any shadows yet. Challenge Dungeon Bosses and use the 'ARISE' command upon victory!",
                        color = AriseTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(shadows) { unit ->
                ShadowCard(
                    unit = unit,
                    onToggleDeploy = { viewModel.toggleDeployShadow(unit.id) },
                    onUpgrade = { viewModel.upgradeShadow(unit.id) }
                )
            }
        }
    }
}

@Composable
fun ShadowCard(
    unit: ShadowUnit,
    onToggleDeploy: () -> Unit,
    onUpgrade: () -> Unit
) {
    NeonCard(
        borderColor = if (unit.isDeployed) AriseShadowViolet else Color(0xFF374151),
        backgroundColor = if (unit.isDeployed) Color(0xFF140E26) else Color(0xFF0F111A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = unit.iconEmoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = unit.name,
                        color = AriseTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF4C1D95))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = unit.rank,
                            color = Color(0xFFE9D5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Text(
                    text = unit.title,
                    color = AriseShadowViolet,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Lv.${unit.level}",
                    color = AriseCyanNeon,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                if (unit.isDeployed) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF065F46))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⚔️ DEPLOYED",
                            color = Color(0xFF6EE7B7),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scaling & Origin Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E1538))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚖️ Scaled from Origin: ${unit.originRank}",
                    color = Color(0xFFD8B4FE),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "MP Respawn: ${unit.mpUpkeepCost} MP",
                    color = AriseGoldRank,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = unit.lore,
            color = AriseTextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("❤️ HP ${unit.maxHp}", color = AriseEmeraldHeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("⚔️ ATK ${unit.attackPower}", color = AriseCyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("🛡️ DEF ${unit.defense}", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("💨 SPD ${unit.speed}", color = AriseGoldRank, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1B1430))
                .padding(8.dp)
        ) {
            Text(
                text = "Signature: ${unit.signatureSkill} — ${unit.skillDescription}",
                color = Color(0xFFE9D5FF),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Actions: Deploy to Raid Squad & Ascend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onToggleDeploy,
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (unit.isDeployed) Color(0xFF3B185F) else Color(0xFF1E293B)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (unit.isDeployed) "⚔️ IN SQUADRON" else "➕ DEPLOY TO RAID",
                    color = if (unit.isDeployed) AriseGoldRank else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onUpgrade,
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C1D95)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "⚡ ASCEND (+10% STATS)",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
