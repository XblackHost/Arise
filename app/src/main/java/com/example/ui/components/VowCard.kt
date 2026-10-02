package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VowState
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@Composable
fun VowCard(viewModel: AriseViewModel) {
    val vow by viewModel.vowState.collectAsState()
    val v = vow

    NeonCard(
        borderColor = when {
            v == null || !v.isActive -> AriseShadowViolet
            v.isTimerExpired() -> AriseCrimson
            else -> AriseGoldRank
        },
        backgroundColor = Color(0xFF130E26)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "🛡️ VOW OF DISCIPLINE",
                color = AriseShadowViolet,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            if (v != null && v.isActive) {
                Text(
                    "Streak ${v.currentStreak} • Best ${v.longestStreak}",
                    color = AriseGoldRank,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            v == null || !v.isActive -> {
                Text(
                    "Take the 48-hour covenant. Confirm you have held the line before the timer expires. Reward fires at midnight.",
                    color = AriseTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.takeVow() },
                    colors = ButtonDefaults.buttonColors(containerColor = AriseShadowViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("TAKE THE VOW", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            v.isTimerExpired() -> {
                Text(
                    "Timer lapsed. Reset to begin a fresh window.",
                    color = AriseCrimson,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.resetVowTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = AriseEmeraldHeal),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("RESET TIMER", color = Color.Black, fontWeight = FontWeight.Black)
                    }

                    OutlinedButton(
                        onClick = { viewModel.abandonVow() },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, AriseBorderGlow)
                    ) {
                        Text("ABANDON", color = AriseTextSecondary, fontSize = 11.sp)
                    }
                }
            }
            else -> {
                var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
                LaunchedEffect(v.id) {
                    while (true) {
                        kotlinx.coroutines.delay(30_000L)
                        nowMs = System.currentTimeMillis()
                    }
                }
                val hours = ((VowState.WINDOW_MS - (nowMs - v.lastResetAt)) / 3_600_000L).coerceAtLeast(0L)
                val mins = (((VowState.WINDOW_MS - (nowMs - v.lastResetAt)) / 60_000L) % 60).coerceAtLeast(0L)
                Text(
                    "Window closes in ${hours}h ${mins}m.",
                    color = AriseTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.resetVowTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = AriseGoldRank),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("I HELD THE LINE", color = Color.Black, fontWeight = FontWeight.Black)
                    }

                    OutlinedButton(
                        onClick = { viewModel.abandonVow() },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, AriseBorderGlow)
                    ) {
                        Text("ABANDON", color = AriseTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
