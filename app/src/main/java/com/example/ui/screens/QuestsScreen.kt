package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Quest
import com.example.data.model.QuestCategory
import com.example.data.model.QuestDifficulty
import com.example.data.model.VerificationType
import com.example.ui.components.NeonCard
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel
import kotlinx.coroutines.delay

@Composable
fun QuestsScreen(
    viewModel: AriseViewModel
) {
    val allQuests by viewModel.quests.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf<QuestCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAiQuestDialog by remember { mutableStateOf(false) }

    val filteredQuests = remember(allQuests, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) allQuests else allQuests.filter { it.category == selectedCategoryFilter }
    }

    Scaffold(
        containerColor = AriseVoidBlack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAiQuestDialog = true },
                containerColor = AriseCyanNeon,
                contentColor = AriseVoidBlack,
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text("AI Quest", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "QUEST LOG",
                        color = AriseCyanNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Daily Incursions",
                        color = AriseTextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }

                OutlinedButton(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AriseCyanNeon),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AriseCyanNeon)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("All Quests") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AriseCyanNeon,
                            selectedLabelColor = AriseVoidBlack
                        )
                    )
                }
                items(QuestCategory.values()) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text("${cat.iconEmoji} ${cat.displayName.split("&")[0]}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AriseCyanNeon,
                            selectedLabelColor = AriseVoidBlack
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quests List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                if (filteredQuests.isEmpty()) {
                    item {
                        NeonCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No quests in this category. Tap '+ Custom' or 'AI Quest' to generate one!",
                                color = AriseTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(filteredQuests, key = { it.id }) { quest ->
                        InteractiveQuestCard(
                            quest = quest,
                            onComplete = { viewModel.completeQuest(quest) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CustomQuestDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newQuest ->
                viewModel.addCustomQuest(newQuest)
                showAddDialog = false
            }
        )
    }

    if (showAiQuestDialog) {
        AiQuestDialog(
            onDismiss = { showAiQuestDialog = false },
            onGenerate = { prompt ->
                viewModel.generateAiDailyQuest(prompt)
                showAiQuestDialog = false
            }
        )
    }
}

@Composable
fun InteractiveQuestCard(
    quest: Quest,
    onComplete: () -> Unit
) {
    var timerRunning by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(quest.durationMinutes * 60) }

    LaunchedEffect(timerRunning) {
        while (timerRunning && secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
        if (secondsLeft == 0 && timerRunning) {
            timerRunning = false
            onComplete()
        }
    }

    val isFinished = quest.isCompleted
    val borderColor = if (isFinished) AriseEmeraldHeal else Color(quest.difficulty.colorHex)

    NeonCard(
        borderColor = borderColor,
        backgroundColor = if (isFinished) Color(0xFF0B1917) else AriseSurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = quest.category.iconEmoji, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = quest.category.displayName,
                    color = AriseTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(quest.difficulty.colorHex).copy(alpha = 0.2f))
                    .border(1.dp, Color(quest.difficulty.colorHex), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = quest.difficulty.rank,
                    color = Color(quest.difficulty.colorHex),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = quest.title,
            color = if (isFinished) AriseEmeraldHeal else AriseTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = quest.description,
            color = AriseTextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        if (!quest.bonusObjective.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "★ Bonus Objective: ${quest.bonusObjective}",
                color = AriseGoldRank,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Rewards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "+${quest.xpReward} EXP", color = AriseCyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(text = "+${quest.goldReward} Gold", color = AriseGoldRank, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "+${quest.attributeGain} ${quest.targetAttribute}", color = AriseShadowViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isFinished) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF064E3B))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AriseEmeraldHeal, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CONQUERED & REWARDED", color = AriseEmeraldHeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (quest.verificationType == VerificationType.TIMER) {
                    val minutes = secondsLeft / 60
                    val seconds = secondsLeft % 60
                    val timeStr = "%02d:%02d".format(minutes, seconds)

                    OutlinedButton(
                        onClick = { timerRunning = !timerRunning },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AriseCyanNeon),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AriseCyanNeon)
                    ) {
                        Icon(
                            imageVector = if (timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (timerRunning) "PAUSE ($timeStr)" else "START TIMER ($timeStr)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AriseEmeraldHeal)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AriseVoidBlack, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MARK COMPLETE", color = AriseVoidBlack, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun AiQuestDialog(
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit
) {
    var interestText by remember { mutableStateOf("Core calisthenics & 100 pushups") }

    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            borderColor = AriseCyanNeon,
            backgroundColor = AriseDeepNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "⚡ NYX QUEST DISPATCH",
                color = AriseCyanNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Materialize Daily Mission",
                color = AriseTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Tell NYX what real-world skill or workout you want to tackle today:",
                color = AriseTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = interestText,
                onValueChange = { interestText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriseCyanNeon,
                    unfocusedBorderColor = AriseBorderGlow,
                    focusedTextColor = AriseTextPrimary,
                    unfocusedTextColor = AriseTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = AriseTextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onGenerate(interestText) },
                    colors = ButtonDefaults.buttonColors(containerColor = AriseCyanNeon)
                ) {
                    Text("SYNTHESIZE", color = AriseVoidBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CustomQuestDialog(
    onDismiss: () -> Unit,
    onAdd: (Quest) -> Unit
) {
    var title by remember { mutableStateOf("Morning Sprint Intervals") }
    var description by remember { mutableStateOf("Run 5x 100m fast sprints with 60s rest.") }
    var duration by remember { mutableStateOf("15") }

    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            borderColor = AriseCyanNeon,
            backgroundColor = AriseDeepNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "FORGE CUSTOM MISSION",
                color = AriseCyanNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Objective / Task") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            val parsedDuration = duration.toIntOrNull()
            val isDurationValid = parsedDuration != null && parsedDuration in 1..360
            val isFormValid = title.isNotBlank() && isDurationValid

            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it.filter { char -> char.isDigit() } },
                label = { Text("Duration (minutes: 1 - 360)") },
                modifier = Modifier.fillMaxWidth(),
                isError = duration.isNotEmpty() && !isDurationValid,
                supportingText = {
                    if (duration.isNotEmpty() && !isDurationValid) {
                        Text("Duration must be between 1 and 360 minutes", color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = AriseTextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val dur = parsedDuration ?: 15
                        val q = Quest(
                            title = title.trim(),
                            description = description.trim(),
                            category = QuestCategory.FITNESS,
                            difficulty = QuestDifficulty.D,
                            xpReward = 60,
                            goldReward = 40,
                            targetAttribute = "STRENGTH",
                            attributeGain = 1,
                            durationMinutes = dur,
                            timerSecondsRemaining = dur * 60,
                            verificationType = VerificationType.TIMER,
                            isDaily = false
                        )
                        onAdd(q)
                    },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AriseCyanNeon,
                        disabledContainerColor = Color(0xFF1E293B)
                    )
                ) {
                    Text("ADD QUEST", color = if (isFormValid) AriseVoidBlack else AriseTextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
