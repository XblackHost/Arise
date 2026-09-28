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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
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
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@Composable
fun InventoryScreen(
    viewModel: AriseViewModel
) {
    val profile by viewModel.playerProfile.collectAsState()
    val allItems by viewModel.equipment.collectAsState()

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
                borderColor = AriseGoldRank,
                backgroundColor = Color(0xFF1E1708),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "HUNTER VAULT & EQUIPMENT",
                            color = AriseGoldRank,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Artifact Armory",
                            color = AriseTextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF3F2B06))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "🪙 ${profile?.gold ?: 0}", color = AriseGoldRank, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0C4A6E))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "💎 ${profile?.manaCrystals ?: 0}", color = AriseBlueMana, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "INVENTORY ARTIFACTS",
                color = AriseCyanNeon,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }

        items(allItems) { item ->
            EquipmentCard(equipment = item)
        }
    }
}

@Composable
fun EquipmentCard(equipment: Equipment) {
    val rarityColor = Color(equipment.rarity.colorHex)

    NeonCard(
        borderColor = rarityColor.copy(alpha = 0.7f),
        backgroundColor = AriseSurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(rarityColor.copy(alpha = 0.2f))
                    .border(1.dp, rarityColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = rarityColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = equipment.name,
                        color = AriseTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(rarityColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = equipment.rarity.label,
                            color = rarityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Text(
                    text = "${equipment.slot.name.replace("_", " ")} • ${equipment.description}",
                    color = AriseTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (equipment.attackBonus > 0) Text("+${equipment.attackBonus} ATK", color = AriseCyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (equipment.defenseBonus > 0) Text("+${equipment.defenseBonus} DEF", color = AriseEmeraldHeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (equipment.hpBonus > 0) Text("+${equipment.hpBonus} HP", color = AriseGoldRank, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (equipment.speedBonus > 0) Text("+${equipment.speedBonus} SPD", color = AriseShadowViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (equipment.isEquipped) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF064E3B))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("EQUIPPED", color = AriseEmeraldHeal, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
