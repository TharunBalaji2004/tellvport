package com.ldihackos.tellvport.views

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.ldihackos.tellvport.R
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@AndroidEntryPoint
class BeaconActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load osmdroid configuration
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm_prefs", MODE_PRIVATE))

        setContentView(R.layout.activity_beacon)
        mapView = findViewById(R.id.map_view)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check permissions and enable location
        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            enableLocationUpdates()
        } else {
            requestPermissionsLauncher.launch(permissions)
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.all { it }) {
                enableLocationUpdates()
            }
        }

    private fun enableLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, // Priority for high accuracy
            3000 // Update interval in milliseconds
        ).apply {
            setMinUpdateIntervalMillis(2000) // Minimum interval between updates
            setWaitForAccurateLocation(false) // Optional: Wait for accurate locations
        }.build()


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location = locationResult.lastLocation ?: return
            updateUserLocation(location)
        }
    }

    private fun updateUserLocation(location: Location) {
        val userLocation = GeoPoint(location.latitude, location.longitude)
        mapView.controller.setCenter(userLocation)

        // Add a marker for the user's location
        val marker = Marker(mapView)
        marker.position = userLocation
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "You are here"
        mapView.overlays.clear() // Clear old markers
        mapView.overlays.add(marker)
        mapView.invalidate()
    }
}
