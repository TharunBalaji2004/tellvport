package com.ldihackos.tellvport.views

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ldihackos.tellvport.R
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@AndroidEntryPoint
class RoutingActivity : AppCompatActivity() {
    private var map: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        Configuration.getInstance().load(this, this.getSharedPreferences("osmdroid", 0))
        setContentView(R.layout.activity_routing)

        // Sample coordinates (replace with your array)
        val coordinates = arrayOf(
            doubleArrayOf(16.654353, 74.262023), // Start
            doubleArrayOf(16.654232, 74.262013),
            doubleArrayOf(16.654114, 74.262007),  // End
        )


        // Initialize map
        map = findViewById(R.id.map_view)
        setupMap()


        // Draw route
        drawRoute(coordinates)
    }

    private fun setupMap() {
        map!!.setBuiltInZoomControls(true)
        map!!.setMultiTouchControls(true)


        // Set default zoom and center point
        map!!.controller.setZoom(20.0)
        map!!.controller.setCenter(GeoPoint(16.654363, 74.262050)) // Center of US
    }

    private fun drawRoute(coordinates: Array<DoubleArray>) {
        // Create a list of GeoPoints from coordinates
        val routePoints: MutableList<GeoPoint> = ArrayList()
        for (coord in coordinates) {
            routePoints.add(GeoPoint(coord[0], coord[1]))
        }


        // Create and style the route line
        val routeLine = Polyline(map)
        routeLine.setPoints(routePoints)
        routeLine.color = Color.BLUE
        routeLine.width = 5f


        // Add markers for start and end points
        addMarker(routePoints[0], "Start")
        addMarker(routePoints[routePoints.size - 1], "End")


        // Add the route line to the map
        map!!.overlayManager.add(routeLine)


        // Force a redraw
        map!!.invalidate()
    }

    private fun addMarker(point: GeoPoint, title: String) {
        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = title
        map!!.overlayManager.add(marker)
    }

    override fun onResume() {
        super.onResume()
        map!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        map!!.onPause()
    }
}