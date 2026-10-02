package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassExclusives
import com.example.data.ShopCatalog
import com.example.data.ShopCategory
import com.example.data.ShopItem
import com.example.ui.components.NeonCard
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@Composable
fun ShopScreen(viewModel: AriseViewModel) {
    val profile by viewModel.playerProfile.collectAsState()
    val owned by viewModel.equipment.collectAsState()
    val ownedCosmeticIds by viewModel.ownedCosmeticIds.collectAsState()
    val consumables by viewModel.consumables.collectAsState()
    val vow by viewModel.vowState.collectAsState()
    var filter by rememberSaveable { mutableStateOf<ShopCategory?>(null) }

    val vowDiscount = viewModel.vowDiscountPercent(vow?.longestStreak ?: 0)
    val gearDiscount = viewModel.permanentShopDiscount()
    val totalDiscount = (vowDiscount + gearDiscount).coerceAtMost(30)

    val visible: List<ShopItem> = remember(filter, profile?.selectedClass, totalDiscount) {
        val base = when (filter) {
            null -> ShopCatalog.allItems +
                listOf(ClassExclusives.forClass(profile?.selectedClass ?: "Warrior")) +
                ShopCatalog.featuredOfWeek()
            ShopCategory.FEATURED -> ShopCatalog.featuredOfWeek()
            ShopCategory.CLASS_EXCLUSIVE ->
                listOf(ClassExclusives.forClass(profile?.selectedClass ?: "Warrior"))
            else -> ShopCatalog.allItems.filter { it.category == filter }
        }
        base.distinctBy { it.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AriseVoidBlack).padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NeonCard(borderColor = AriseGoldRank, backgroundColor = Color(0xFF1E1708)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("⚒️ ARISE BLACKSMITH", color = AriseGoldRank,
                        fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🪙 ${profile?.gold ?: 0}", color = AriseGoldRank, fontWeight = FontWeight.Bold)
                        Text("💎 ${profile?.manaCrystals ?: 0}", color = AriseBlueMana, fontWeight = FontWeight.Bold)
                    }
                }
                if (totalDiscount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("Active discount: -$totalDiscount%", color = AriseEmeraldHeal, fontSize = 11.sp)
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filter == null,
                        onClick = { filter = null },
                        label = { Text("All") })
                }
                items(ShopCategory.values()) { cat ->
                    FilterChip(
                        selected = filter == cat,
                        onClick = { filter = cat },
                        label = { Text("${cat.icon} ${cat.label}") })
                }
            }
        }
        items(visible, key = { it.id }) { shopItem ->
            val isOwned = when {
                shopItem.isConsumable -> false
                shopItem.isCosmetic -> ownedCosmeticIds.contains(shopItem.id)
                else -> owned.any { it.name == shopItem.equipment.name }
            }
            val myCount = if (shopItem.isConsumable && shopItem.consumableId != null)
                consumables.find { it.itemId == shopItem.consumableId }?.count ?: 0
            else 0

            ShopItemCard(
                item = shopItem,
                playerLevel = profile?.level ?: 1,
                playerClass = profile?.selectedClass ?: "Warrior",
                gold = profile?.gold ?: 0,
                crystals = profile?.manaCrystals ?: 0,
                discountPercent = totalDiscount,
                isOwned = isOwned,
                ownedCount = myCount,
                onBuy = { viewModel.purchaseShopItem(shopItem) }
            )
        }
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItem,
    playerLevel: Int,
    playerClass: String,
    gold: Int,
    crystals: Int,
    discountPercent: Int,
    isOwned: Boolean,
    ownedCount: Int,
    onBuy: () -> Unit
) {
    val eq = item.equipment
    val rarityColor = Color(eq.rarity.colorHex)
    val levelOk = playerLevel >= item.requiredLevel
    val classOk = item.className == null || item.className.equals(playerClass, ignoreCase = true)
    val finalGold = item.goldPrice * (100 - discountPercent) / 100
    val currencyOk = gold >= finalGold && crystals >= item.crystalPrice
    val canBuy = !isOwned && levelOk && currencyOk && classOk

    NeonCard(
        borderColor = if (canBuy) rarityColor else AriseBorderGlow,
        backgroundColor = AriseSurfaceDark
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                    .background(rarityColor.copy(alpha = 0.2f))
                    .border(1.dp, rarityColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, null, tint = rarityColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(eq.name, color = AriseTextPrimary, fontWeight = FontWeight.Bold)
                    if (ownedCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text("×$ownedCount", color = AriseEmeraldHeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("[${eq.rarity.label}] • ${eq.slot.name.replace("_", " ")}",
                    color = rarityColor, fontSize = 11.sp)
                Text(eq.description, color = AriseTextSecondary, fontSize = 11.sp, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (eq.attackBonus > 0) Text("+${eq.attackBonus} ATK", color = AriseCyanNeon, fontSize = 11.sp)
                    if (eq.defenseBonus > 0) Text("+${eq.defenseBonus} DEF", color = AriseEmeraldHeal, fontSize = 11.sp)
                    if (eq.hpBonus > 0) Text("+${eq.hpBonus} HP", color = AriseGoldRank, fontSize = 11.sp)
                    if (eq.mpBonus > 0) Text("+${eq.mpBonus} MP", color = AriseBlueMana, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (item.goldPrice > 0) {
                    val showStrike = discountPercent > 0
                    if (showStrike) {
                        Text("🪙 ${item.goldPrice}", color = AriseTextMuted, fontSize = 11.sp,
                            style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                        Spacer(Modifier.width(2.dp))
                    }
                    Text("🪙 $finalGold",
                        color = if (gold >= finalGold) AriseGoldRank else AriseCrimson,
                        fontWeight = FontWeight.Bold)
                }
                if (item.crystalPrice > 0) Text("💎 ${item.crystalPrice}",
                    color = if (crystals >= item.crystalPrice) AriseBlueMana else AriseCrimson,
                    fontWeight = FontWeight.Bold)
                if (item.requiredLevel > 1) Text("Lv.${item.requiredLevel}",
                    color = if (levelOk) AriseTextSecondary else AriseCrimson, fontSize = 11.sp)
            }
            Button(
                onClick = onBuy,
                enabled = canBuy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = rarityColor,
                    disabledContainerColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    when {
                        isOwned -> "OWNED"
                        !classOk -> "LOCKED"
                        !levelOk -> "LOCKED"
                        !currencyOk -> "NO FUNDS"
                        else -> "BUY"
                    },
                    fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White
                )
            }
        }
    }
}
