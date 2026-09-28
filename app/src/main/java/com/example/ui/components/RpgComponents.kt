package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    borderColor: Color = AriseCyanNeon,
    backgroundColor: Color = AriseSurfaceDark,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatProgressBar(
    label: String,
    currentValue: Int,
    maxValue: Int,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (maxValue > 0) (currentValue.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f) else 0f
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AriseTextSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$currentValue / $maxValue",
                style = MaterialTheme.typography.labelSmall,
                color = AriseTextPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(fillColor.copy(alpha = 0.7f), fillColor)
                        )
                    )
            )
        }
    }
}

@Composable
fun RankBadge(rank: String, modifier: Modifier = Modifier) {
    val (badgeBg, textColor) = when (rank) {
        "Shadow Monarch" -> Pair(Color(0xFF4C1D95), Color(0xFFE9D5FF))
        "S-Rank" -> Pair(Color(0xFF7F1D1D), Color(0xFFFCA5A5))
        "A-Rank" -> Pair(Color(0xFF581C87), Color(0xFFD8B4FE))
        "B-Rank" -> Pair(Color(0xFF78350F), Color(0xFFFCD34D))
        "C-Rank" -> Pair(Color(0xFF064E3B), Color(0xFF6EE7B7))
        "D-Rank" -> Pair(Color(0xFF0C4A6E), Color(0xFF7DD3FC))
        else -> Pair(Color(0xFF1E293B), Color(0xFF94A3B8))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeBg)
            .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rank,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun CelebrationModal(
    message: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (message != null) {
            Dialog(onDismissRequest = onDismiss) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AriseDeepNavy),
                    border = BorderStroke(2.dp, AriseGoldRank)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚡ SYSTEM NOTICE ⚡",
                            color = AriseGoldRank,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = message,
                            color = AriseTextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = AriseGoldRank)
                        ) {
                            Text("ACKNOWLEDGE", color = Color(0xFF2D1E00), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecificationViewerDialog(
    specText: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AriseVoidBlack),
            border = BorderStroke(1.dp, AriseCyanNeon)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ARISE SPECIFICATION & LORE",
                        color = AriseCyanNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = onDismiss) {
                        Text("CLOSE", color = AriseTextSecondary)
                    }
                }
                HorizontalDivider(color = AriseBorderGlow, modifier = Modifier.padding(vertical = 8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = specText,
                        color = AriseTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
