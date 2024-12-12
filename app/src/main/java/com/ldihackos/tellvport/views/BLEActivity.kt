package com.ldihackos.tellvport.views

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.ldihackos.tellvport.databinding.ActivityBleBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.*
import kotlin.math.abs
import kotlin.math.max

@AndroidEntryPoint
class BLEActivity : AppCompatActivity() {

    // BLE and Map variables (keep existing)
    private var _binding: ActivityBleBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // Tracking and accuracy variables
    private var lastKnownLocation: Location? = null
    private var lastBLELocation: GeoPoint? = null
    private var isTracking = true
    private var isBLEScanning = true

    // Position fusion variables
    private val LOCATION_FUSION_THRESHOLD = 10.0 // meters
    private val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
    private val FASTEST_LOCATION_UPDATE_INTERVAL = 2000L // 2 seconds

    // Existing BLE and map variables (keep as they were)
    private var routeLine: Polyline? = null
    private val destinationPoint = GeoPoint(16.654344, 74.262113)
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var userMarker: Marker? = null
    private var destMark: Marker? = null

    // Existing beacon map
    private val beaconMap: HashMap<String, Beacon> = hashMapOf(
        "KBPro_469145" to Beacon(16.654348, 74.262050, -80),
        "KBPro_469166" to Beacon(16.654327, 74.261972, -85),
        "KBPro_469111" to Beacon(16.654327, 74.262026, -80)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityBleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Existing map configuration (keep as is)
        Configuration.getInstance().load(this, this.getSharedPreferences("osmdroid", 0))
        setupMapView()

        // Initialize Bluetooth
        setupBluetoothScanning()

        setupGTracking()

        // Setup tracking buttons
        setupTrackingButtons()
    }

    private fun setupMapView() {
        binding.mapView.apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 19.0
            maxZoomLevel = 22.0
        }

        val originalGeoPoint = GeoPoint(16.654222, 74.261795)
        binding.mapView.controller.apply {
            setZoom(21.0)
            setCenter(originalGeoPoint)
        }

        // Initialize markers
        userMarker = Marker(binding.mapView).apply {
            position = originalGeoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "You are here"
        }

