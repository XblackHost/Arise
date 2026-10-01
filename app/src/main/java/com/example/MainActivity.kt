package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CelebrationModal
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

enum class AriseScreen(val label: String) {
    DASHBOARD("Citadel"),
    QUESTS("Missions"),
    DUNGEONS("Raids"),
    SHADOWS("Shadows"),
    NYX("NYX AI"),
    CHARACTER("Status"),
    CLASSES("Classes"),
    EXPLORATION("Explore"),
    MULTIPLAYER("Squadron"),
    INVENTORY("Inventory"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AriseTheme {
                val viewModel: AriseViewModel = viewModel()
                MainAriseApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAriseApp(viewModel: AriseViewModel) {
    val hasCompletedFirstLaunch by viewModel.hasCompletedFirstLaunch.collectAsState()
    val profile by viewModel.playerProfile.collectAsState()
    val celebrationEvent by viewModel.celebrationEvent.collectAsState()

    var currentScreen by rememberSaveable(
        stateSaver = Saver(
            save = { it.name },
            restore = { AriseScreen.valueOf(it) }
        )
    ) { mutableStateOf(AriseScreen.DASHBOARD) }

    // First Launch: Prompt for API Key and Secure it in Local Storage
    if (!hasCompletedFirstLaunch) {
        FirstLaunchApiKeyScreen(
            viewModel = viewModel,
            onContinue = {
                // Proceeds to Onboarding if profile is not yet completed
            }
        )
        return
    }

    // Onboarding if player hasn't forged initial identity
    if (profile != null && !profile!!.isOnboardingComplete) {
        OnboardingScreen(
            viewModel = viewModel,
            onComplete = {
                currentScreen = AriseScreen.DASHBOARD
            }
        )
        return
    }

    BackHandler(enabled = currentScreen != AriseScreen.DASHBOARD) {
        currentScreen = AriseScreen.DASHBOARD
    }

    // Main App with Navigation
    Scaffold(
        containerColor = AriseVoidBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            text = "ARISE",
                            color = AriseCyanNeon,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${currentScreen.label}",
                            color = AriseTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { currentScreen = AriseScreen.INVENTORY }) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Armory & Inventory",
                            tint = if (currentScreen == AriseScreen.INVENTORY) AriseGoldRank else AriseTextSecondary
                        )
                    }
                    IconButton(onClick = { currentScreen = AriseScreen.EXPLORATION }) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Radar GPS",
                            tint = if (currentScreen == AriseScreen.EXPLORATION) AriseCyanNeon else AriseTextSecondary
                        )
                    }
                    IconButton(onClick = { currentScreen = AriseScreen.MULTIPLAYER }) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Squadron Co-Op",
                            tint = if (currentScreen == AriseScreen.MULTIPLAYER) AriseCyanNeon else AriseTextSecondary
                        )
                    }
                    IconButton(onClick = { currentScreen = AriseScreen.SETTINGS }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (currentScreen == AriseScreen.SETTINGS) AriseCyanNeon else AriseTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AriseDeepNavy,
                    titleContentColor = AriseTextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = AriseDeepNavy,
                contentColor = AriseTextPrimary
            ) {
                NavigationBarItem(
                    selected = currentScreen == AriseScreen.DASHBOARD,
                    onClick = { currentScreen = AriseScreen.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Citadel", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AriseCyanNeon,
                        selectedTextColor = AriseCyanNeon,
                        indicatorColor = AriseSurfaceElevated,
                        unselectedIconColor = AriseTextSecondary,
                        unselectedTextColor = AriseTextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AriseScreen.QUESTS,
                    onClick = { currentScreen = AriseScreen.QUESTS },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                    label = { Text("Missions", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AriseCyanNeon,
                        selectedTextColor = AriseCyanNeon,
                        indicatorColor = AriseSurfaceElevated,
                        unselectedIconColor = AriseTextSecondary,
                        unselectedTextColor = AriseTextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AriseScreen.DUNGEONS,
                    onClick = { currentScreen = AriseScreen.DUNGEONS },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                    label = { Text("Raids", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFEF4444),
                        selectedTextColor = Color(0xFFEF4444),
                        indicatorColor = Color(0xFF2B1111),
                        unselectedIconColor = AriseTextSecondary,
                        unselectedTextColor = AriseTextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AriseScreen.SHADOWS,
                    onClick = { currentScreen = AriseScreen.SHADOWS },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                    label = { Text("Shadows", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AriseShadowViolet,
                        selectedTextColor = AriseShadowViolet,
                        indicatorColor = Color(0xFF25133E),
                        unselectedIconColor = AriseTextSecondary,
                        unselectedTextColor = AriseTextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AriseScreen.NYX,
                    onClick = { currentScreen = AriseScreen.NYX },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("NYX AI", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AriseShadowViolet,
                        selectedTextColor = AriseShadowViolet,
                        indicatorColor = Color(0xFF25133E),
                        unselectedIconColor = AriseTextSecondary,
                        unselectedTextColor = AriseTextSecondary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AriseScreen.CHARACTER,
                    onClick = { currentScreen = AriseScreen.CHARACTER },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Status", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AriseGoldRank,
                        selectedTextColor = AriseGoldRank,
                        indicatorColor = Color(0xFF2E2207),
                        unselectedIconColor = AriseTextSecondary,
                        unselectedTextColor = AriseTextSecondary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AriseScreen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToQuests = { currentScreen = AriseScreen.QUESTS },
                    onNavigateToStatus = { currentScreen = AriseScreen.CHARACTER },
                    onNavigateToBosses = { currentScreen = AriseScreen.DUNGEONS },
                    onNavigateToExploration = { currentScreen = AriseScreen.EXPLORATION },
                    onNavigateToNyx = { currentScreen = AriseScreen.NYX },
                    onNavigateToShadows = { currentScreen = AriseScreen.SHADOWS },
                    onNavigateToMultiplayer = { currentScreen = AriseScreen.MULTIPLAYER },
                    onNavigateToInventory = { currentScreen = AriseScreen.INVENTORY }
                )
                AriseScreen.QUESTS -> QuestsScreen(viewModel = viewModel)
                AriseScreen.DUNGEONS -> DungeonBossScreen(viewModel = viewModel)
                AriseScreen.SHADOWS -> ShadowArmyScreen(
                    viewModel = viewModel,
                    onNavigateToBossRaid = { currentScreen = AriseScreen.DUNGEONS }
                )
                AriseScreen.NYX -> NyxCompanionScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { currentScreen = AriseScreen.SETTINGS }
                )
                AriseScreen.CHARACTER -> CharacterScreen(
                    viewModel = viewModel,
                    onNavigateToClasses = { currentScreen = AriseScreen.CLASSES },
                    onNavigateToInventory = { currentScreen = AriseScreen.INVENTORY }
                )
                AriseScreen.CLASSES -> ClassesScreen(viewModel = viewModel)
                AriseScreen.EXPLORATION -> ExplorationScreen(
                    viewModel = viewModel,
                    onNavigateToBossBattle = { currentScreen = AriseScreen.DUNGEONS }
                )
                AriseScreen.MULTIPLAYER -> MultiplayerScreen(
                    viewModel = viewModel,
                    onNavigateToBossRaid = { currentScreen = AriseScreen.DUNGEONS }
                )
                AriseScreen.INVENTORY -> InventoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = AriseScreen.DASHBOARD }
                )
                AriseScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    CelebrationModal(
        message = celebrationEvent,
        onDismiss = { viewModel.dismissCelebration() }
    )
}
