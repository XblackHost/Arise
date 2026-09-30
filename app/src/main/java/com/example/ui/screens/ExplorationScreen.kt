package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.RadarEngine
import com.example.data.model.SpawnedGate
import com.example.ui.components.NeonCard
import com.example.ui.components.PokemonGoOverworldMap
import com.example.ui.components.RankBadge
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExplorationScreen(
    viewModel: AriseViewModel,
    onNavigateToBossBattle: () -> Unit = {}
) {
    val context = LocalContext.current
    val playerProfile by viewModel.playerProfile.collectAsState()
    val isTracking by viewModel.isTrackingSteps.collectAsState()
    val steps by viewModel.sessionSteps.collectAsState()
    val distanceFeet by viewModel.sessionDistanceFeet.collectAsState()
    val distanceMeters by viewModel.sessionDistanceMeters.collectAsState()
    val burnedCalories by viewModel.burnedCalories.collectAsState()
    val hasGpsFix by viewModel.hasGpsFix.collectAsState()
    val currentLat by viewModel.currentLatitude.collectAsState()
    val currentLng by viewModel.currentLongitude.collectAsState()
    val spawnedGates by viewModel.spawnedGates.collectAsState()
    val selectedGate by viewModel.selectedGate.collectAsState()
    val radarEngine by viewModel.radarEngine.collectAsState()
    val radarRangeFeet by viewModel.radarRangeFeet.collectAsState()
    val sqliteCachedGateCount by viewModel.sqliteCachedGateCount.collectAsState()
    val playerBearing by viewModel.playerBearing.collectAsState()
    val walkingSpeedMps by viewModel.walkingSpeedMps.collectAsState()
    val isWalking by viewModel.isWalking.collectAsState()

    var explorationViewMode by remember { mutableStateOf("3D_MAP") }

    val hasActivityPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    } else true
    val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    var hasPermissions by remember {
        mutableStateOf(hasLocationPermission && hasActivityPermission)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val locGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val actGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            perms[Manifest.permission.ACTIVITY_RECOGNITION] == true
        } else true

        hasPermissions = locGranted && actGranted
        if (locGranted) {
            viewModel.toggleExplorationTracking()
        }
    }

    val km = distanceMeters / 1000f
    val miles = distanceFeet / 5280f

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            val permsToRequest = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }.toTypedArray()
            permissionLauncher.launch(permsToRequest)
        }
        if (!isTracking) {
            viewModel.toggleExplorationTracking()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AriseVoidBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // GPS & Sensor Permission Banner (if not yet granted)
        if (!hasPermissions) {
            item {
                NeonCard(
                    borderColor = AriseGoldRank,
                    backgroundColor = Color(0xFF261905),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationSearching,
                            contentDescription = null,
                            tint = AriseGoldRank,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GPS RADAR & SENSOR PERMISSIONS NEEDED",
                                color = AriseGoldRank,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "• Location (Fine GPS): Projects Dungeon Gates at real coordinates around you\n• Activity Recognition: Counts physical steps automatically like Pokémon GO",
                                color = AriseTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val permsToRequest = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ).apply {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    add(Manifest.permission.ACTIVITY_RECOGNITION)
                                }
                            }.toTypedArray()
                            permissionLauncher.launch(permsToRequest)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AriseGoldRank),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AUTHORIZE LOCATION & STEP SENSORS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Radar & Real-Time Walking Footstep Tracker Card
        item {
            NeonCard(
                borderColor = Color(0xFF10B981),
                backgroundColor = Color(0xFF0C1F1A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF064E3B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "DIMENSIONAL RADAR & RECON",
                                color = Color(0xFF6EE7B7),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (hasGpsFix) Color(0xFF065F46) else Color(0xFF334155))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (hasGpsFix) "GPS LOCKED" else "RADAR SIMULATOR",
                                    color = if (hasGpsFix) AriseEmeraldHeal else AriseTextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Territory Recon & Step Tracker",
                            color = AriseTextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF164E40))
                Spacer(modifier = Modifier.height(10.dp))

                // Primary Metrics: Walked Feet, Steps, Km/Miles, Calories
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WALKED FOOT", color = AriseTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "%.0f ft".format(distanceFeet),
                            color = AriseCyanNeon,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "%.2f mi".format(miles),
                            color = AriseTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("STEPS TAKEN", color = AriseTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$steps",
                            color = AriseEmeraldHeal,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "%.2f km".format(km),
                            color = AriseTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CALORIES", color = AriseTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "%.0f kcal".format(burnedCalories),
                            color = AriseGoldRank,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Active Burn",
                            color = AriseTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Real-Time Walking Speed, Cadence & Auto Step Sensor Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC06251E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isWalking) "🏃" else "🧍", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isWalking) "WALKING: %.1f m/s (%.1f km/h)".format(walkingSpeedMps, walkingSpeedMps * 3.6f) else "STEP SENSOR ACTIVE",
                                color = if (isWalking) AriseEmeraldHeal else AriseTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "HEADING: %.0f°".format(playerBearing),
                            color = AriseCyanNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scout Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleExplorationTracking() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTracking) AriseCrimson else AriseEmeraldHeal
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTracking) "PAUSE SCOUT" else "START RUN / WALK",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.respawnNearbyGates() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6EE7B7)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan New Gates", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Walk Distance Drills (Convenient increment buttons for testing or indoor drills)
                Text(
                    text = "MANUAL WALKING DRILLS (TEST INCREMENTS):",
                    color = AriseTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = { viewModel.addWalkedFeet(250f) },
                        label = { Text("+250 ft", fontSize = 11.sp, color = AriseCyanNeon) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = AriseDeepNavy)
                    )
                    SuggestionChip(
                        onClick = { viewModel.addWalkedFeet(500f) },
                        label = { Text("+500 ft", fontSize = 11.sp, color = AriseCyanNeon) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = AriseDeepNavy)
                    )
                    SuggestionChip(
                        onClick = { viewModel.addWalkedFeet(1000f) },
                        label = { Text("+1,000 ft", fontSize = 11.sp, color = AriseCyanNeon) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = AriseDeepNavy)
                    )
                    SuggestionChip(
                        onClick = { viewModel.addWalkedFeet(5280f) },
                        label = { Text("+1 Mile", fontSize = 11.sp, color = AriseGoldRank) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = AriseDeepNavy)
                    )
                }
            }
        }

        // Exploration Map View Mode Selector (Pokémon GO 3D vs Tactical Radar)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 3D Overworld (Pokémon GO)
                Button(
                    onClick = { explorationViewMode = "3D_MAP" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (explorationViewMode == "3D_MAP") AriseCyanNeon else Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = if (explorationViewMode == "3D_MAP") Color(0xFF040A14) else AriseTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "3D Map (Pokémon GO)",
                        color = if (explorationViewMode == "3D_MAP") Color(0xFF040A14) else AriseTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tactical Radar HUD
                Button(
                    onClick = { explorationViewMode = "RADAR_HUD" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (explorationViewMode == "RADAR_HUD") AriseCyanNeon else Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = if (explorationViewMode == "RADAR_HUD") Color(0xFF040A14) else AriseTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tactical Radar",
                        color = if (explorationViewMode == "RADAR_HUD") Color(0xFF040A14) else AriseTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Map View: 3D Pokémon GO Overworld or Tactical Radar HUD
        item {
            if (explorationViewMode == "3D_MAP") {
                PokemonGoOverworldMap(
                    spawnedGates = spawnedGates,
                    selectedGate = selectedGate,
                    playerProfile = playerProfile,
                    radarEngine = radarEngine,
                    radarRangeFeet = radarRangeFeet,
                    currentLat = currentLat,
                    currentLng = currentLng,
                    distanceFeet = distanceFeet,
                    steps = steps,
                    burnedCalories = burnedCalories,
                    playerBearing = playerBearing,
                    walkingSpeedMps = walkingSpeedMps,
                    isWalking = isWalking,
                    onSelectGate = { viewModel.selectRadarGate(it) },
                    onRaidGate = { gate ->
                        viewModel.raidSpawnedGate(gate) {
                            onNavigateToBossBattle()
                        }
                    },
                    onWalkFeet = { feet -> viewModel.addWalkedFeet(feet) },
                    onToggleRadarEngine = { viewModel.toggleRadarEngine() },
                    onSetRadarRange = { viewModel.setRadarRange(it) }
                )
            } else {
                InteractiveRadarCanvas(
                    radarEngine = radarEngine,
                    radarRangeFeet = radarRangeFeet,
                    sqliteCachedGateCount = sqliteCachedGateCount,
                    currentLat = currentLat,
                    currentLng = currentLng,
                    onToggleEngine = { viewModel.toggleRadarEngine() },
                    onSetRange = { viewModel.setRadarRange(it) },
                    spawnedGates = spawnedGates,
                    selectedGate = selectedGate,
                    onSelectGate = { viewModel.selectRadarGate(it) }
                )
            }
        }

        // Selected Gate Detailed Incursion Card
        selectedGate?.let { gate ->
            item {
                NeonCard(
                    borderColor = when (gate.rank) {
                        "E-Rank" -> AriseCyanNeon
                        "D-Rank" -> Color(0xFF38BDF8)
                        "C-Rank" -> AriseEmeraldHeal
                        "B-Rank" -> AriseShadowViolet
                        else -> Color(0xFFEF4444)
                    },
                    backgroundColor = Color(0xFF131B2E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = gate.boss.iconEmoji,
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = gate.name,
                                    color = AriseTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                RankBadge(rank = gate.rank)
                            }
                            Text(
                                text = "Boss: ${gate.boss.name} (${gate.boss.rank})",
                                color = AriseCyanNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Direction: ${gate.directionLabel} • Bearing: ${gate.bearingDegrees.toInt()}°",
                                color = AriseTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DISTANCE REMAINING", color = AriseTextSecondary, fontSize = 10.sp)
                            Text(
                                text = "%.0f feet (%.0f m)".format(gate.distanceFeet, gate.distanceMeters),
                                color = if (gate.isRaidable) AriseEmeraldHeal else AriseGoldRank,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("CLEAR REWARDS", color = AriseTextSecondary, fontSize = 10.sp)
                            Text(
                                text = "+${gate.xpReward} XP • +${gate.goldReward}G • +${gate.manaCrystalReward} Crystals",
                                color = AriseTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (gate.isRaidable) {
                        Button(
                            onClick = {
                                viewModel.raidSpawnedGate(gate) {
                                    onNavigateToBossBattle()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AriseCrimson),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🔥 IN RAID RANGE! ENTER GATE & ENGAGE BOSS",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.advanceTowardsSelectedGate(300f) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AriseCyanNeon),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AriseCyanNeon),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Walk +300 ft Toward Gate", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    // Walk remaining distance to close in
                                    val needed = gate.distanceFeet.coerceAtLeast(0f)
                                    viewModel.advanceTowardsSelectedGate(needed)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AriseDeepNavy),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = AriseCyanNeon, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sprint To Gate", fontSize = 11.sp, color = AriseCyanNeon)
                            }
                        }
                    }
                }
            }
        }

        // Spawned Gate List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEARBY SPAWNED GATES (${spawnedGates.size})",
                    color = AriseCyanNeon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Tap gate to lock-on radar",
                    color = AriseTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Spawned Gate Cards
        items(spawnedGates) { gate ->
            val isSelected = selectedGate?.id == gate.id
            NeonCard(
                borderColor = if (isSelected) AriseCyanNeon else if (gate.isRaidable) AriseEmeraldHeal else AriseBorderGlow,
                backgroundColor = if (isSelected) Color(0xFF1E293B) else AriseSurfaceDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectRadarGate(gate) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (gate.isRaidable) Color(0xFF064E3B) else Color(0xFF1E293B)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = gate.boss.iconEmoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = gate.name,
                                color = AriseTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            RankBadge(rank = gate.rank)
                        }
                        Text(
                            text = "${gate.directionLabel} • %.0f feet away (%.0f m)".format(gate.distanceFeet, gate.distanceMeters),
                            color = if (gate.isRaidable) AriseEmeraldHeal else AriseCyanNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Boss: ${gate.boss.name} • Weakness: ${gate.boss.weakness}",
                            color = AriseTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (gate.isRaidable) {
                        Button(
                            onClick = {
                                viewModel.raidSpawnedGate(gate) {
                                    onNavigateToBossBattle()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AriseCrimson),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("RAID", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.advanceTowardsGate(gate, 200f) },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AriseCyanNeon),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AriseCyanNeon.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("+200ft", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas drawing a high-tech tactical radar screen with concentric distance rings,
 * rotating scanner sweep, player center beacon, and interactive Gate blips!
 */
@Composable
fun InteractiveRadarCanvas(
    radarEngine: RadarEngine,
    radarRangeFeet: Float,
    sqliteCachedGateCount: Int,
    currentLat: Double,
    currentLng: Double,
    onToggleEngine: () -> Unit,
    onSetRange: (Float) -> Unit,
    spawnedGates: List<SpawnedGate>,
    selectedGate: SpawnedGate?,
    onSelectGate: (SpawnedGate) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Sweep"
    )

    NeonCard(
        borderColor = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) AriseCyanNeon else AriseEmeraldHeal,
        backgroundColor = Color(0xFF080F1D),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Dual Engine Selector Bar: "Online: MapLibre GL" vs "Offline: SQLite / WatermelonDB"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Online: MapLibre GL Tab
                Button(
                    onClick = { if (radarEngine != RadarEngine.ONLINE_MAPLIBRE) onToggleEngine() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) Color(0xFF0369A1) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) Color.White else AriseTextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Online: MapLibre GL",
                        color = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) Color.White else AriseTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Offline: SQLite / WatermelonDB Tab
                Button(
                    onClick = { if (radarEngine != RadarEngine.OFFLINE_SQLITE) onToggleEngine() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (radarEngine == RadarEngine.OFFLINE_SQLITE) Color(0xFF065F46) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = if (radarEngine == RadarEngine.OFFLINE_SQLITE) Color.White else AriseTextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Offline: SQLite",
                        color = if (radarEngine == RadarEngine.OFFLINE_SQLITE) Color.White else AriseTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Engine Telemetry Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) Color(0xFF082F49) else Color(0xFF064E3B))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) Icons.Default.GpsFixed else Icons.Default.Save,
                    contentDescription = null,
                    tint = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) AriseCyanNeon else AriseEmeraldHeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE)
                            "ENGINE: MapLibre GL Vector Tile Matrix"
                        else
                            "ENGINE: SQLite Local DB / WatermelonDB Spatial Cache",
                        color = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) AriseCyanNeon else AriseEmeraldHeal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE)
                            "Live Spatial Triangulation • Lat %.4f, Lng %.4f".format(currentLat, currentLng)
                        else
                            "$sqliteCachedGateCount Gate nodes indexed in SQLite • WatermelonDB Sync Adapter • 0ms Latency",
                        color = AriseTextSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Range Selector Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RADAR ZOOM:",
                    color = AriseTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(500f to "500 ft", 1000f to "1,000 ft", 2000f to "2,000 ft").forEach { (range, label) ->
                        val isSelected = radarRangeFeet == range
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) AriseDeepNavy else Color.Transparent)
                                .border(1.dp, if (isSelected) AriseCyanNeon else Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                .clickable { onSetRange(range) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) AriseCyanNeon else AriseTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Radar Canvas
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF040A14))
                    .border(
                        2.dp,
                        if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) AriseCyanNeon.copy(alpha = 0.7f) else AriseEmeraldHeal.copy(alpha = 0.7f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.width / 2f - 12.dp.toPx()

                    // Background grid styling based on engine
                    if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) {
                        // Vector street grid matrix lines
                        for (i in -3..3) {
                            val offset = i * (maxRadius / 3.5f)
                            drawLine(
                                color = AriseCyanNeon.copy(alpha = 0.08f),
                                start = Offset(center.x + offset, center.y - maxRadius),
                                end = Offset(center.x + offset, center.y + maxRadius),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = AriseCyanNeon.copy(alpha = 0.08f),
                                start = Offset(center.x - maxRadius, center.y + offset),
                                end = Offset(center.x + maxRadius, center.y + offset),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    } else {
                        // SQLite offline spatial database rings
                        drawCircle(
                            color = AriseEmeraldHeal.copy(alpha = 0.06f),
                            radius = maxRadius * 0.85f,
                            center = center,
                            style = Stroke(width = 8.dp.toPx())
                        )
                    }

                    // Draw concentric distance rings (scaled to radarRangeFeet)
                    val ringSteps = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                    val ringColor = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE)
                        AriseCyanNeon.copy(alpha = 0.25f)
                    else
                        AriseEmeraldHeal.copy(alpha = 0.25f)

                    ringSteps.forEach { fraction ->
                        drawCircle(
                            color = ringColor,
                            radius = maxRadius * fraction,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Crosshair axis lines
                    drawLine(
                        color = ringColor.copy(alpha = 0.2f),
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = ringColor.copy(alpha = 0.2f),
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw Rotating Radar Scanner Sweep
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val sweepEnd = Offset(
                        (center.x + maxRadius * cos(sweepRad)).toFloat(),
                        (center.y + maxRadius * sin(sweepRad)).toFloat()
                    )
                    drawLine(
                        color = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) AriseCyanNeon.copy(alpha = 0.6f) else AriseEmeraldHeal.copy(alpha = 0.6f),
                        start = center,
                        end = sweepEnd,
                        strokeWidth = 2.dp.toPx()
                    )

                    // Draw Player Center Dot
                    drawCircle(
                        color = Color(0xFF10B981),
                        radius = 6.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF6EE7B7),
                        radius = 2.dp.toPx(),
                        center = center
                    )

                    // Draw Gate Blips
                    spawnedGates.forEach { gate ->
                        // Scale distance to chosen radar range
                        val normDist = (gate.distanceFeet / radarRangeFeet).coerceIn(0.08f, 1.0f)
                        val blipRadius = maxRadius * normDist
                        val blipAngleRad = Math.toRadians((gate.bearingDegrees - 90).toDouble()) // 0 deg is North (up)

                        val blipX = (center.x + blipRadius * cos(blipAngleRad)).toFloat()
                        val blipY = (center.y + blipRadius * sin(blipAngleRad)).toFloat()
                        val blipCenter = Offset(blipX, blipY)

                        val blipColor = when (gate.rank) {
                            "E-Rank" -> AriseCyanNeon
                            "D-Rank" -> Color(0xFF38BDF8)
                            "C-Rank" -> AriseEmeraldHeal
                            "B-Rank" -> AriseShadowViolet
                            else -> Color(0xFFEF4444)
                        }

                        val isSel = selectedGate?.id == gate.id

                        if (isSel) {
                            drawCircle(
                                color = blipColor.copy(alpha = 0.5f),
                                radius = 12.dp.toPx(),
                                center = blipCenter,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        drawCircle(
                            color = blipColor,
                            radius = if (gate.isRaidable) 7.dp.toPx() else 5.dp.toPx(),
                            center = blipCenter
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Radar Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                RadarLegendItem(color = AriseCyanNeon, label = "E-Rank")
                RadarLegendItem(color = Color(0xFF38BDF8), label = "D-Rank")
                RadarLegendItem(color = AriseEmeraldHeal, label = "C-Rank")
                RadarLegendItem(color = AriseShadowViolet, label = "B-Rank")
                RadarLegendItem(color = Color(0xFFEF4444), label = "A-Rank")
            }
        }
    }
}

@Composable
fun RadarLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = AriseTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
