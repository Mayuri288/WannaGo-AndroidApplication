package com.bignerdranch.android.wannago.Parks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bignerdranch.android.wannago.databinding.FragmentParkDetailBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ParkDetailFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentParkDetailBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding is not initialized" }

    private lateinit var mapView: MapView
    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParkDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = binding.mapView
        latitudeTextView = binding.latitude
        longitudeTextView = binding.longitude

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Retrieve data passed from the RecyclerView item click
        val args = arguments?.let { ParkDetailFragmentArgs.fromBundle(it) }
        val location = args?.location

        // Display latitude and longitude
        latitudeTextView.text = "Latitude: ${location?.latitude}"
        longitudeTextView.text = "Longitude: ${location?.longitude}"
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val location = arguments?.let { ParkDetailFragmentArgs.fromBundle(it)?.location }
        if (location != null) {
            val latLng = LatLng(location.latitude, location.longitude)
            googleMap.addMarker(MarkerOptions().position(latLng).title("Marker"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
