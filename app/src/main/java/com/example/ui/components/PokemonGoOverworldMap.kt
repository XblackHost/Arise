package com.example.ui.components

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RadarEngine
import com.example.data.model.PlayerProfile
import com.example.data.model.SpawnedGate
import com.example.ui.theme.*
import kotlin.math.*

/**
 * 3D Perspective Pokémon GO Overworld Map
 * Features:
 * - 3D angled camera with horizon, sky gradients, and floating clouds
 * - City street grid with asphalt avenues, cross streets, park polygons, waterways, and 3D buildings
 * - Hunter avatar at the road intersection with animated walk bob and pulsating 165ft raid interaction circle
 * - Towering Dungeon Gym Spires with holographic boss crowns, raid light beams, and floating Mana Crystal PokéStop portals
 * - Navigational chevrons along the avenue leading to the selected Dungeon Gate
 * - Interactive tap-to-select on any Dungeon Spire, camera rotation drag, zoom controls, and Pokémon GO style bottom HUD
 */
@Composable
fun PokemonGoOverworldMap(
    spawnedGates: List<SpawnedGate>,
    selectedGate: SpawnedGate?,
    playerProfile: PlayerProfile?,
    radarEngine: RadarEngine,
    radarRangeFeet: Float,
    currentLat: Double,
    currentLng: Double,
    distanceFeet: Float = 0f,
    steps: Int = 0,
    burnedCalories: Float = 0f,
    playerBearing: Float = 0f,
    walkingSpeedMps: Float = 0f,
    isWalking: Boolean = false,
    onSelectGate: (SpawnedGate) -> Unit,
    onRaidGate: (SpawnedGate) -> Unit,
    onWalkFeet: (Float) -> Unit,
    onToggleRadarEngine: () -> Unit,
    onSetRadarRange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Camera heading (in degrees) - user can drag to rotate around player
    var cameraHeading by remember { mutableFloatStateOf(0f) }
    var cameraPitch by remember { mutableFloatStateOf(1.0f) } // zoom / pitch multiplier
    var isFollowHeadingMode by remember { mutableStateOf(false) }

    // Follow Mode: camera smoothly locks behind player's real-life walking heading (like Pokémon GO)
    LaunchedEffect(playerBearing, isFollowHeadingMode) {
        if (isFollowHeadingMode) {
            cameraHeading = playerBearing
        }
    }

    // Auto-walk simulation toggle for hands-free scouting
    var isAutoWalking by remember { mutableStateOf(false) }

    LaunchedEffect(isAutoWalking) {
        while (isAutoWalking) {
            kotlinx.coroutines.delay(350L)
            onWalkFeet(4f)
        }
    }

    // Selected Gate Inspection Modal
    var inspectionGate by remember { mutableStateOf<SpawnedGate?>(null) }

    // Infinite animation loops for pulse rings, tower beams, clouds, and float effects
    val infiniteTransition = rememberInfiniteTransition(label = "OverworldLoops")

    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRing"
    )

    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Clouds"
    )

    val towerGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TowerGlow"
    )

    val avatarBob by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AvatarBob"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(440.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(1.5.dp, AriseCyanNeon.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        // Main 3D Canvas Rendering
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Dragging left/right orbits camera
                        cameraHeading = (cameraHeading - dragAmount.x * 0.35f + 360f) % 360f
                    }
                }
                .pointerInput(spawnedGates, cameraHeading, cameraPitch) {
                    detectTapGestures { tapOffset ->
                        // Check if tap hit any of the projected dungeon towers
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val vpY = h * 0.22f
                        val playerY = h * 0.78f
                        val cameraZ = 300f * cameraPitch

                        var tappedGate: SpawnedGate? = null
                        var minDistanceSq = 45f * 45f // tap radius in px

                        for (gate in spawnedGates) {
                            val relAngle = (gate.bearingDegrees - cameraHeading + 540f) % 360f - 180f
                            val angleRad = Math.toRadians(relAngle.toDouble())
                            val dist = gate.distanceFeet

                            val gz = (dist * cos(angleRad)).toFloat()
                            val gx = (dist * sin(angleRad)).toFloat()

                            if (gz > -40f) {
                                val depth = gz + cameraZ
                                val t = (cameraZ / depth).coerceIn(0.04f, 1.2f)
                                val screenY = vpY + (playerY - vpY) * t
                                val screenX = (w / 2f) + (gx * 0.9f) * t

                                // Tower extends upward from base
                                val towerHeight = 110f * t
                                val towerCenter = Offset(screenX, screenY - towerHeight * 0.5f)

                                val dx = tapOffset.x - towerCenter.x
                                val dy = tapOffset.y - towerCenter.y
                                val distSq = dx * dx + dy * dy
                                if (distSq < minDistanceSq) {
                                    minDistanceSq = distSq
                                    tappedGate = gate
                                }
                            }
                        }

                        if (tappedGate != null) {
                            onSelectGate(tappedGate)
                            inspectionGate = tappedGate
                        } else if (tapOffset.y >= vpY) {
                            // Tap on road or terrain! Walk character forward towards tap location
                            val forwardT = ((playerY - tapOffset.y) / (playerY - vpY)).coerceIn(0.1f, 1.0f)
                            val walkAdvance = (forwardT * 120f).coerceIn(25f, 200f)
                            onWalkFeet(walkAdvance)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Sky & Curved Horizon
            val horizonY = h * 0.22f
            drawSkyAndHorizon(w, horizonY, cloudOffset)

            // Dynamic road scroll offset driven by distance walked
            val walkScrollOffset = (distanceFeet * 0.05f) % 1.0f
            val isActivelyWalking = isWalking || isAutoWalking || walkingSpeedMps > 0.25f
            val walkCycle = if (isActivelyWalking) {
                (distanceFeet * 1.5f) + (System.currentTimeMillis() / 140f)
            } else {
                distanceFeet * 1.5f
            }
            val avatarFacingAngle = if (isFollowHeadingMode) 0f else ((playerBearing - cameraHeading + 540f) % 360f - 180f)

            // 2. 3D Ground Terrain, Street Grids, Waterways & Parcels
            drawOverworldTerrain(
                w = w,
                h = h,
                horizonY = horizonY,
                cameraHeading = cameraHeading,
                cameraPitch = cameraPitch,
                scrollOffset = walkScrollOffset
            )

            // 3. Navigation Arrows leading to selected Dungeon Gate
            selectedGate?.let { target ->
                drawNavigationRoute(
                    w = w,
                    h = h,
                    horizonY = horizonY,
                    gate = target,
                    cameraHeading = cameraHeading,
                    cameraPitch = cameraPitch
                )
            }

            // 4. Player Interaction Pulse Rings (165ft raid circle) & Hunter Avatar
            val playerPos = Offset(w / 2f, h * 0.78f)
            drawPlayerAvatarAndPulse(
                playerPos = playerPos,
                pulseFraction = pulseFraction,
                avatarBob = avatarBob,
                walkCycle = walkCycle,
                avatarRotationDegrees = avatarFacingAngle,
                isGateNearby = selectedGate?.isRaidable == true
            )

            // 5. Towering Dungeon Gym Spires & Gate Portals
            drawDungeonTowers(
                w = w,
                h = h,
                horizonY = horizonY,
                gates = spawnedGates,
                selectedGate = selectedGate,
                cameraHeading = cameraHeading,
                cameraPitch = cameraPitch,
                towerGlow = towerGlow
            )
        }

        // Top Header Controls: Mode Indicator, Live Step Counter, and Compass Rose
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Engine Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC091220),
                border = androidx.compose.foundation.BorderStroke(1.dp, AriseCyanNeon.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onToggleRadarEngine() }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) Icons.Default.Public else Icons.Default.Storage,
                        contentDescription = null,
                        tint = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) AriseCyanNeon else AriseEmeraldHeal,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (radarEngine == RadarEngine.ONLINE_MAPLIBRE) "MapLibre" else "Offline",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Live Step Counter & Walked Distance Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xEE064E3B),
                border = androidx.compose.foundation.BorderStroke(1.dp, AriseEmeraldHeal)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = AriseEmeraldHeal,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$steps STEPS • ${distanceFeet.toInt()} FT",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Compass Rose with Pokémon GO Follow Mode Toggle
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xDD091220),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isFollowHeadingMode) AriseEmeraldHeal else AriseCyanNeon
                ),
                modifier = Modifier.clickable {
                    if (!isFollowHeadingMode) {
                        isFollowHeadingMode = true
                        cameraHeading = playerBearing
                    } else {
                        isFollowHeadingMode = false
                        cameraHeading = 0f
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Compass",
                        tint = if (isFollowHeadingMode) AriseEmeraldHeal else AriseCrimson,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(-cameraHeading)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFollowHeadingMode) "FOLLOW" else "NORTH",
                        color = if (isFollowHeadingMode) AriseEmeraldHeal else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Camera Quick Zoom & Walk Controls (Floating on right side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Auto-Walk toggle button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isAutoWalking) AriseEmeraldHeal else Color(0xDD091220),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isAutoWalking) Color.White else AriseEmeraldHeal),
                modifier = Modifier.clickable { isAutoWalking = !isAutoWalking }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAutoWalking) Icons.AutoMirrored.Filled.DirectionsRun else Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = "Auto Walk",
                        tint = if (isAutoWalking) Color(0xFF040A14) else AriseEmeraldHeal,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isAutoWalking) "AUTO ON" else "AUTO",
                        color = if (isAutoWalking) Color(0xFF040A14) else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            FloatingMapButton(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                onClick = { onWalkFeet(50f) },
                contentDesc = "Walk 50ft",
                badgeText = "+50ft"
            )
            FloatingMapButton(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                onClick = { onWalkFeet(200f) },
                contentDesc = "Sprint 200ft",
                badgeText = "+200ft"
            )
            FloatingMapButton(
                icon = Icons.Default.Add,
                onClick = { cameraPitch = (cameraPitch * 0.85f).coerceAtLeast(0.6f) },
                contentDesc = "Zoom In"
            )
            FloatingMapButton(
                icon = Icons.Default.Remove,
                onClick = { cameraPitch = (cameraPitch * 1.15f).coerceAtMost(1.8f) },
                contentDesc = "Zoom Out"
            )
        }

        // Pokémon GO Bottom HUD: Player Profile, Arise Action Orb, and Nearby Dungeon Tracker
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bottom-Left: Hunter Profile Bubble
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xDD091220),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AriseGoldRank)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AriseShadowViolet),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👑", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = playerProfile?.name ?: "Hunter",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Lv.${playerProfile?.level ?: 1} ${playerProfile?.selectedClass ?: "Mage"}",
                                color = AriseGoldRank,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Bottom-Center: Large Glowing Arise Hunter Core (Pokéball Style)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(AriseCyanNeon, Color(0xFF0369A1), Color(0xFF0F172A))
                            )
                        )
                        .border(2.5.dp, Color.White, CircleShape)
                        .clickable {
                            selectedGate?.let { gate ->
                                if (gate.isRaidable) {
                                    onRaidGate(gate)
                                } else {
                                    inspectionGate = gate
                                }
                            } ?: run {
                                inspectionGate = spawnedGates.firstOrNull()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedGate?.isRaidable == true) Icons.Default.Bolt else Icons.Default.Explore,
                        contentDescription = "Raid / Scan",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Bottom-Right: Nearby Dungeons Tracker Bar
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD091220),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AriseCyanNeon.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                // Cycle to next nearby gate
                                val curIndex = spawnedGates.indexOfFirst { it.id == selectedGate?.id }
                                val nextGate = spawnedGates.getOrNull((curIndex + 1) % spawnedGates.size)
                                nextGate?.let { onSelectGate(it) }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        spawnedGates.take(3).forEach { gate ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (gate.id == selectedGate?.id) AriseCyanNeon.copy(alpha = 0.3f) else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(gate.boss.iconEmoji, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Radar,
                            contentDescription = null,
                            tint = AriseCyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Inspection / Raid Dialog Sheet for Tapped Dungeon Spire
        AnimatedVisibility(
            visible = inspectionGate != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            inspectionGate?.let { gate ->
                DungeonGateInspectionCard(
                    gate = gate,
                    onDismiss = { inspectionGate = null },
                    onRaid = {
                        inspectionGate = null
                        onRaidGate(gate)
                    },
                    onSprintTo = {
                        onWalkFeet(gate.distanceFeet.coerceAtLeast(0f))
                    }
                )
            }
        }
    }
}

