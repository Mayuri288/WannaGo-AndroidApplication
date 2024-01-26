package com.bignerdranch.android.wannago.Restaurants

import RestaurantViewModel
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.LocationRequest
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bignerdranch.android.wannago.databinding.FragmentRestaurantBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale


private const val TAG = "RestaurantFragment"
private const val DEFAULT_ZOOM = 15f
class RestaurantFragment: Fragment() , OnMapReadyCallback {
    private var _binding: FragmentRestaurantBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Unable to access binding. Is view created"
        }

    // location objects to fetch and retrieve location updates
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // location variables to store current location and permission grants
    private var currentLocation: Location? = null
    private var locationPermissionGranted: Boolean = false

    private var map: GoogleMap? = null
    private val defaultLocation = LatLng(35.7763139,-102.4360321)

    private val restaurantViewModel: RestaurantViewModel by viewModels()

    /**
     * This variable refers to the popup that asks the user
     * if he/she allows the app to access his/her location
     */
    @SuppressLint("MissingPermission")
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            locationPermissionGranted = permissions.entries.all {
                it.value
            }

            if (locationPermissionGranted) {
                // starts requesting for location updates
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestaurantBinding.inflate(inflater, container, false)
        if (!locationEnabled()) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }

        // configuration object for requesting location updates
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        // the function that gets called when location requests are returned
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                currentLocation = locationResult.lastLocation

                if (currentLocation != null && map != null) {
                    Log.d(TAG, "$currentLocation")
                    updateMapLocation(currentLocation)
                    updateMapUI()
                    // once we get a location, we can stop requesting for updates
                    // if we do not do this, the phone will continually check for updates
                    // which will use battery power
                    fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                }
            }
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        // checking if the app has the appropriate permissions
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
        } else { // launch the dialog requesting for permissions
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.restaurantRecyclerView.layoutManager = LinearLayoutManager(context)
        // RecyclerView is populated with records from Firestore
        restaurantViewModel.getLocationsFromFirestore()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect values from locationsFlow to update the RecyclerView when data changes
                restaurantViewModel.locationsFlow.collect { latLngs ->
                    // Tapping a cell in the RecyclerView centers the Map on that location
                    binding.restaurantRecyclerView.adapter = RestaurantAdapter(latLngs) { clickedLatLng, clickedAddress->
                        centerMapOnLocation(clickedLatLng)
                        restaurantViewModel.addLocation(Restaurant(clickedAddress, clickedLatLng.latitude, clickedLatLng.longitude))

                 //       Tapping a cell in the RecyclerView loads a Detail Fragment that shows the location on a Map and the Latitude and Longitude in text fields
                        val action = RestaurantFragmentDirections.actionRestaurantFragmentToRestaurantDetailFragment(
                            LatLng(clickedLatLng.latitude, clickedLatLng.longitude)
                        )
                        findNavController().navigate(action)
                    }



                    // Map shows a marker at each location stored in Firestore
                    latLngs.forEach { latLng ->
                        addMarkerToMap(latLng, map!!)
                    }
                }
            }
        }
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
    }
    // Tapping a cell in the RecyclerView centers the Map on that location
    private fun centerMapOnLocation(location: LatLng) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM))
    }
    /**
     * function that checks if location services is enabled
     */
    private fun locationEnabled(): Boolean {
        val locationManager: LocationManager =
            this.requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }



    @SuppressLint("MissingPermission")
    override fun onMapReady(p0: GoogleMap) {
        map = p0

        // Check if the current location is available and permission is granted
        if (locationPermissionGranted) {
            if (currentLocation != null) {
                // Move the camera to the user's current location
                map?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(currentLocation!!.latitude, currentLocation!!.longitude),
                       DEFAULT_ZOOM
                    )
                )

                // Enable the "My Location" layer on the map
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                // Handle the case where location is not available
                // Move the camera to the default location
                map?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(defaultLocation.latitude, defaultLocation.longitude),
                        DEFAULT_ZOOM
                    )
                )

                // Disable the "My Location" layer on the map
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
            }
        } else {
            // Handle the case where location permission is not granted
            // Move the camera to the default location
            map?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(defaultLocation.latitude, defaultLocation.longitude),
                    DEFAULT_ZOOM
                )
            )

            // Disable the "My Location" layer on the map
            map?.isMyLocationEnabled = false
            map?.uiSettings?.isMyLocationButtonEnabled = false
        }

        updateMapUI()

        // Set up a marker for each location from locationsFlow
        restaurantViewModel.getLocations().value.forEach { restaurant ->
            addMarkerToMap(restaurant, map!!)
        }

        map?.setOnMapClickListener { latLng ->
            // Use Places SDK to get place details
            getPlaceDetails(latLng) { placeName, address ->
                val restaurant = Restaurant(address, latLng.latitude, latLng.longitude)

                addMarkerToMap(restaurant, map!!)
                restaurantViewModel.addLocation(restaurant)
                // Adding data to Firestore
                restaurantViewModel.storeLocationInFirestore(restaurant)
            }
        }
        binding.mapView.onResume()
    }


    private fun addMarkerToMap(restaurant: Restaurant, map: GoogleMap) {
        map.addMarker(
            MarkerOptions()
                .position(LatLng(restaurant.latitude, restaurant.longitude))
                .title(restaurant.address)
        )
    }


    private fun updateMapUI() {
        try {
         //   Show the user's current location on the Map
            if (map == null) return
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    //When a new marker/location is added to the Map, update the Map so that it is centered between all stored points
    private fun updateMapLocation(location: Location?) {
        try {
            if (map == null || location == null) return
            if (!locationPermissionGranted) {
                map?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        defaultLocation,
                        DEFAULT_ZOOM
                    )
                )
                return
            }

            // Get all the LatLng points from locationsFlow
            val allRestaurants = restaurantViewModel.getLocations().value

            // Calculate the bounds to include all LatLng points
            val builder = LatLngBounds.Builder()
            for (restaurant in allRestaurants) {
                builder.include(LatLng(restaurant.latitude, restaurant.longitude))
            }
            val bounds = builder.build()

            // Move the camera to show all LatLng points
            map?.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 100) // Adjust the padding as needed
            )
        } catch (e: Exception) {
            e.message?.let { Log.e(TAG, it) }
        }
    }



    //  Use Google's Places SDK to automatically retrieve the name or address of the point that was tapped,
  //  store that in Firestore, and show the place name in the RecyclerView with the coordinates**
    private fun getPlaceDetails(latLng: LatLng, callback: (String, String) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

                if (addresses != null && addresses.isNotEmpty()) {
                    val placeName = addresses[0].featureName ?: "Unknown Place"
                    val address = addresses[0].getAddressLine(0) ?: "Unknown Address"
                    callback.invoke(placeName, address)
                } else {
                    callback.invoke("Unknown Place", "Unknown Address")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error getting place details: ${e.message}", e)
                callback.invoke("Unknown Place", "Unknown Address")
            }
        } else {
            // Handle the case where permission is not granted
            callback.invoke("Permission Denied", "Permission Denied")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}