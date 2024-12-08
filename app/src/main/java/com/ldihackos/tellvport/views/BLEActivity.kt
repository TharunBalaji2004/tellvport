package com.ldihackos.tellvport.views

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ldihackos.tellvport.databinding.ActivityBleBinding
import org.altbeacon.beacon.*

class BLEActivity : AppCompatActivity(), BeaconConsumer {
    private lateinit var beaconManager: BeaconManager
    private lateinit var beaconAdapter: ArrayAdapter<String>
    private val beaconList = mutableListOf<String>()
    private lateinit var binding: ActivityBleBinding

    companion object {
        private const val PERMISSION_REQUEST_FINE_LOCATION = 1
        private const val TAG = "BeaconScanner"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI
//        beaconListView = findViewById(R.id.beaconListView)
//        beaconAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, beaconList)
//        beaconListView.adapter = beaconAdapter

        // Setup beacon manager
        beaconManager = BeaconManager.getInstanceForApplication(this)

        // Set beacon parser for different beacon types
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        )

        // Check and request permissions
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        val permissionCheck = ContextCompat.checkSelfPermission(
            this@BLEActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val permissionBluetoothCheck = ContextCompat.checkSelfPermission(
            this@BLEActivity,
            Manifest.permission.BLUETOOTH_SCAN
        )

        if (permissionCheck != PackageManager.PERMISSION_GRANTED || permissionBluetoothCheck != PackageManager.PERMISSION_GRANTED) {
            // ask permissions here using below code
            ActivityCompat.requestPermissions(
                this@BLEActivity,
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN),
                PERMISSION_REQUEST_FINE_LOCATION
            )
        } else {
            // Permission granted, start scanning
            initializeBeaconScanning()
        }
    }

    private fun initializeBeaconScanning() {
        try {
            // Bind beacon service
            beaconManager.bind(this)

            // Configure scanning settings
            beaconManager.foregroundScanPeriod = 5000
            beaconManager.foregroundBetweenScanPeriod = 0
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing beacon scanning", e)
            Toast.makeText(this, "Failed to start beacon scanning", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBeaconServiceConnect() {
        // Setup a ranging listener to detect beacons
        beaconManager.addRangeNotifier { beacons, region -> // Clear previous list
            runOnUiThread {
                beaconList.clear()
            }

            // Process detected beacons
            beacons?.forEach { beacon ->
                val beaconInfo = buildBeaconInfoString(beacon)

                runOnUiThread {
                    beaconList.add(beaconInfo)
                    beaconAdapter.notifyDataSetChanged()
                }
            }

            // Log the number of beacons found
            Log.d(TAG, "Beacons found: ${beacons?.size ?: 0}")
        }

        // Start ranging beacons in all regions
        try {
            beaconManager.startRangingBeaconsInRegion(Region("myRangingUniqueId", null, null, null))
        } catch (e: Exception) {
            Log.e(TAG, "Cannot start ranging", e)
        }
    }

    private fun buildBeaconInfoString(beacon: Beacon): String {
        return """
            UUID: ${beacon.id1}
            Major: ${beacon.id2}
            Minor: ${beacon.id3}
            RSSI: ${beacon.rssi} dBm
            Distance: ${String.format("%.2f", beacon.distance)} m
            Manufacturer: ${beacon.manufacturer}
        """.trimIndent()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeBeaconScanning()
                } else {
                    Toast.makeText(
                        this,
                        "Location permission is required for beacon scanning",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unbind from beacon service
        beaconManager.unbind(this)
    }
}