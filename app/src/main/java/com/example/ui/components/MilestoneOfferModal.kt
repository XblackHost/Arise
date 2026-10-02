package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@Composable
fun MilestoneOfferModal(viewModel: AriseViewModel) {
    val offers by viewModel.activeOffers.collectAsState()
    val profile by viewModel.playerProfile.collectAsState()
    val top = offers.firstOrNull() ?: return

    val remaining = (top.expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
    val hours = remaining / 3_600_000L
    val mins = (remaining % 3_600_000L) / 60_000L

    Dialog(onDismissRequest = { /* must pick */ }) {
        NeonCard(borderColor = AriseGoldRank, backgroundColor = Color(0xFF1E1708)) {
            Text("🏆 MILESTONE OFFER", color = AriseGoldRank,
                fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(top.title, color = AriseTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(top.description, color = AriseTextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Text("Expires in ${hours}h ${mins}m", color = AriseCrimson,
                fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (top.goldCost > 0) Text("🪙 ${top.goldCost}", color = AriseGoldRank, fontWeight = FontWeight.Bold)
                if (top.crystalCost > 0) Text("💎 ${top.crystalCost}", color = AriseBlueMana, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.declineOffer(top.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AriseTextSecondary),
                    border = BorderStroke(1.dp, AriseBorderGlow)
                ) { Text("DECLINE") }
                Button(
                    onClick = { viewModel.claimOffer(top.id) },
                    modifier = Modifier.weight(1f),
                    enabled = (profile?.gold ?: 0) >= top.goldCost &&
                              (profile?.manaCrystals ?: 0) >= top.crystalCost,
                    colors = ButtonDefaults.buttonColors(containerColor = AriseGoldRank)
                ) { Text("CLAIM", color = Color.Black, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.height(8.dp))
            Text("Declined offers are gone forever.", color = AriseTextMuted, fontSize = 10.sp)
        }
    }
}
