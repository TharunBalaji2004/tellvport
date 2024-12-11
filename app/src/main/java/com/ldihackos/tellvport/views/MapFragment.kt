package com.ldihackos.tellvport.views

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ldihackos.tellvport.R
import com.ldihackos.tellvport.databinding.FragmentMapBinding
import com.ldihackos.tellvport.utils.LanguagePref
import com.ldihackos.tellvport.utils.setLocale
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment(R.layout.fragment_map), MapListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val INITIAL_ZOOM_LEVEL = 20.0 // The initial zoom level (user starts at zoom level 20)
    private val MAX_ZOOM_LEVEL = 22.0    // Allow zoom-in to level 30
    private val MIN_ZOOM_LEVEL = 20.0    // Disallow zooming out below level 20

    private var hasMapMoved = false

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isScanning = false

    private var userMarker: Marker? = null

    private val beaconMap: HashMap<String, Beacon> = hashMapOf(
        "KBPro_469145" to Beacon(16.654441, 74.261921, -80),
        "KBPro_469166" to Beacon(16.654250, 74.261924, -85),
        "KBPro_469111" to Beacon(16.654363, 74.262109, -80)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load configuration settings
        val context = requireContext()
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))

        binding.imgChangeLang.setOnClickListener {
            val changeLang = when ((activity as HomeActivity).getLanguage()) {
                "en" -> "ta"
                "ta" -> "hi"
                "hi" -> "en"
                else -> "en"
            }

            (activity as HomeActivity).updateLanguage(changeLang)
        }

        // Initialize the map
        binding.mapView.setMultiTouchControls(true)

        // Disable the zoom buttons (default zoom in and out buttons)
        binding.mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        // Set the initial map center and zoom level
        val originalGeoPoint = GeoPoint(16.654222, 74.261795) // Chennai Central
        val mapController = binding.mapView.controller
        mapController.setZoom(INITIAL_ZOOM_LEVEL) // Set initial zoom to level 20
        mapController.setCenter(originalGeoPoint)

        // Add a marker to the map
        userMarker = Marker(binding.mapView)
        userMarker?.position = originalGeoPoint
        userMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        userMarker?.title = "You are here"
        binding.mapView.overlays.add(userMarker)

        binding.mapView.minZoomLevel = MIN_ZOOM_LEVEL  // Restrict zoom-out to 20
        binding.mapView.maxZoomLevel = MAX_ZOOM_LEVEL  // Allow zoom-in up to 30

        binding.cardSync.setOnClickListener {
            resetMapViewToOriginalState()
        }

        binding.cardFoodcourts.setOnClickListener {
            if (binding.llSheet.visibility == View.GONE) {
                binding.cardSync.visibility = View.GONE
                binding.llSheet.visibility = View.VISIBLE
            }
            else {
                binding.llSheet.visibility = View.GONE
                binding.cardSync.visibility = View.VISIBLE
            }
        }

        binding.btnClose.setOnClickListener {
            binding.llSheet.visibility = View.GONE
            binding.cardSync.visibility = View.VISIBLE
        }

        binding.mapView.addMapListener(this)

        // Restore the hasMapMoved state
        savedInstanceState?.let {
            hasMapMoved = it.getBoolean("hasMapMoved", false)
            if (hasMapMoved) {
                binding.cardSync.visibility = View.VISIBLE
            }
        }

        // Initialize Bluetooth Adapter
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(requireContext(), "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        checkPermissionsAndStartScanning()
    }

    // Function to reset map view to its original state
    private fun resetMapViewToOriginalState() {
        val mapController = binding.mapView.controller
        mapController.setZoom(INITIAL_ZOOM_LEVEL) // Reset zoom to initial level
        mapController.setCenter(GeoPoint(16.654222, 74.261795)) // Reset the map center to the original point
        binding.cardSync.visibility = View.GONE // Hide the reset button after resetting the map
        hasMapMoved = false
    }

    // Save the hasMapMoved state
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("hasMapMoved", hasMapMoved)
    }

    // MapListener interface methods
    override fun onScroll(event: ScrollEvent?): Boolean {
        if (!hasMapMoved) {
            binding.cardSync.visibility = View.VISIBLE
            hasMapMoved = true
        }
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        if (!hasMapMoved) {
            binding.cardSync.visibility = View.VISIBLE
            hasMapMoved = true
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopScanning()
        // Avoid memory leaks by nullifying the binding reference
        _binding = null
    }

    private fun checkPermissionsAndStartScanning() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                Toast.makeText(requireContext(), "Permissions denied. Cannot scan for BLE devices.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun startScanning() {
        if (isScanning) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Bluetooth Scan permission not granted", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show()
                return
            }
        }

        bluetoothLeScanner?.startScan(leScanCallback)
        isScanning = true
        Toast.makeText(requireContext(), "Scanning for BLE devices...", Toast.LENGTH_SHORT).show()
    }

    private fun stopScanning() {
        if (!isScanning) return

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        bluetoothLeScanner?.stopScan(leScanCallback)
        isScanning = false
        //Toast.makeText(requireContext(), "Scanning stopped.", Toast.LENGTH_SHORT).show()
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
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

                requireActivity().runOnUiThread {
                    // Update marker position with a focus on longitude
                    val currentLatitude = userMarker?.position?.latitude ?: GeoPoint(16.654222, 74.261795).latitude
                    val newGeoPoint = GeoPoint(currentLatitude, currentPosition.second) // Only longitude changes
                    userMarker?.let {
                        it.position = newGeoPoint
                        it.title = "Current Longitude: ${currentPosition.second}"
                    }
                    binding.mapView.overlays.add(userMarker)

                    // Refresh map overlays
                    binding.mapView.invalidate()

                    // Center map on new position
//                    val mapController = binding.mapView.controller
//                    mapController.setCenter(newGeoPoint)
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
}

// Beacon class for storing beacon data (latitude, longitude, and RSSI)
data class Beacon(val latitude: Double, val longitude: Double, val rssi: Int)