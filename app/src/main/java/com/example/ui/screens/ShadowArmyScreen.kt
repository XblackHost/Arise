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
                ShadowCard(unit = unit)
            }
        }
    }
}

@Composable
fun ShadowCard(unit: ShadowUnit) {
    NeonCard(
        borderColor = AriseShadowViolet,
        backgroundColor = Color(0xFF110E1F),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
            Text(
                text = "Lv.${unit.level}",
                color = AriseCyanNeon,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
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
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("⚔️ ATK ${unit.attackPower}", color = AriseCyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("🛡️ DEF ${unit.defense}", color = AriseEmeraldHeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("💨 SPD ${unit.speed}", color = AriseGoldRank, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("❤️ Loyalty ${unit.loyalty}%", color = AriseShadowViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E1538))
                .padding(8.dp)
        ) {
            Text(
                text = "Signature: ${unit.signatureSkill} — ${unit.skillDescription}",
                color = Color(0xFFD8B4FE),
                fontSize = 11.sp
            )
        }
    }
}