@Composable
private fun FloatingMapButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    contentDesc: String,
    badgeText: String? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xDD091220),
        border = androidx.compose.foundation.BorderStroke(1.dp, AriseCyanNeon.copy(alpha = 0.6f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = AriseCyanNeon,
                modifier = Modifier.size(18.dp)
            )
            badgeText?.let {
                Spacer(modifier = Modifier.width(4.dp))
                Text(it, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Pokémon GO Style Dungeon / Gym Raid Inspection Sheet
 */
@Composable
private fun DungeonGateInspectionCard(
    gate: SpawnedGate,
    onDismiss: () -> Unit,
    onRaid: () -> Unit,
    onSprintTo: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF20B132B)),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            if (gate.isRaidable) AriseCrimson else AriseCyanNeon
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (gate.isRaidable) Color(0xFF7F1D1D) else Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(gate.boss.iconEmoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = gate.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Raid Boss: ${gate.boss.name}",
                            color = AriseCyanNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = AriseTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("DISTANCE", color = AriseTextSecondary, fontSize = 9.sp)
                    Text(
                        text = "%.0f ft (%.0f m)".format(gate.distanceFeet, gate.distanceMeters),
                        color = if (gate.isRaidable) AriseEmeraldHeal else AriseGoldRank,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
                Column {
                    Text("DIRECTION", color = AriseTextSecondary, fontSize = 9.sp)
                    Text(
                        text = gate.directionLabel,
                        color = AriseTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("REWARDS", color = AriseTextSecondary, fontSize = 9.sp)
                    Text(
                        text = "+${gate.xpReward} XP • +${gate.goldReward}G",
                        color = AriseCyanNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (gate.isRaidable) {
                Button(
                    onClick = onRaid,
                    colors = ButtonDefaults.buttonColors(containerColor = AriseCrimson),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ENTER DUNGEON RAID & BATTLE BOSS",
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
                        onClick = onSprintTo,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AriseCyanNeon),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AriseCyanNeon),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sprint To Dungeon", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Track On Map", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// DRAWING ROUTINES FOR 3D PERSPECTIVE OVERWORLD
// -------------------------------------------------------------------------------------------------

private fun DrawScope.drawSkyAndHorizon(w: Float, horizonY: Float, cloudOffset: Float) {
    // Sky gradient from bright azure blue to soft horizon haze
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF38BDF8),
                Color(0xFF7DD3FC),
                Color(0xFFBAE6FD)
            ),
            startY = 0f,
            endY = horizonY
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w, horizonY)
    )

    // Curved horizon line (planetary curvature)
    val horizonPath = Path().apply {
        moveTo(0f, horizonY + 6f)
        quadraticTo(w * 0.5f, horizonY - 14f, w, horizonY + 6f)
        lineTo(w, horizonY + 20f)
        lineTo(0f, horizonY + 20f)
        close()
    }
    drawPath(horizonPath, Color(0xFF86EFAC).copy(alpha = 0.5f))

    // Puffy stylized clouds drifting along horizon
    val cloudCount = 5
    for (i in 0 until cloudCount) {
        val baseCloudX = (w * (i.toFloat() / cloudCount) + cloudOffset * w) % (w + 120f) - 60f
        val cloudY = horizonY * (0.35f + (i % 3) * 0.18f)
        val cloudWidth = 64f + (i % 2) * 28f
        val cloudHeight = 16f

        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(baseCloudX, cloudY),
            size = Size(cloudWidth, cloudHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = 12f,
            center = Offset(baseCloudX + cloudWidth * 0.4f, cloudY - 4f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = 16f,
            center = Offset(baseCloudX + cloudWidth * 0.65f, cloudY - 6f)
        )
    }
}

private fun DrawScope.drawOverworldTerrain(
    w: Float,
    h: Float,
    horizonY: Float,
    cameraHeading: Float,
    cameraPitch: Float,
    scrollOffset: Float = 0f
) {
    val vp = Offset(w * 0.5f, horizonY)
    val groundHeight = h - horizonY

    // Base ground: lush Pokémon GO mint green terrain
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF6EE7B7), // Mint green in distance
                Color(0xFF34D399), // Richer grass green near foreground
                Color(0xFF10B981)
            ),
            startY = horizonY,
            endY = h
        ),
        topLeft = Offset(0f, horizonY),
        size = Size(w, groundHeight)
    )

    // Deep park green parcel polygons on sides
    val leftPark = Path().apply {
        moveTo(0f, horizonY + 20f)
        lineTo(w * 0.30f, horizonY + 60f)
        lineTo(w * 0.22f, h)
        lineTo(0f, h)
        close()
    }
    drawPath(leftPark, Color(0xFF059669).copy(alpha = 0.35f))

    val rightPark = Path().apply {
        moveTo(w, horizonY + 25f)
        lineTo(w * 0.72f, horizonY + 70f)
        lineTo(w * 0.82f, h)
        lineTo(w, h)
        close()
    }
    drawPath(rightPark, Color(0xFF059669).copy(alpha = 0.35f))

    // Waterway canal across the left terrain (blue river)
    val riverPath = Path().apply {
        moveTo(0f, horizonY + 80f)
        lineTo(w * 0.25f, horizonY + 95f)
        lineTo(w * 0.21f, horizonY + 115f)
        lineTo(0f, horizonY + 100f)
        close()
    }
    drawPath(riverPath, Color(0xFF0284C7))
    drawPath(riverPath, Color(0xFF38BDF8).copy(alpha = 0.5f), style = Stroke(width = 2f))

    // 3D City Building Blocks (Isometric prisms matching Screenshot 1)
    val buildingColors = listOf(
        Color(0xFFA7F3D0), // top face
        Color(0xFF6EE7B7), // left side
        Color(0xFF34D399)  // right side
    )

    draw3DBuilding(Offset(w * 0.12f, horizonY + 45f), 35f, 25f, 18f, buildingColors)
    draw3DBuilding(Offset(w * 0.75f, horizonY + 50f), 45f, 30f, 24f, buildingColors)
    draw3DBuilding(Offset(w * 0.16f, horizonY + 130f), 55f, 40f, 30f, buildingColors)
    draw3DBuilding(Offset(w * 0.78f, horizonY + 140f), 65f, 45f, 34f, buildingColors)

    // Perspective Road Grid
    // 1. Main Forward Avenue (converges directly into vanishing point)
    val roadTopHalfWidth = 14f * cameraPitch
    val roadBottomHalfWidth = 65f * cameraPitch

    val mainRoad = Path().apply {
        moveTo(vp.x - roadTopHalfWidth, vp.y)
        lineTo(vp.x + roadTopHalfWidth, vp.y)
        lineTo(vp.x + roadBottomHalfWidth, h)
        lineTo(vp.x - roadBottomHalfWidth, h)
        close()
    }
    // Asphalt road surface
    drawPath(mainRoad, Color(0xFF334155))
    // Road curbs
    drawLine(Color(0xFFCBD5E1), Offset(vp.x - roadTopHalfWidth, vp.y), Offset(vp.x - roadBottomHalfWidth, h), strokeWidth = 3f)
    drawLine(Color(0xFFCBD5E1), Offset(vp.x + roadTopHalfWidth, vp.y), Offset(vp.x + roadBottomHalfWidth, h), strokeWidth = 3f)

    // Center dash line on main road - animated scrolling with player movement
    val dashCount = 8
    for (step in 0..dashCount) {
        val rawT = (step.toFloat() / dashCount + scrollOffset) % 1.0f
        val t1 = rawT.pow(1.5f)
        val t2 = ((rawT + 0.055f).coerceAtMost(1f)).pow(1.5f)
        if (t2 > t1) {
            val y1 = vp.y + (h - vp.y) * t1
            val y2 = vp.y + (h - vp.y) * t2
            drawLine(
                color = Color(0xFFFDE047),
                start = Offset(vp.x, y1),
                end = Offset(vp.x, y2),
                strokeWidth = (2f + 4f * t1)
            )
        }
    }

    // 2. Cross Streets (horizontal perspective lines) - scrolling with movement
    val crossStreetBases = listOf(0.25f, 0.55f, 0.85f)
    crossStreetBases.forEach { baseT ->
        val rawT = (baseT + scrollOffset * 0.35f) % 1.0f
        val t = rawT.pow(1.3f)
        val roadY = vp.y + (h - vp.y) * t
        val roadThickness = (18f * t * cameraPitch).coerceAtLeast(4f)

        drawRect(
            color = Color(0xFF334155),
            topLeft = Offset(0f, roadY - roadThickness * 0.5f),
            size = Size(w, roadThickness)
        )
        // Crosswalk stripes at intersection with main road
        for (i in -2..2) {
            val stripeX = vp.x + i * (roadBottomHalfWidth * t * 0.35f)
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(stripeX, roadY - roadThickness * 0.45f),
                end = Offset(stripeX, roadY + roadThickness * 0.45f),
                strokeWidth = (3f * t).coerceAtLeast(1.5f)
            )
        }
    }
}

