package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.data.model.Equipment
import com.example.ui.components.NeonCard
import com.example.ui.components.RankBadge
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@Composable
fun CharacterScreen(
    viewModel: AriseViewModel,
    onNavigateToClasses: () -> Unit,
    onNavigateToInventory: () -> Unit = {}
) {
    val profile by viewModel.playerProfile.collectAsState()
    val equippedGear by viewModel.equipment.collectAsState()
    val equippedItems = equippedGear.filter { it.isEquipped }

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
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1B4B))
                            .border(2.dp, AriseCyanNeon, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lv.${profile?.level ?: 1}",
                            color = AriseCyanNeon,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = profile?.name ?: "Hunter",
                                color = AriseTextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            RankBadge(rank = profile?.rank ?: "E-Rank")
                        }
                        Text(
                            text = "Title: ${profile?.title ?: "The Awakened"}",
                            color = AriseGoldRank,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Class: ${profile?.selectedClass ?: "Warrior"}",
                                color = AriseShadowViolet,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onNavigateToClasses) {
                                Text("CHANGE CLASS >", color = AriseCyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = AriseBorderGlow)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL COMBAT POWER", color = AriseTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${profile?.calculateTotalPower() ?: 0}", color = AriseCyanNeon, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SHADOW LEGION", color = AriseTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${profile?.shadowArmyCount ?: 0} Units", color = AriseShadowViolet, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CONSISTENCY", color = AriseTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${profile?.streakDays ?: 1} Days", color = Color(0xFFFB923C), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Unallocated Points Banner
        item {
            val unallocated = profile?.unallocatedStatPoints ?: 0
            if (unallocated > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF3F2B06))
                        .border(1.dp, AriseGoldRank, RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ $unallocated Stat Points Unassigned!",
                            color = AriseGoldRank,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Tap [+] below to empower",
                            color = AriseTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Core Attributes Grid
        item {
            Text(
                text = "CORE RPG ATTRIBUTES",
                color = AriseCyanNeon,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }

        val unassignedPoints = profile?.unallocatedStatPoints ?: 0

        val attributes = listOf(
            Triple("STRENGTH", profile?.strength ?: 10, "Melee damage, heavy weapon mastery, physical endurance"),
            Triple("ENDURANCE", profile?.endurance ?: 10, "Stamina pool, damage mitigation, exploration pace"),
            Triple("AGILITY", profile?.agility ?: 10, "Evasion, attack velocity, critical strike multiplier"),
            Triple("INTELLIGENCE", profile?.intelligence ?: 10, "Max MP, magical potency, learning XP multiplier"),
            Triple("FOCUS", profile?.focus ?: 10, "Study durability, timer efficiency, tactical precision"),
            Triple("DISCIPLINE", profile?.discipline ?: 10, "Habit consistency, streak immunity, willpower resilience"),
            Triple("VITALITY", profile?.vitality ?: 10, "Max HP, health regeneration from sleep, bodily vitality")
        )

        items(attributes) { (name, value, desc) ->
            AttributeRowCard(
                name = name,
                value = value,
                description = desc,
                canIncrement = unassignedPoints > 0,
                onIncrement = { viewModel.allocateStat(name) }
            )
        }

        // Equipped Gear
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EQUIPPED HUNTER ARTIFACTS",
                    color = AriseCyanNeon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = onNavigateToInventory) {
                    Text("MANAGE ARMORY >", color = AriseGoldRank, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        if (equippedItems.isEmpty()) {
            item {
                NeonCard(
                    borderColor = AriseBorderGlow,
                    backgroundColor = AriseSurfaceDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No artifacts equipped. Visit the Armory to equip gear!",
                            color = AriseTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onNavigateToInventory,
                            colors = ButtonDefaults.buttonColors(containerColor = AriseGoldRank),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("ARMORY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        items(equippedItems) { gear ->
            NeonCard(
                borderColor = Color(gear.rarity.colorHex),
                backgroundColor = AriseSurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(gear.rarity.colorHex).copy(alpha = 0.2f))
                            .border(1.dp, Color(gear.rarity.colorHex), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(gear.rarity.colorHex))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = gear.name, color = AriseTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "[${gear.rarity.label}]", color = Color(gear.rarity.colorHex), fontSize = 11.sp)
                        }
                        Text(text = gear.description, color = AriseTextSecondary, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (gear.attackBonus > 0) Text("+${gear.attackBonus} ATK", color = AriseCyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (gear.defenseBonus > 0) Text("+${gear.defenseBonus} DEF", color = AriseEmeraldHeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (gear.hpBonus > 0) Text("+${gear.hpBonus} HP", color = AriseGoldRank, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributeRowCard(
    name: String,
    value: Int,
    description: String,
    canIncrement: Boolean,
    onIncrement: () -> Unit
) {
    NeonCard(
        borderColor = AriseBorderGlow,
        backgroundColor = AriseSurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = AriseTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$value",
                        color = AriseCyanNeon,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
                Text(
                    text = description,
                    color = AriseTextSecondary,
                    fontSize = 11.sp
                )
            }

            if (canIncrement) {
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AriseGoldRank)
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Point",
                        tint = Color(0xFF2D1E00)
                    )
                }
            }
        }
    }
}
