package com.example.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeonCard
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstLaunchApiKeyScreen(
    viewModel: AriseViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var inputKey by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    val validationStatus by viewModel.keyValidationStatus.collectAsState()

    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    LaunchedEffect(Unit) {
        try {
            clipboardManager?.primaryClip?.let { clip ->
                if (clip.itemCount > 0) {
                    val text = clip.getItemAt(0)?.text?.toString()?.trim() ?: ""
                    if (text.startsWith("AIza") && text.length in 35..55) {
                        inputKey = text
                    }
                }
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        containerColor = AriseVoidBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Glowing Sigil Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(AriseCyanNeon.copy(alpha = 0.4f), AriseDeepNavy)
                        )
                    )
                    .border(2.dp, AriseCyanNeon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "System Key",
                    tint = AriseCyanNeon,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SYSTEM INITIALIZATION",
                style = MaterialTheme.typography.labelMedium,
                color = AriseCyanNeon,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Awaken the Core",
                style = MaterialTheme.typography.headlineMedium,
                color = AriseTextPrimary,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ARISE uses Google Gemini to manifest your sentient AI guide NYX, generate infinite personalized real-life quests, and orchestrate tactical boss raids.",
                style = MaterialTheme.typography.bodyMedium,
                color = AriseTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Guidance Card
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = AriseShadowViolet,
                backgroundColor = Color(0xFF131126)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Local Secure Storage",
                        tint = AriseShadowViolet,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Encrypted Local Storage",
                        color = AriseTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your API key is encrypted and stored locally in this device's private storage. It is never transmitted anywhere except directly to Google's Gemini API.",
                    color = AriseTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://aistudio.google.com/app/apikey")
                        )
                        context.startActivity(browserIntent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AriseCyanNeon),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AriseCyanNeon.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get Free Gemini Key (AI Studio)", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input Field
            OutlinedTextField(
                value = inputKey,
                onValueChange = { inputKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gemini API Key (e.g. AIzaSy...)") },
                placeholder = { Text("Paste your key here", color = AriseTextMuted) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (inputKey.isNotBlank()) {
                        isProcessing = true
                        viewModel.saveApiKey(inputKey) {
                            isProcessing = false
                            onContinue()
                        }
                    }
                }),
                trailingIcon = {
                    Row {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = AriseTextSecondary
                            )
                        }
                        IconButton(onClick = {
                            try {
                                clipboardManager?.primaryClip?.let { clip ->
                                    if (clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        if (text.isNotBlank()) inputKey = text.trim()
                                    }
                                }
                            } catch (e: Exception) {
                                // Safeguard against restricted OEM clipboard access on Android 12+
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = AriseCyanNeon
                            )
                        }
                    }
                },
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

            if (!validationStatus.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationStatus ?: "",
                    color = AriseCyanNeon,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Connect Button
            Button(
                onClick = {
                    if (inputKey.isNotBlank()) {
                        isProcessing = true
                        viewModel.saveApiKey(inputKey) {
                            isProcessing = false
                            onContinue()
                        }
                    } else {
                        viewModel.skipApiKeyFirstLaunch()
                        onContinue()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AriseCyanNeon),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AriseVoidBlack,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("SECURING IN LOCAL VAULT...", color = AriseVoidBlack, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AriseVoidBlack,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (inputKey.isNotBlank()) "SECURE KEY & AWAKEN SYSTEM" else "CONTINUE WITH KEY",
                        color = AriseVoidBlack,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Skip / Offline Mode Button
            TextButton(
                onClick = {
                    viewModel.skipApiKeyFirstLaunch()
                    onContinue()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continue in Offline / Prototype Mode (Enter key later in Settings)",
                    color = AriseTextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
