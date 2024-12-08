package com.ldihackos.tellvport.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
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
    private lateinit var originalGeoPoint: GeoPoint // To store the original map center

    private var hasMapMoved = false

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
        val context = requireContext()  // Use the fragment's context
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))

        // Initialize the map
        binding.mapView.setMultiTouchControls(true)

        // Disable the zoom buttons (default zoom in and out buttons)
        binding.mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        binding.imgChangeLang.setOnClickListener {
            val currentLang = LanguagePref.getLanguage(requireContext())
            val newLang = when (currentLang) {
                "en" -> "ta"
                "ta" -> "hi"
                else -> "en"
            }

            (activity as? HomeActivity)?.updateLanguage(newLang)
        }
        // Set the initial map center and zoom level
        originalGeoPoint = GeoPoint(13.083637554100747, 80.27421251402276) // Chennai Central
        val mapController = binding.mapView.controller
        mapController.setZoom(INITIAL_ZOOM_LEVEL) // Set initial zoom to level 20
        mapController.setCenter(originalGeoPoint)

        // Add a marker to the map
        val marker = Marker(binding.mapView)
        marker.position = originalGeoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "You are here"
        binding.mapView.overlays.add(marker)

        binding.mapView.minZoomLevel = MIN_ZOOM_LEVEL  // Restrict zoom-out to 20
        binding.mapView.maxZoomLevel = MAX_ZOOM_LEVEL  // Allow zoom-in up to 30

        binding.cardSync.setOnClickListener {
            resetMapViewToOriginalState()
            (activity as? HomeActivity)?.toggleBottomBar()
        }

        binding.mapView.addMapListener(this)

        // Restore the hasMapMoved state
        savedInstanceState?.let {
            hasMapMoved = it.getBoolean("hasMapMoved", false)
            if (hasMapMoved) {
                binding.cardSync.visibility = View.VISIBLE
            }
        }
    }

    // Function to reset map view to its original state
    private fun resetMapViewToOriginalState() {
        val mapController = binding.mapView.controller
        mapController.setZoom(INITIAL_ZOOM_LEVEL) // Reset zoom to initial level
        mapController.setCenter(originalGeoPoint) // Reset the map center to the original point
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
        // Avoid memory leaks by nullifying the binding reference
        _binding = null
    }
}
