package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassCatalog
import com.example.ui.components.NeonCard
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: AriseViewModel,
    onComplete: () -> Unit
) {
    var hunterName by remember { mutableStateOf("Jin-Woo") }
    var selectedClassId by remember { mutableStateOf("warrior") }
    var selectedGoal by remember { mutableStateOf("Fitness & Calisthenics") }

    val starterClasses = remember {
        listOf(
            ClassCatalog.getById("warrior")!!,
            ClassCatalog.getById("assassin")!!,
            ClassCatalog.getById("mage")!!,
            ClassCatalog.getById("paladin")!!,
            ClassCatalog.getById("shadow_monarch")!!,
            ClassCatalog.getById("ranger")!!
        )
    }

    val goals = listOf(
        "Fitness & Calisthenics",
        "Deep Academic Study",
        "Exploration & Running",
        "Discipline & Mindfulness"
    )

    Scaffold(
        containerColor = AriseVoidBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "HUNTER AWAKENING RITUAL",
                color = AriseCyanNeon,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Forge Your Identity",
                color = AriseTextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Player Name Input
            OutlinedTextField(
                value = hunterName,
                onValueChange = { hunterName = it },
                label = { Text("Hunter Call-Sign / Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriseCyanNeon,
                    unfocusedBorderColor = AriseBorderGlow,
                    focusedTextColor = AriseTextPrimary,
                    unfocusedTextColor = AriseTextPrimary,
                    focusedContainerColor = AriseSurfaceDark,
                    unfocusedContainerColor = AriseSurfaceDark
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Choose Starting Class
            Text(
                text = "SELECT YOUR INITIAL CLASS",
                color = AriseShadowViolet,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                starterClasses.forEach { cls ->
                    val isSelected = selectedClassId == cls.id
                    val borderColor = if (isSelected) AriseCyanNeon else AriseBorderGlow
                    val bgColor = if (isSelected) Color(0xFF132238) else AriseSurfaceDark

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable { selectedClassId = cls.id }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = cls.iconEmoji, fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cls.name,
                                    color = if (isSelected) AriseCyanNeon else AriseTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${cls.coreAttribute}",
                                    color = AriseTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = cls.lore,
                                color = AriseTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AriseCyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Goal
            Text(
                text = "PRIMARY REAL-LIFE PROGRESSION FOCUS",
                color = AriseGoldRank,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.take(2).forEach { goal ->
                    val isSel = selectedGoal == goal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) Color(0xFF2E2207) else AriseSurfaceDark)
                            .border(1.dp, if (isSel) AriseGoldRank else AriseBorderGlow, RoundedCornerShape(10.dp))
                            .clickable { selectedGoal = goal }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = goal,
                            color = if (isSel) AriseGoldRank else AriseTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.drop(2).forEach { goal ->
                    val isSel = selectedGoal == goal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) Color(0xFF2E2207) else AriseSurfaceDark)
                            .border(1.dp, if (isSel) AriseGoldRank else AriseBorderGlow, RoundedCornerShape(10.dp))
                            .clickable { selectedGoal = goal }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = goal,
                            color = if (isSel) AriseGoldRank else AriseTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val chosen = starterClasses.find { it.id == selectedClassId }?.name ?: "Warrior"
                    viewModel.completeOnboarding(hunterName, chosen)
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AriseCyanNeon)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = AriseVoidBlack)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENTER THE SYSTEM",
                    color = AriseVoidBlack,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