        destMark = Marker(binding.mapView).apply {
            position = destinationPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Destination"
        }
    }

    private fun setupBluetoothScanning() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupGTracking() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = FASTEST_LOCATION_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationFromG(location)
                }
            }
        }
    }

    private fun setupTrackingButtons() {
        binding.btnStartScan.setOnClickListener {
            startCompositiveTracking()
        }

        binding.btnStopScan.setOnClickListener {
            stopCompositiveTracking()
        }
    }

    private fun startCompositiveTracking() {
        if (!isTracking && !isBLEScanning) {
            checkPermissionsAndStartTracking()
        } else {
            //Toast.makeText(this, "Already tracking", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopCompositiveTracking() {
        if (isTracking || isBLEScanning) {
            stopGTracking()
            //stopBluetoothScanning()
        } else {
            Toast.makeText(this, "Not currently tracking", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionsAndStartTracking() {
        val permissionsToRequest = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startTracking()
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                startTracking()
            } else {
                Toast.makeText(this, "Permissions denied. Cannot start tracking.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun startTracking() {
        startGTracking()
        //startBluetoothScanning()
    }

    private fun startGTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        isTracking = true
    }

    private fun stopGTracking() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        isTracking = false
    }

    private fun updateLocationFromG(location: Location) {
        lastKnownLocation = location
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        Log.i("tag", "$location.latitude, $location.longitude")
        val finalLocation = fuseBLEAndGLocation(geoPoint)

        updateUserPositionOnMap(finalLocation)
    }

    private fun fuseBLEAndGLocation(location: GeoPoint): GeoPoint {
        val bleLocation = lastBLELocation
        Log.i("bleTag", "$bleLocation.mLatitude, $bleLocation.mLongitude, $location.latitude - $bleLocation.mLatitude, $location.longitude - $bleLocation.mLongitude")
        return when {
            bleLocation == null -> location
            lastKnownLocation == null -> bleLocation
            else -> {
                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    bleLocation.latitude, bleLocation.longitude,
                    results
                )

                // If locations are close, average them
                if (results[0] <= LOCATION_FUSION_THRESHOLD) {
                    GeoPoint(
                        (location.latitude + bleLocation.latitude) / 2,
                        (location.longitude + bleLocation.longitude) / 2
                    )
                } else {
                    // Choose more accurate location based on available data
                    location
                }
            }
        }
    }

    // Rest of the existing BLE scanning methods (leScanCallback, etc.) remain the same
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this@BLEActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    result.device.name ?: "Unknown Device"
                } else {
                    "Permission Required"
                }
            } else {
                result.device.name ?: "Unknown Device"
            }

            val rssi = result.rssi

            if (beaconMap.containsKey(deviceName)) {
                // Update beacon and calculate BLE-based location
                val beacon = beaconMap[deviceName]?.copy(rssi = rssi)
                beacon?.let {
                    beaconMap[deviceName] = it
                }

                val bleLocation = calculatePosition(beaconMap.values.toList())
                lastBLELocation = GeoPoint(bleLocation.first, bleLocation.second)

                lastKnownLocation?.let {
                    val finalLocation = fuseBLEAndGLocation(GeoPoint(it.latitude, it.longitude))
                    updateUserPositionOnMap(finalLocation)
                }
            }
        }
    }

    private fun updateUserPositionOnMap(location: GeoPoint) {
        runOnUiThread {
            userMarker?.apply {
                position = location
                title = "Current Position: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
            }

            binding.mapView.overlays.apply {
                clear()
                add(userMarker)
                add(destMark)
            }

            updateRouteLine(location)
            Log.i("LocationTracking", "Marker updated to: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}")

            binding.mapView.controller.setCenter(location)
            binding.mapView.invalidate()
        }
    }


    private fun calculatePosition(beacons: List<Beacon>): Pair<Double, Double> {
        // Filter valid beacons and sort by signal strength
        val validBeacons = beacons.filter { it.rssi < 0 && it.rssi > -100 }
            .sortedBy { abs(it.rssi) }  // Sort by signal strength (closest first)





        // Calculate weights based on RSSI values
        val totalWeight = validBeacons.sumOf {
            // Exponential weight calculation for better accuracy
            Math.pow(2.0, (100.0 + it.rssi) / 20.0)
        }


        var weightedLat = 0.0
        var weightedLon = 0.0


        validBeacons.forEach { beacon ->
            // Calculate weight for this beacon
            val weight = Math.pow(2.0, (100.0 + beacon.rssi) / 20.0) / totalWeight


            weightedLat += beacon.latitude * weight
            weightedLon += beacon.longitude * weight
        }


        // Add some smoothing to prevent jumps
        val smoothingFactor = 0.3 // Adjust this value (0-1) to control smoothing
        val originalGeoPoint = GeoPoint(16.654222, 74.261795);
        val currentLat = userMarker?.position?.latitude ?: originalGeoPoint.latitude
        val currentLon = userMarker?.position?.longitude ?: originalGeoPoint.longitude


        val smoothedLat = currentLat + (weightedLat - currentLat) * smoothingFactor
        val smoothedLon = currentLon + (weightedLon - currentLon) * smoothingFactor


        return Pair(smoothedLat, smoothedLon)
    }


    private fun updateRouteLine(currentLocation: GeoPoint) {
        // Remove existing route line if it exists
        routeLine?.let {
            binding.mapView.overlayManager.remove(it)
        }


        // Create new route line
        routeLine = Polyline(binding.mapView).apply {
            setPoints(listOf(currentLocation, destinationPoint))
            color = Color.BLUE
            width = 5f
            // Optional: make the line smoother
            isGeodesic = true
        }


        // Add new route line
        binding.mapView.overlayManager.add(routeLine)
    }


//    override fun onDestroy() {
//        super.onDestroy()
//        stopScanning()
//    }




// Existing methods like calculatePosition(), updateRouteLine() remain the same

    override fun onDestroy() {
        super.onDestroy()
        stopCompositiveTracking()
    }
}