private fun DrawScope.draw3DBuilding(
    pos: Offset,
    width: Float,
    depth: Float,
    height: Float,
    colors: List<Color>
) {
    val (topColor, leftColor, rightColor) = colors

    // Left face
    val leftFace = Path().apply {
        moveTo(pos.x, pos.y)
        lineTo(pos.x, pos.y + height)
        lineTo(pos.x - width * 0.45f, pos.y + height + depth * 0.3f)
        lineTo(pos.x - width * 0.45f, pos.y + depth * 0.3f)
        close()
    }
    drawPath(leftFace, leftColor)

    // Right face
    val rightFace = Path().apply {
        moveTo(pos.x, pos.y)
        lineTo(pos.x, pos.y + height)
        lineTo(pos.x + width * 0.55f, pos.y + height + depth * 0.25f)
        lineTo(pos.x + width * 0.55f, pos.y + depth * 0.25f)
        close()
    }
    drawPath(rightFace, rightColor)

    // Top face
    val topFace = Path().apply {
        moveTo(pos.x, pos.y)
        lineTo(pos.x - width * 0.45f, pos.y + depth * 0.3f)
        lineTo(pos.x + width * 0.1f, pos.y + depth * 0.55f)
        lineTo(pos.x + width * 0.55f, pos.y + depth * 0.25f)
        close()
    }
    drawPath(topFace, topColor)
}

