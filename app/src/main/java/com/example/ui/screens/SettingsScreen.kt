package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.NeonCard
import com.example.ui.components.SpecificationViewerDialog
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@Composable
fun SettingsScreen(
    viewModel: AriseViewModel
) {
    val context = LocalContext.current
    val maskedKey by viewModel.maskedApiKey.collectAsState()
    val hasValidKey by viewModel.hasValidApiKey.collectAsState()

    var showKeyEditor by remember { mutableStateOf(false) }
    var showSpecDialog by remember { mutableStateOf(false) }
    var specContent by remember { mutableStateOf("") }

    LaunchedEffect(showSpecDialog) {
        if (showSpecDialog && specContent.isEmpty()) {
            specContent = try {
                context.assets.open("ARISE_SPECIFICATION.txt").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                "ARISE System Specification file loaded."
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AriseVoidBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "SYSTEM CONFIGURATION & VAULT",
                color = AriseCyanNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "Settings & Neural Core",
                color = AriseTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        }

        // Gemini AI API Key Vault Card
        item {
            NeonCard(
                borderColor = if (hasValidKey) AriseEmeraldHeal else AriseGoldRank,
                backgroundColor = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = if (hasValidKey) AriseEmeraldHeal else AriseGoldRank,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Gemini API Key Storage",
                            color = AriseTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (hasValidKey) "Status: Encrypted in local private storage" else "Status: Key not set (Using offline simulated mode)",
                            color = if (hasValidKey) AriseEmeraldHeal else AriseGoldRank,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF070B14))
                        .border(1.dp, AriseBorderGlow, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = maskedKey,
                        color = AriseTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showKeyEditor = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AriseCyanNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (hasValidKey) "UPDATE KEY" else "ENTER API KEY",
                            color = AriseVoidBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    if (hasValidKey) {
                        OutlinedButton(
                            onClick = { viewModel.clearApiKey() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AriseCrimson),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AriseCrimson),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("REMOVE", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Full Specification In-App Reader
        item {
            NeonCard(
                borderColor = AriseShadowViolet,
                backgroundColor = Color(0xFF130E26),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = AriseShadowViolet,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Full ARISE Specification",
                            color = AriseTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Complete game design document, lore, and mechanics",
                            color = AriseTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showSpecDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AriseShadowViolet),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("VIEW FULL PROMPT SPECIFICATION (TXT)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Accountability & Safety Rules Card
        item {
            NeonCard(
                borderColor = AriseBorderGlow,
                backgroundColor = AriseSurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "FAIR ACCOUNTABILITY SYSTEM",
                    color = AriseCyanNeon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Never removes large earned XP\n• Flexible grace periods for missed days\n• Recovery quests to rebuild streaks\n• No exercise as punishment\n• Offline-first data privacy",
                    color = AriseTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }

    if (showKeyEditor) {
        ApiKeyEditorDialog(
            onDismiss = { showKeyEditor = false },
            onSave = { newKey ->
                viewModel.saveApiKey(newKey) {
                    showKeyEditor = false
                }
            }
        )
    }

    if (showSpecDialog) {
        SpecificationViewerDialog(
            specText = specContent,
            onDismiss = { showSpecDialog = false }
        )
    }
}

@Composable
fun ApiKeyEditorDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    var keyInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            borderColor = AriseCyanNeon,
            backgroundColor = AriseDeepNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "UPDATE GEMINI API KEY",
                color = AriseCyanNeon,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your Google Gemini API key to update the secure local vault:",
                color = AriseTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("Gemini Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AriseCyanNeon,
                    unfocusedBorderColor = AriseBorderGlow,
                    focusedTextColor = AriseTextPrimary,
                    unfocusedTextColor = AriseTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                    context.startActivity(browserIntent)
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Get key at Google AI Studio", fontSize = 11.sp, color = AriseCyanNeon)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = AriseTextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onSave(keyInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = AriseCyanNeon)
                ) {
                    Text("SAVE TO VAULT", color = AriseVoidBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
