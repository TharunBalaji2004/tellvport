package com.ldihackos.tellvport

import kotlin.math.pow
import kotlin.math.sqrt

data class BeaconLocation(
    val latitude: Double,
    val longitude: Double,
    val distance: Double
)

data class Position(
    val latitude: Double,
    val longitude: Double
)

fun calculateDistance(
    rssi: Int,
    txPower: Int,
    n: Double = 2.0 // Default path loss exponent
): Double {
    // Ensure RSSI is not zero to avoid mathematical errors
    if (rssi == 0) return Double.MAX_VALUE

    // Basic log-distance path loss model
    // Distance = 10 ^ ((Tx Power - RSSI) / (10 * Path Loss Exponent))
    val distance = 10.0.pow(
        (txPower - rssi) / (10.0 * n)
    )

    // Typically, we don't want negative or extremely large distances
    return maxOf(0.0, minOf(distance, 100.0))
}


fun calculatePosition(beacons: List<BeaconLocation>): Position {
    // Ensure we have exactly 3 beacons
    require(beacons.size == 3) { "Exactly 3 beacons are required for trilateration" }

    // Earth's radius in kilometers (used for distance calculations)
    val earthRadius = 6371.0

    // Convert latitude and longitude to radians
    val beaconRadians = beacons.map { beacon ->
        Triple(
            Math.toRadians(beacon.latitude),
            Math.toRadians(beacon.longitude),
            beacon.distance / earthRadius
        )
    }

    // Perform trilateration calculation
    var x = 0.0
    var y = 0.0
    var z = 0.0

    beaconRadians.forEach { (lat, lon, dist) ->
        val cosLat = kotlin.math.cos(lat)
        val sinLat = kotlin.math.sin(lat)
        val cosLon = kotlin.math.cos(lon)
        val sinLon = kotlin.math.sin(lon)

        x += (cosLat * cosLon) * dist
        y += (cosLat * sinLon) * dist
        z += sinLat * dist
    }

    // Normalize the result
    val total = sqrt(x * x + y * y + z * z)
    x /= total
    y /= total
    z /= total

    // Convert back to latitude and longitude
    val finalLat = Math.toDegrees(kotlin.math.asin(z))
    val finalLon = Math.toDegrees(kotlin.math.atan2(y, x))

    return Position(finalLat, finalLon)
}

// Example usage
fun main() {
    val beacons = listOf(
        BeaconLocation(16.654266, 74.262526, calculateDistance(-70, -59)), // Beacon 1
        BeaconLocation(16.654202, 74.262557, calculateDistance(-90, -59)), // Beacon 2
        BeaconLocation(16.654230, 74.262508, calculateDistance(-50, -59))  // Beacon 3
    )

    val currentPosition = calculatePosition(beacons)
    println("Estimated Position: ${currentPosition.latitude}, ${currentPosition.longitude}")
}