private fun DrawScope.drawNavigationRoute(
    w: Float,
    h: Float,
    horizonY: Float,
    gate: SpawnedGate,
    cameraHeading: Float,
    cameraPitch: Float
) {
    val vp = Offset(w * 0.5f, horizonY)
    val playerY = h * 0.78f

    // Draw yellow/lime navigation chevron arrows (▲ ▲ ▲) along the road matching Image 1
    for (i in 1..4) {
        val t = 0.78f - (i * 0.11f)
        val arrowY = vp.y + (h - vp.y) * t
        val arrowSize = (10f + 16f * t) * cameraPitch

        val chevron = Path().apply {
            moveTo(vp.x - arrowSize * 0.6f, arrowY + arrowSize * 0.4f)
            lineTo(vp.x, arrowY - arrowSize * 0.4f)
            lineTo(vp.x + arrowSize * 0.6f, arrowY + arrowSize * 0.4f)
        }
        drawPath(
            path = chevron,
            color = Color(0xFFFACC15),
            style = Stroke(width = 3.5f * t, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawPlayerAvatarAndPulse(
    playerPos: Offset,
    pulseFraction: Float,
    avatarBob: Float,
    walkCycle: Float = 0f,
    avatarRotationDegrees: Float = 0f,
    isGateNearby: Boolean
) {
    // 1. Pulsing Interaction Range Rings (Pokémon GO 165ft raid circle)
    val maxPulseRadius = 85f
    val currentPulseRadius = 25f + (maxPulseRadius - 25f) * pulseFraction
    val pulseAlpha = (1f - pulseFraction).coerceIn(0f, 1f) * 0.75f

    drawCircle(
        color = (if (isGateNearby) AriseCrimson else AriseCyanNeon).copy(alpha = pulseAlpha),
        radius = currentPulseRadius,
        center = playerPos,
        style = Stroke(width = 2.5f)
    )

    // Inner fixed raid presence circle
    drawCircle(
        color = (if (isGateNearby) AriseCrimson else Color(0xFF38BDF8)).copy(alpha = 0.25f),
        radius = 28f,
        center = playerPos
    )
    drawCircle(
        color = (if (isGateNearby) AriseCrimson else Color(0xFF38BDF8)).copy(alpha = 0.6f),
        radius = 28f,
        center = playerPos,
        style = Stroke(width = 1.5f)
    )

    // 2. Hunter Avatar Shadow
    drawOval(
        color = Color(0x66000000),
        topLeft = Offset(playerPos.x - 14f, playerPos.y + 4f),
        size = Size(28f, 10f)
    )

    // 3. 3D Hunter Avatar Body rotated to match walking bearing
    withTransform({
        rotate(avatarRotationDegrees, playerPos)
    }) {
        val avatarCenter = Offset(playerPos.x, playerPos.y - 18f + avatarBob)

        // Animated Walking Legs with stride swing
        val leftLegSwing = sin(walkCycle) * 6f
        val rightLegSwing = -sin(walkCycle) * 6f

        // Left leg
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(playerPos.x - 5f, avatarCenter.y + 14f),
            end = Offset(playerPos.x - 5f, playerPos.y + leftLegSwing),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        // Left shoe
        drawOval(
            color = Color(0xFFEF4444),
            topLeft = Offset(playerPos.x - 8f, playerPos.y + leftLegSwing - 2f),
            size = Size(7f, 5f)
        )

        // Right leg
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(playerPos.x + 5f, avatarCenter.y + 14f),
            end = Offset(playerPos.x + 5f, playerPos.y + rightLegSwing),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        // Right shoe
        drawOval(
            color = Color(0xFFEF4444),
            topLeft = Offset(playerPos.x + 2f, playerPos.y + rightLegSwing - 2f),
            size = Size(7f, 5f)
        )

        // Body (Hunter Coat)
        drawRoundRect(
            color = AriseShadowViolet,
            topLeft = Offset(avatarCenter.x - 9f, avatarCenter.y - 4f),
            size = Size(18f, 22f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
        // Red Trim Collar (matching Pokemon GO trainer jacket in image 2)
        drawRect(
            color = Color(0xFFEF4444),
            topLeft = Offset(avatarCenter.x - 7f, avatarCenter.y - 3f),
            size = Size(14f, 5f)
        )
        // Head / Face
        drawCircle(
            color = Color(0xFFFED7AA), // skin tone
            radius = 8f,
            center = Offset(avatarCenter.x, avatarCenter.y - 12f)
        )
        // Hair
        drawArc(
            color = Color(0xFF1E293B),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(avatarCenter.x - 8f, avatarCenter.y - 20f),
            size = Size(16f, 16f)
        )
        // Monarch Blue Eyes / Aura
        drawCircle(
            color = AriseCyanNeon,
            radius = 2f,
            center = Offset(avatarCenter.x - 3f, avatarCenter.y - 12f)
        )
        drawCircle(
            color = AriseCyanNeon,
            radius = 2f,
            center = Offset(avatarCenter.x + 3f, avatarCenter.y - 12f)
        )
    }
}

private data class GymVisualTheme(
    val primaryColor: Color,
    val accentColor: Color,
    val beaconColor: Color,
    val gymCrest: String,
    val stars: String,
    val cpRating: String,
    val gymTypeTitle: String,
    val isMajorSpire: Boolean = true
)

private fun getGymTheme(gate: SpawnedGate): GymVisualTheme {
    val bId = gate.boss.id.lowercase()
    return when {
        gate.rank == "S-Rank" || bId.contains("dragon") || bId.contains("kamish") -> GymVisualTheme(
            primaryColor = Color(0xFFDC2626), // Lava Red
            accentColor = Color(0xFFF59E0B),  // Molten Gold
            beaconColor = Color(0xFFEF4444),
            gymCrest = "🐉",
            stars = "⭐⭐⭐⭐⭐",
            cpRating = "CP 48,000",
            gymTypeTitle = "DRAGON CITADEL"
        )
        gate.rank == "A-Rank" || bId.contains("igris") || bId.contains("kargalgan") -> GymVisualTheme(
            primaryColor = Color(0xFF7C3AED), // Shadow Violet
            accentColor = Color(0xFF06B6D4),  // Neon Monarch Cyan
            beaconColor = Color(0xFF8B5CF6),
            gymCrest = "👑",
            stars = "⭐⭐⭐⭐",
            cpRating = "CP 36,000",
            gymTypeTitle = "MONARCH GYM"
        )
        gate.rank == "B-Rank" || bId.contains("cerberus") || bId.contains("silum") -> GymVisualTheme(
            primaryColor = Color(0xFF0284C7), // Glacial Azure
            accentColor = Color(0xFFE0F2FE),  // Frost White
            beaconColor = Color(0xFF38BDF8),
            gymCrest = "🐺",
            stars = "⭐⭐⭐",
            cpRating = "CP 24,000",
            gymTypeTitle = "FROST SANCTUM"
        )
        gate.rank == "C-Rank" || bId.contains("arachna") -> GymVisualTheme(
            primaryColor = Color(0xFF059669), // Necrotic Jade
            accentColor = Color(0xFF84CC16),  // Poison Lime
            beaconColor = Color(0xFF10B981),
            gymCrest = "🕷️",
            stars = "⭐⭐",
            cpRating = "CP 15,000",
            gymTypeTitle = "POISON CRYPT"
        )
        gate.rank == "D-Rank" || bId.contains("hobgoblin") -> GymVisualTheme(
            primaryColor = Color(0xFFEA580C), // Iron Rust
            accentColor = Color(0xFFFDE047),  // War Bronze
            beaconColor = Color(0xFFF97316),
            gymCrest = "🛡️",
            stars = "⭐",
            cpRating = "CP 8,500",
            gymTypeTitle = "WAR FORTRESS"
        )
        else -> GymVisualTheme(
            primaryColor = Color(0xFF06B6D4), // Mana Cyan
            accentColor = Color(0xFFA78BFA),  // Arcane Violet
            beaconColor = Color(0xFF38BDF8),
            gymCrest = "💎",
            stars = "✨",
            cpRating = "CACHE",
            gymTypeTitle = "MANA MONOLITH",
            isMajorSpire = false
        )
    }
}

private fun DrawScope.drawDungeonTowers(
    w: Float,
    h: Float,
    horizonY: Float,
    gates: List<SpawnedGate>,
    selectedGate: SpawnedGate?,
    cameraHeading: Float,
    cameraPitch: Float,
    towerGlow: Float
) {
    val vp = Offset(w * 0.5f, horizonY)
    val playerY = h * 0.78f
    val cameraZ = 300f * cameraPitch

    // Sort gates furthest first (depth sorting)
    val sortedGates = gates.sortedByDescending { it.distanceFeet }

    sortedGates.forEach { gate ->
        val relAngle = (gate.bearingDegrees - cameraHeading + 540f) % 360f - 180f
        val angleRad = Math.toRadians(relAngle.toDouble())
        val dist = gate.distanceFeet

        val gz = (dist * cos(angleRad)).toFloat()
        val gx = (dist * sin(angleRad)).toFloat()

        if (gz > -40f) { // only in front of camera
            val depth = gz + cameraZ
            val t = (cameraZ / depth).coerceIn(0.05f, 1.25f)
            val screenY = vp.y + (playerY - vp.y) * t
            val screenX = vp.x + (gx * 0.9f) * t

            val isSelected = selectedGate?.id == gate.id
            val theme = getGymTheme(gate)

            if (theme.isMajorSpire || gate.rank in listOf("A-Rank", "B-Rank", "C-Rank", "D-Rank")) {
                drawGymDungeonSpire(
                    base = Offset(screenX, screenY),
                    scale = t,
                    gate = gate,
                    theme = theme,
                    isSelected = isSelected,
                    towerGlow = towerGlow
                )
            } else {
                drawPokestopManaPortal(
                    base = Offset(screenX, screenY),
                    scale = t,
                    gate = gate,
                    theme = theme,
                    isSelected = isSelected,
                    towerGlow = towerGlow
                )
            }
        }
    }
}

/**
 * Towering Pokémon GO Gym Spire with distinct architectural themes, floating boss avatars & raid banners
 */
private fun DrawScope.drawGymDungeonSpire(
    base: Offset,
    scale: Float,
    gate: SpawnedGate,
    theme: GymVisualTheme,
    isSelected: Boolean,
    towerGlow: Float
) {
    val towerHeight = 140f * scale
    val towerWidth = 40f * scale
    val spireTop = Offset(base.x, base.y - towerHeight)

    // 1. Vertical Raid Sky Beacon
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(
                theme.beaconColor.copy(alpha = 0f),
                theme.beaconColor.copy(alpha = 0.5f * towerGlow),
                theme.beaconColor.copy(alpha = 0.85f * towerGlow)
            ),
            startY = 0f,
            endY = base.y
        ),
        start = Offset(base.x, 0f),
        end = base,
        strokeWidth = (9f * scale)
    )

    // 2. Base Arena Pedestal Ring
    val basePedestalWidth = towerWidth * 2.2f
    drawOval(
        color = Color(0xFF0F172A),
        topLeft = Offset(base.x - basePedestalWidth * 0.5f, base.y - 7f * scale),
        size = Size(basePedestalWidth, 14f * scale)
    )
    drawOval(
        color = theme.primaryColor,
        topLeft = Offset(base.x - basePedestalWidth * 0.5f, base.y - 7f * scale),
        size = Size(basePedestalWidth, 14f * scale),
        style = Stroke(width = 2.5f * scale)
    )

    // Glowing Encounter ring when within raid range (165 ft)
    if (gate.isRaidable) {
        drawOval(
            color = Color(0xFFF59E0B).copy(alpha = 0.7f * towerGlow),
            topLeft = Offset(base.x - basePedestalWidth * 0.7f, base.y - 10f * scale),
            size = Size(basePedestalWidth * 1.4f, 20f * scale),
            style = Stroke(width = 3f * scale)
        )
    }

    // 3. Spire Core Column
    drawLine(
        color = Color(0xFF1E293B),
        start = base,
        end = spireTop,
        strokeWidth = 7f * scale
    )

    // 4. Multi-tiered Gym Stadium Rings (Pokémon GO Gym style)
    for (ring in 1..3) {
        val ry = base.y - (towerHeight * (ring / 3.4f))
        val rw = (towerWidth * (1.3f - ring * 0.18f))
        drawOval(
            color = Color.White.copy(alpha = 0.95f),
            topLeft = Offset(base.x - rw * 0.5f, ry - 3.5f * scale),
            size = Size(rw, 7f * scale)
        )
        drawOval(
            color = theme.accentColor,
            topLeft = Offset(base.x - rw * 0.5f, ry - 3.5f * scale),
            size = Size(rw, 7f * scale),
            style = Stroke(width = 2f * scale)
        )
    }

    // 5. Distinct Gym Summit Plinth & Boss Hologram Disc
    val summitRadius = 22f * scale
    // Outer glow aura
    drawCircle(
        color = theme.primaryColor.copy(alpha = 0.45f * towerGlow),
        radius = summitRadius * 1.35f,
        center = spireTop
    )
    // Dark Plinth Disc
    drawCircle(
        color = Color(0xFF0F172A),
        radius = summitRadius,
        center = spireTop
    )
    // Metallic Neon Ring
    drawCircle(
        color = theme.accentColor,
        radius = summitRadius,
        center = spireTop,
        style = Stroke(width = 2.5f * scale)
    )

    // Draw Distinct Boss Crest Icon (Large & Vibrant)
    val crestPaint = Paint().apply {
        textSize = (24f * scale).coerceAtLeast(16f)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        gate.boss.iconEmoji.ifEmpty { theme.gymCrest },
        spireTop.x,
        spireTop.y + (7f * scale),
        crestPaint
    )

    // 6. Selected Aura
    if (isSelected) {
        drawCircle(
            color = AriseCyanNeon.copy(alpha = 0.75f * towerGlow),
            radius = summitRadius * 1.9f,
            center = spireTop,
            style = Stroke(width = 3f * scale)
        )
    }

    // 7. Distinct Floating Gym Badges & Raid Status Banner (Pokémon GO style)
    val raidBannerY = spireTop.y - (summitRadius + 8f * scale)

    // Floating Stars / CP Rating
    val starsPaint = Paint().apply {
        color = android.graphics.Color.YELLOW
        textSize = (9f * scale).coerceIn(8f, 13f)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        theme.stars,
        spireTop.x,
        raidBannerY - (13f * scale),
        starsPaint
    )

    // Floating Gym Title & Distance Badge
    val badgePaint = Paint().apply {
        color = if (gate.isRaidable) android.graphics.Color.GREEN else android.graphics.Color.WHITE
        textSize = (11f * scale).coerceIn(10f, 16f)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    val distanceText = if (gate.isRaidable) {
        "⚔️ RAID READY!"
    } else {
        "${gate.rank} (%.0fft)".format(gate.distanceFeet)
    }
    drawContext.canvas.nativeCanvas.drawText(
        distanceText,
        spireTop.x,
        raidBannerY,
        badgePaint
    )
}

/**
 * Floating PokéStop-style Mana Crystal Portal
 */
private fun DrawScope.drawPokestopManaPortal(
    base: Offset,
    scale: Float,
    gate: SpawnedGate,
    theme: GymVisualTheme,
    isSelected: Boolean,
    towerGlow: Float
) {
    val portalHeight = 65f * scale
    val portalCenter = Offset(base.x, base.y - portalHeight)
    val ringColor = theme.primaryColor

    // Base ground pedestal
    drawOval(
        color = ringColor.copy(alpha = 0.4f),
        topLeft = Offset(base.x - 16f * scale, base.y - 5f * scale),
        size = Size(32f * scale, 10f * scale)
    )
    // Upright metallic post
    drawLine(
        color = Color(0xFF94A3B8),
        start = base,
        end = portalCenter,
        strokeWidth = 3.5f * scale
    )

    // Floating spinning PokéStop ring (Image 2 style)
    val ringRadius = 16f * scale
    drawCircle(
        color = Color(0xFF0F172A),
        radius = ringRadius,
        center = portalCenter
    )
    drawCircle(
        color = ringColor,
        radius = ringRadius,
        center = portalCenter,
        style = Stroke(width = 3f * scale)
    )

    // Inner floating crystal icon
    val paint = Paint().apply {
        textSize = (18f * scale).coerceAtLeast(13f)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        gate.boss.iconEmoji.ifEmpty { "💎" },
        portalCenter.x,
        portalCenter.y + (6f * scale),
        paint
    )

    if (isSelected) {
        drawCircle(
            color = Color.White.copy(alpha = 0.85f * towerGlow),
            radius = ringRadius * 1.6f,
            center = portalCenter,
            style = Stroke(width = 2.5f * scale)
        )
    }

    // Portal Tag
    val tagPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = (10f * scale).coerceIn(9f, 14f)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    val label = if (gate.isRaidable) "✨ HARVEST MANA" else "%.0f ft".format(gate.distanceFeet)
    drawContext.canvas.nativeCanvas.drawText(
        label,
        portalCenter.x,
        portalCenter.y - (ringRadius + 5f * scale),
        tagPaint
    )
}
