package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.components.NeonCard
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@Composable
fun NyxCompanionScreen(
    viewModel: AriseViewModel,
    onNavigateToSettings: () -> Unit
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isReplying by viewModel.isNyxReplying.collectAsState()
    val hasKey by viewModel.hasValidApiKey.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.lastOrNull()?.id) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "Suggest a 20-min bodyweight workout",
        "Give me strategic advice to defeat Igris",
        "Evaluate my current stats and recommend a build",
        "How can I build unstoppable discipline?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AriseVoidBlack)
            .padding(16.dp)
    ) {
        // Header
        NeonCard(
            borderColor = AriseShadowViolet,
            backgroundColor = Color(0xFF130E26),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AriseShadowViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NYX SYSTEM GUIDE",
                            color = AriseShadowViolet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (hasKey) "Gemini AI Neural Core: CONNECTED" else "Neural Core: OFFLINE MODE",
                            color = if (hasKey) AriseEmeraldHeal else AriseGoldRank,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!hasKey) {
                    TextButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = AriseCyanNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Key", color = AriseCyanNeon, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick prompts row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(quickPrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1B4B))
                        .border(1.dp, AriseShadowViolet.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { viewModel.sendChatMessage(prompt) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = prompt, color = Color(0xFFD8B4FE), fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }

            if (isReplying) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = AriseShadowViolet,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NYX is processing transmission...",
                            color = AriseTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = { Text("Consult with NYX...", color = AriseTextMuted, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriseShadowViolet,
                    unfocusedBorderColor = AriseBorderGlow,
                    focusedTextColor = AriseTextPrimary,
                    unfocusedTextColor = AriseTextPrimary,
                    focusedContainerColor = AriseSurfaceDark,
                    unfocusedContainerColor = AriseSurfaceDark
                ),
                shape = RoundedCornerShape(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputPrompt.isNotBlank()) {
                        val text = inputPrompt
                        inputPrompt = ""
                        viewModel.sendChatMessage(text)
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AriseShadowViolet)
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isNyx = message.sender.equals("NYX", ignoreCase = true)
    val alignment = if (isNyx) Alignment.Start else Alignment.End
    val bgColor = if (isNyx) Color(0xFF16112C) else Color(0xFF0F2338)
    val borderColor = if (isNyx) AriseShadowViolet.copy(alpha = 0.5f) else AriseCyanNeon.copy(alpha = 0.5f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Text(
            text = if (isNyx) "NYX" else "HUNTER",
            color = if (isNyx) AriseShadowViolet else AriseCyanNeon,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = AriseTextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
