package com.ldihackos.tellvport.views

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ldihackos.tellvport.databinding.ActivitySensorBinding
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@AndroidEntryPoint
class SensorActivity : AppCompatActivity(), SensorEventListener {

    private var _binding: ActivitySensorBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Movement detection parameters
    private val movementThreshold = 0.5f  // Minimum acceleration to be considered movement
    private val smoothingFactor = 0.1f    // For low-pass filter (0 to 1)
    private val locationUpdateThreshold = 1.0 // Minimum distance in meters to update location

    // Smoothed acceleration values
    private var smoothedX = 0f
    private var smoothedY = 0f
    private var smoothedZ = 0f

    // Previous values for velocity calculation
    private var lastUpdateTime = 0L
    private var velocityX = 0f
    private var velocityY = 0f

    // Current position
    private var currentLatitude = 16.654222
    private var currentLongitude = 74.261795
    private var lastLocationUpdateTime = 0L

    // Distance calculation constants
    private val earthRadius = 6371000.0 // Earth's radius in meters

    // MapView and user marker
    private lateinit var mapView: MapView
    private lateinit var userMarker: Marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySensorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SensorManager with faster update rate for better movement detection
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        // Register the accelerometer listener with faster updates
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        initializeMap()
    }

    private fun initializeMap() {
        mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val startGeoPoint = GeoPoint(currentLatitude, currentLongitude)
        mapView.controller.setCenter(startGeoPoint)
        mapView.controller.setZoom(18)

        userMarker = Marker(mapView).apply {
            position = startGeoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(userMarker)
        mapView.setBuiltInZoomControls(true)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_LINEAR_ACCELERATION) return

        val currentTime = System.currentTimeMillis()
        if (lastUpdateTime == 0L) {
            lastUpdateTime = currentTime
            return
        }

        val deltaTime = (currentTime - lastUpdateTime) / 1000f // Convert to seconds
        lastUpdateTime = currentTime

        // Apply low-pass filter for smoothing
        smoothedX += (event.values[0] - smoothedX) * smoothingFactor
        smoothedY += (event.values[1] - smoothedY) * smoothingFactor
        smoothedZ += (event.values[2] - smoothedZ) * smoothingFactor

        // Check if movement exceeds threshold
        if (Math.abs(smoothedX) > movementThreshold || Math.abs(smoothedY) > movementThreshold) {
            // Calculate velocity using time-based integration
            velocityX += smoothedX * deltaTime
            velocityY += smoothedY * deltaTime

            // Apply simple dampening to prevent unlimited velocity growth
            velocityX *= 0.95f
            velocityY *= 0.95f

            // Update position based on velocity
            updateUserPosition(velocityX * deltaTime, velocityY * deltaTime)
        } else {
            // Gradually reduce velocity when no significant movement
            velocityX *= 0.9f
            velocityY *= 0.9f
        }
    }

    private fun updateUserPosition(deltaX: Float, deltaY: Float) {
        val currentTime = System.currentTimeMillis()

        // Convert movement to approximate meters (assuming phone is held flat)
        val metersX = deltaX * 0.5 // Scale factor can be adjusted
        val metersY = deltaY * 0.5

        // Calculate new position using Haversine formula
        val newLatitude = currentLatitude + (metersY / earthRadius) * (180.0 / Math.PI)
        val newLongitude = currentLongitude + (metersX / earthRadius) * (180.0 / Math.PI) /
                Math.cos(Math.toRadians(currentLatitude))

        // Check if we've moved far enough to warrant a location update
        val distance = calculateDistance(currentLatitude, currentLongitude, newLatitude, newLongitude)

        if (distance >= locationUpdateThreshold &&
            (currentTime - lastLocationUpdateTime) >= 100) { // Update at most every 100ms

            currentLatitude = newLatitude
            currentLongitude = newLongitude
            lastLocationUpdateTime = currentTime

            updateMapLocation(currentLatitude, currentLongitude)
        }
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        Log.d("THARUN", "$latitude, $longitude")
        userMarker.position = GeoPoint(latitude, longitude)
        mapView.invalidate()

        // Optionally animate the map to follow the user
        mapView.controller.animateTo(GeoPoint(latitude, longitude))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}