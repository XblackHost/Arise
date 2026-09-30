package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.example.data.model.SpawnedGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.*

enum class RadarEngine(val displayName: String, val badge: String, val description: String) {
    ONLINE_MAPLIBRE(
        "Online: MapLibre GL",
        "Vector Tile HUD",
        "MapLibre GL Vector Tile Engine • Live Satellite Triangulation"
    ),
    OFFLINE_SQLITE(
        "Offline: SQLite Spatial Index",
        "Local Spatial Index",
        "Room / SQLite Spatial Index • Local Gate Cache • Zero Latency"
    )
}

class HunterRadarManager(private val context: Context) : LocationListener, SensorEventListener {

    companion object {
        const val FEET_PER_METER = 3.28084f
        const val RAID_RANGE_FEET = 165f // approx 50 meters
        private const val TAG = "HunterRadarManager"
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    // Radar Engine Mode (Online: MapLibre GL vs Offline: SQLite Spatial Index)
    private val _radarEngine = MutableStateFlow(RadarEngine.ONLINE_MAPLIBRE)
    val radarEngine: StateFlow<RadarEngine> = _radarEngine.asStateFlow()

    private val _radarRangeFeet = MutableStateFlow(2000f)
    val radarRangeFeet: StateFlow<Float> = _radarRangeFeet.asStateFlow()

    private val _sqliteCachedGateCount = MutableStateFlow(6)
    val sqliteCachedGateCount: StateFlow<Int> = _sqliteCachedGateCount.asStateFlow()

    // Current player location (defaults to a central point if GPS fix pending)
    private val _currentLatitude = MutableStateFlow(37.7749)
    val currentLatitude: StateFlow<Double> = _currentLatitude.asStateFlow()

    private val _currentLongitude = MutableStateFlow(-122.4194)
    val currentLongitude: StateFlow<Double> = _currentLongitude.asStateFlow()

    private val _hasGpsFix = MutableStateFlow(false)
    val hasGpsFix: StateFlow<Boolean> = _hasGpsFix.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // Real-Time Walking Speed, Bearing & Movement state (like Pokémon GO)
    private val _playerBearing = MutableStateFlow(0f)
    val playerBearing: StateFlow<Float> = _playerBearing.asStateFlow()

    private val _walkingSpeedMps = MutableStateFlow(0f)
    val walkingSpeedMps: StateFlow<Float> = _walkingSpeedMps.asStateFlow()

    private val _isWalking = MutableStateFlow(false)
    val isWalking: StateFlow<Boolean> = _isWalking.asStateFlow()

    // Walking metrics
    private val _sessionSteps = MutableStateFlow(0)
    val sessionSteps: StateFlow<Int> = _sessionSteps.asStateFlow()

    private val _sessionFeet = MutableStateFlow(0f)
    val sessionFeet: StateFlow<Float> = _sessionFeet.asStateFlow()

    private val _sessionMeters = MutableStateFlow(0f)
    val sessionMeters: StateFlow<Float> = _sessionMeters.asStateFlow()

    private val _burnedCalories = MutableStateFlow(0f)
    val burnedCalories: StateFlow<Float> = _burnedCalories.asStateFlow()

    // Spawned Gates
    private val _spawnedGates = MutableStateFlow<List<SpawnedGate>>(emptyList())
    val spawnedGates: StateFlow<List<SpawnedGate>> = _spawnedGates.asStateFlow()

    private val _selectedGate = MutableStateFlow<SpawnedGate?>(null)
    val selectedGate: StateFlow<SpawnedGate?> = _selectedGate.asStateFlow()

    private var lastRecordedLocation: Location? = null
    private var lastLocationTimeMs: Long = 0L
    private var lastRawStepCounterValue = -1
    private var hasHardwareStepSensor = false
    private var lastPhysicalStepTimeMs = 0L

    // Accelerometer-based Automatic Step Detection Filter
    private var filteredGravity = 9.81f
    private var lastStepTimeMs = 0L
    private var isStepArmed = true

    init {
        // Initial spawn around default coordinates
        refreshSpawnedGates()
        // Auto-start tracking on creation so steps and location are immediately active
        startTracking()
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        _isTracking.value = true

        try {
            locationManager?.let { lm ->
                val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val netEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                // High-frequency location updates for smooth real-time walking like Pokémon GO
                if (gpsEnabled) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.5f, this)
                    lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { onLocationChanged(it) }
                }
                if (netEnabled) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0.5f, this)
                    if (!gpsEnabled) {
                        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { onLocationChanged(it) }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start location updates: ${e.message}")
        }

        // Register All Step and Orientation Sensors
        try {
            sensorManager?.let { sm ->
                // 1. Hardware Step Counter
                sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
                    hasHardwareStepSensor = true
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                }
                // 2. Hardware Step Detector
                sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let {
                    hasHardwareStepSensor = true
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                }
                // 3. Accelerometer (Fallback automatic step detection on phones without hardware pedometer)
                sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                }
                // 4. Rotation Vector for real-time 3D compass heading
                sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sensor registration error: ${e.message}")
        }
    }

    fun stopTracking() {
        _isTracking.value = false
        _isWalking.value = false
        _walkingSpeedMps.value = 0f
        hasHardwareStepSensor = false
        lastRawStepCounterValue = -1
        try {
            locationManager?.removeUpdates(this)
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping location/sensor updates: ${e.message}")
        }
    }

    override fun onLocationChanged(location: Location) {
        _hasGpsFix.value = true
        val now = System.currentTimeMillis()

        val prev = lastRecordedLocation
        if (prev != null && _isTracking.value) {
            val deltaMeters = calculateDistanceMeters(prev.latitude, prev.longitude, location.latitude, location.longitude)
            val timeDeltaSec = ((now - lastLocationTimeMs) / 1000f).coerceAtLeast(0.5f)

            if (deltaMeters in 0.5f..250.0f) { // Real human physical movement
                val addedFeet = deltaMeters * FEET_PER_METER
                _sessionFeet.value += addedFeet
                _sessionMeters.value += deltaMeters

                // Speed calculation
                val speed = if (location.hasSpeed() && location.speed > 0.1f) {
                    location.speed
                } else {
                    (deltaMeters / timeDeltaSec).coerceIn(0f, 15f)
                }
                _walkingSpeedMps.value = speed
                _isWalking.value = speed >= 0.35f

                // Direction / Bearing calculation (faces the way the player is walking)
                val bearing = if (location.hasBearing() && location.bearing != 0f) {
                    location.bearing
                } else {
                    calculateBearing(prev.latitude, prev.longitude, location.latitude, location.longitude)
                }
                _playerBearing.value = bearing

                // Fallback steps ONLY if no physical pedometer/accelerometer step events occurred recently
                // (e.g. running on an emulator or simulated GPS without sensor pulses)
                val timeSinceSensorStep = now - lastPhysicalStepTimeMs
                if (timeSinceSensorStep > 3500L) {
                    val addedSteps = (deltaMeters / 0.762f).roundToInt().coerceAtLeast(1)
                    _sessionSteps.value += addedSteps
                    _burnedCalories.value = _sessionSteps.value * 0.04f
                }
            } else if (deltaMeters < 0.3f) {
                // Standing still
                _walkingSpeedMps.value = 0f
                _isWalking.value = false
            }
        } else if (location.hasBearing()) {
            _playerBearing.value = location.bearing
        }

        lastRecordedLocation = location
        lastLocationTimeMs = now
        _currentLatitude.value = location.latitude
        _currentLongitude.value = location.longitude

        // Recalculate gate distances relative to player's new position
        updateDistancesToGates(location.latitude, location.longitude)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !_isTracking.value) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                // Real-time device orientation / compass heading
                try {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    val normalizedBearing = (azimuthDegrees + 360f) % 360f
                    // If not moving fast, use compass orientation
                    if (_walkingSpeedMps.value < 0.5f) {
                        _playerBearing.value = normalizedBearing
                    }
                } catch (e: Exception) {
                    // Ignore vector math error
                }
            }

            Sensor.TYPE_STEP_COUNTER -> {
                val totalSteps = event.values[0].toInt()
                if (lastRawStepCounterValue < 0) {
                    lastRawStepCounterValue = totalSteps
                } else {
                    val delta = totalSteps - lastRawStepCounterValue
                    if (delta in 1..100) {
                        lastRawStepCounterValue = totalSteps
                        onPhysicalStepDetected(delta)
                    } else if (delta > 100) {
                        lastRawStepCounterValue = totalSteps
                    }
                }
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                onPhysicalStepDetected(1)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                // If hardware step sensor is actively reporting, skip accelerometer to prevent double counting
                if (hasHardwareStepSensor && lastRawStepCounterValue >= 0) return

                // High-precision physical step detection from real movement / walking
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)

                // Low-pass filter to isolate gravity component
                filteredGravity = 0.85f * filteredGravity + 0.15f * magnitude
                val dynamicAcc = abs(magnitude - filteredGravity)
                val now = System.currentTimeMillis()

                // Walking footstep peak threshold (1.7 m/s² dynamic acceleration, min 280ms interval)
                if (dynamicAcc > 1.70f && isStepArmed && (now - lastStepTimeMs) > 280L) {
                    isStepArmed = false
                    lastStepTimeMs = now
                    onPhysicalStepDetected(1)
                } else if (dynamicAcc < 0.45f) {
                    isStepArmed = true
                }
            }
        }
    }

    private fun onPhysicalStepDetected(count: Int = 1) {
        if (count <= 0) return
        lastPhysicalStepTimeMs = System.currentTimeMillis()
        _sessionSteps.value += count
        _isWalking.value = true
        _burnedCalories.value = _sessionSteps.value * 0.04f

        if (_walkingSpeedMps.value <= 0.1f) {
            _walkingSpeedMps.value = 1.35f // Average walking speed ~1.35 m/s
        }

        // Distance from steps is only accumulated when GPS fix is NOT active.
        // When GPS is active, onLocationChanged already accurately measures true ground displacement.
        if (!_hasGpsFix.value) {
            val addedFeet = count * 2.5f
            val addedMeters = count * 0.762f
            _sessionFeet.value += addedFeet
            _sessionMeters.value += addedMeters

            // Advance character in world space along current heading
            val target = selectedGate.value
            if (target != null && target.distanceMeters > 0.5f) {
                advanceTowardsGate(target, addedFeet)
            } else {
                val (newLat, newLng) = calculateOffsetCoordinate(
                    _currentLatitude.value,
                    _currentLongitude.value,
                    addedMeters.toDouble(),
                    _playerBearing.value.toDouble()
                )
                _currentLatitude.value = newLat
                _currentLongitude.value = newLng
                updateDistancesToGates(newLat, newLng)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    fun addSteps(steps: Int) {
        if (steps <= 0) return
        val newTotal = _sessionSteps.value + steps
        _sessionSteps.value = newTotal
        // Average stride length is ~2.5 feet (0.762 meters)
        val addedFeet = steps * 2.5f
        val addedMeters = addedFeet / FEET_PER_METER
        _sessionFeet.value += addedFeet
        _sessionMeters.value += addedMeters
        _burnedCalories.value = newTotal * 0.04f

        val target = selectedGate.value
        if (target != null && target.distanceMeters > 0.5f) {
            advanceTowardsGate(target, addedFeet)
        } else {
            val (newLat, newLng) = calculateOffsetCoordinate(_currentLatitude.value, _currentLongitude.value, addedMeters.toDouble(), 0.0)
            _currentLatitude.value = newLat
            _currentLongitude.value = newLng
            updateDistancesToGates(newLat, newLng)
        }
    }

    fun addWalkedFeet(feet: Float) {
        if (feet <= 0f) return
        _sessionFeet.value += feet
        val meters = feet / FEET_PER_METER
        _sessionMeters.value += meters
        val addedSteps = (feet / 2.5f).toInt().coerceAtLeast(1)
        _sessionSteps.value += addedSteps
        _burnedCalories.value = _sessionSteps.value * 0.04f

        val target = selectedGate.value
        if (target != null && target.distanceMeters > 0.5f) {
            advanceTowardsGate(target, feet)
        } else {
            // Move player position forward along current bearing
            val (newLat, newLng) = calculateOffsetCoordinate(_currentLatitude.value, _currentLongitude.value, meters.toDouble(), 0.0)
            _currentLatitude.value = newLat
            _currentLongitude.value = newLng
            updateDistancesToGates(newLat, newLng)
        }
    }

    fun addWalkedMeters(meters: Float) {
        addWalkedFeet(meters * FEET_PER_METER)
    }

    fun toggleRadarEngine() {
        _radarEngine.value = if (_radarEngine.value == RadarEngine.ONLINE_MAPLIBRE) {
            RadarEngine.OFFLINE_SQLITE
        } else {
            RadarEngine.ONLINE_MAPLIBRE
        }
    }

    fun setRadarEngine(engine: RadarEngine) {
        _radarEngine.value = engine
    }

    fun setRadarRange(rangeFeet: Float) {
        _radarRangeFeet.value = rangeFeet
    }

    fun selectGate(gate: SpawnedGate?) {
        _selectedGate.value = gate
    }

    fun clearGate(gateId: String) {
        _spawnedGates.value = _spawnedGates.value.map {
            if (it.id == gateId) it.copy(isCleared = true) else it
        }
        if (_selectedGate.value?.id == gateId) {
            _selectedGate.value = _selectedGate.value?.copy(isCleared = true)
        }
    }

    /**
     * Walks directly toward a selected gate, updating user coordinates and gate distance.
     */
    fun advanceTowardsGate(gate: SpawnedGate, feetToAdvance: Float) {
        val metersToAdvance = feetToAdvance / FEET_PER_METER
        val currentDistMeters = gate.distanceMeters

        if (currentDistMeters <= 0.1f) return

        val fraction = (metersToAdvance / currentDistMeters).coerceIn(0f, 1f)
        val newLat = _currentLatitude.value + fraction * (gate.latitude - _currentLatitude.value)
        val newLng = _currentLongitude.value + fraction * (gate.longitude - _currentLongitude.value)

        _currentLatitude.value = newLat
        _currentLongitude.value = newLng
        updateDistancesToGates(newLat, newLng)
    }

    /**
     * Spawns 6-8 gates of various ranks (E to A) at realistic distances around the user.
     */
    fun refreshSpawnedGates() {
        val lat = _currentLatitude.value
        val lng = _currentLongitude.value
        val bosses = BossCatalog.allBosses

        val gateTemplates = listOf(
            Triple("E-Rank Goblin Cavern", "E-Rank", bosses.find { it.id == "goblin_chief" } ?: bosses[0]),
            Triple("E-Rank Shadow Den", "E-Rank", bosses.find { it.id == "bloodfang_wolf" } ?: bosses[1]),
            Triple("D-Rank Hobgoblin Incursion", "D-Rank", bosses.find { it.id == "hobgoblin_brute" } ?: bosses[2]),
            Triple("C-Rank Arachnid Catacomb", "C-Rank", bosses.find { it.id == "crypt_arachna" } ?: bosses[3]),
            Triple("B-Rank Hellhound Portal", "B-Rank", bosses.find { it.id == "cerberus" } ?: bosses[4]),
            Triple("A-Rank Bloodred Sanctum", "A-Rank", bosses.find { it.id == "igris" } ?: bosses[5])
        )

        // Generate offsets: distances from 35 meters (115 ft) to 480 meters (1575 ft)
        val distancePresets = listOf(40f, 65f, 130f, 220f, 340f, 480f)
        val anglePresets = listOf(30f, 95f, 160f, 215f, 280f, 345f)

        val newGates = mutableListOf<SpawnedGate>()

        for (i in gateTemplates.indices) {
            val (name, rank, boss) = gateTemplates[i]
            val distMeters = distancePresets[i] + ((-10..15).random())
            val bearing = (anglePresets[i] + ((-15..15).random()) + 360f) % 360f

            // Calculate destination coordinate given distance & bearing
            val (destLat, destLng) = calculateOffsetCoordinate(lat, lng, distMeters.toDouble(), bearing.toDouble())
            val distFeet = distMeters * FEET_PER_METER
            val directionName = bearingToCompass(bearing)

            val gate = SpawnedGate(
                id = UUID.randomUUID().toString(),
                name = name,
                rank = rank,
                boss = boss,
                latitude = destLat,
                longitude = destLng,
                distanceMeters = distMeters,
                distanceFeet = distFeet,
                bearingDegrees = bearing,
                directionLabel = directionName,
                isRaidable = distFeet <= RAID_RANGE_FEET,
                manaCrystalReward = when (rank) {
                    "E-Rank" -> 3
                    "D-Rank" -> 5
                    "C-Rank" -> 8
                    "B-Rank" -> 12
                    else -> 20
                },
                xpReward = when (rank) {
                    "E-Rank" -> 60
                    "D-Rank" -> 110
                    "C-Rank" -> 180
                    "B-Rank" -> 280
                    else -> 450
                },
                goldReward = when (rank) {
                    "E-Rank" -> 150
                    "D-Rank" -> 300
                    "C-Rank" -> 500
                    "B-Rank" -> 850
                    else -> 1400
                }
            )
            newGates.add(gate)
        }

        _spawnedGates.value = newGates
        _sqliteCachedGateCount.value = newGates.size
        if (_selectedGate.value == null || _spawnedGates.value.none { it.id == _selectedGate.value?.id }) {
            _selectedGate.value = newGates.firstOrNull()
        }
    }

    private fun updateDistancesToGates(userLat: Double, userLng: Double) {
        val updated = _spawnedGates.value.map { gate ->
            val distMeters = calculateDistanceMeters(userLat, userLng, gate.latitude, gate.longitude)
            val distFeet = distMeters * FEET_PER_METER
            val bearing = calculateBearing(userLat, userLng, gate.latitude, gate.longitude)
            val direction = bearingToCompass(bearing)

            gate.copy(
                distanceMeters = distMeters,
                distanceFeet = distFeet,
                bearingDegrees = bearing,
                directionLabel = direction,
                isRaidable = distFeet <= RAID_RANGE_FEET
            )
        }
        _spawnedGates.value = updated

        // Update selected gate reference
        _selectedGate.value?.let { currentSel ->
            _selectedGate.value = updated.find { it.id == currentSel.id }
        }
    }

    private fun calculateOffsetCoordinate(
        lat: Double,
        lng: Double,
        distMeters: Double,
        bearingDegrees: Double
    ): Pair<Double, Double> {
        val earthRadius = 6371000.0 // meters
        val distRad = distMeters / earthRadius
        val bearingRad = Math.toRadians(bearingDegrees)
        val latRad = Math.toRadians(lat)
        val lngRad = Math.toRadians(lng)

        val newLatRad = asin(
            sin(latRad) * cos(distRad) + cos(latRad) * sin(distRad) * cos(bearingRad)
        )
        val newLngRad = lngRad + atan2(
            sin(bearingRad) * sin(distRad) * cos(latRad),
            cos(distRad) - sin(latRad) * sin(newLatRad)
        )

        return Pair(Math.toDegrees(newLatRad), Math.toDegrees(newLngRad))
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        return try {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            results[0]
        } catch (e: Throwable) {
            haversineMeters(lat1, lon1, lat2, lon2).toFloat()
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLng = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLng) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLng)
        val bearingRad = atan2(y, x)
        return ((Math.toDegrees(bearingRad) + 360.0) % 360.0).toFloat()
    }

    private fun bearingToCompass(bearing: Float): String {
        return when (bearing) {
            in 22.5f..67.5f -> "North-East (NE)"
            in 67.5f..112.5f -> "East (E)"
            in 112.5f..157.5f -> "South-East (SE)"
            in 157.5f..202.5f -> "South (S)"
            in 202.5f..247.5f -> "South-West (SW)"
            in 247.5f..292.5f -> "West (W)"
            in 292.5f..337.5f -> "North-West (NW)"
            else -> "North (N)"
        }
    }
}
