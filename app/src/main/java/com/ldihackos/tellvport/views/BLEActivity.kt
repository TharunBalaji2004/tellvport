package com.ldihackos.tellvport.views

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ldihackos.tellvport.databinding.ActivityBleBinding
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.CustomZoomButtonsController
import java.util.*
import kotlin.math.abs
import kotlinx.coroutines.*
import java.util.LinkedList
import java.util.Queue

@AndroidEntryPoint
class BLEActivity : AppCompatActivity() {

    private var _binding: ActivityBleBinding? = null
    private val binding get() = _binding!!

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isScanning = false
    private var userMarker: Marker? = null

    private val INITIAL_ZOOM_LEVEL = 21.0
    private val MAX_ZOOM_LEVEL = 22.0
    private val MIN_ZOOM_LEVEL = 19.0

    private lateinit var originalGeoPoint: GeoPoint

    // Define a HashMap to store beacons
    private val beaconMap: HashMap<String, Beacon> = hashMapOf(
        "KBPro_469145" to Beacon(16.654441, 74.261921, -80),
        "KBPro_469166" to Beacon(16.654250, 74.261924, -85),
        "KBPro_469111" to Beacon(16.654363, 74.262109, -80)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityBleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // MAP CONFIGURATION
        Configuration.getInstance().load(this, this.getSharedPreferences("osmdroid", 0))
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        originalGeoPoint = GeoPoint(16.654222, 74.261795)
        val mapController = binding.mapView.controller
        mapController.setZoom(INITIAL_ZOOM_LEVEL)
        mapController.setCenter(originalGeoPoint)

        // Initialize marker
        userMarker = Marker(binding.mapView)
        userMarker?.position = originalGeoPoint
        userMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        userMarker?.title = "You are here"

        binding.mapView.minZoomLevel = MIN_ZOOM_LEVEL
        binding.mapView.maxZoomLevel = MAX_ZOOM_LEVEL


        // Initialize Bluetooth Adapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        binding.btnStartScan.setOnClickListener {
            if (!isScanning) {
                checkPermissionsAndStartScanning()
            } else {
                Toast.makeText(this, "Already scanning", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStopScan.setOnClickListener {
            if (isScanning) {
                stopScanning()
            } else {
                Toast.makeText(this, "Not currently scanning", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionsAndStartScanning() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startScanning()
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                startScanning()
            } else {
                Toast.makeText(this, "Permissions denied. Cannot scan for BLE devices.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun startScanning() {
        if (isScanning) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth Scan permission not granted", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
                return
            }
        }

        bluetoothLeScanner?.startScan(leScanCallback)
        isScanning = true
        Toast.makeText(this, "Scanning for BLE devices...", Toast.LENGTH_SHORT).show()
    }

    private fun stopScanning() {
        if (!isScanning) return

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        bluetoothLeScanner?.stopScan(leScanCallback)
        isScanning = false
        Toast.makeText(this, "Scanning stopped.", Toast.LENGTH_SHORT).show()
    }

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
                Log.d("BLE_SCAN", "$deviceName Found, RSSI: $rssi")

                // Update the RSSI for the beacon in the map
                val beacon = beaconMap[deviceName]?.copy(rssi = rssi)
                beacon?.let {
                    beaconMap[deviceName] = it
                }

                // Recalculate position
                val currentPosition = calculatePosition(
                    beaconMap.values.toList()
                )

                runOnUiThread {
                    // Update marker position with a focus on longitude
                    val currentLatitude = userMarker?.position?.latitude ?: originalGeoPoint.latitude
                    val newGeoPoint = GeoPoint(currentLatitude, currentPosition.second) // Only longitude changes
                    userMarker?.let {
                        it.position = newGeoPoint
                        it.title = "Current Longitude: ${currentPosition.second}"
                    }
                    binding.mapView.overlays.add(userMarker)

                    // Refresh map overlays
                    binding.mapView.invalidate()

                    // Center map on new position
                    val mapController = binding.mapView.controller
                    mapController.setCenter(newGeoPoint)
                }

            }
        }
    }

    private fun calculatePosition(beacons: List<Beacon>): Pair<Double, Double> {
        // Perform position calculation logic similar to the previous implementation
        val validBeacons = beacons.filter { it.rssi < 0 && it.rssi > -100 }

        if (validBeacons.isEmpty()) {
            return Pair(0.0, 0.0) // Default fallback
        }

        val totalWeight = validBeacons.sumOf { 100 + it.rssi }.toDouble()
        var weightedLat = 0.0
        var weightedLon = 0.0

        validBeacons.forEach { beacon ->
            val weight = (100 + beacon.rssi) / totalWeight
            weightedLat += beacon.latitude * weight
            weightedLon += beacon.longitude * weight
        }

        return Pair(weightedLat, weightedLon)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
    }
}

// Beacon class for storing beacon data (latitude, longitude, and RSSI)
