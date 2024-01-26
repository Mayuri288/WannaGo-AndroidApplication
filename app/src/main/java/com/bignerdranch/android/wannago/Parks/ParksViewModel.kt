package com.bignerdranch.android.wannago.Parks

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "Park ViewModel"
class ParksViewModel: ViewModel() {
    private val db = Firebase.firestore
    private var _locationsFlow = MutableStateFlow<List<Parks>>(emptyList())
    var locationsFlow = _locationsFlow.asStateFlow()


    fun getLocations(): StateFlow<List<Parks>> = locationsFlow

    fun addLocation(park: Parks) {
        val list = listOf(park) + locationsFlow.value
        _locationsFlow.value = list
    }


    // RecyclerView is populated with records from Firestore
    fun getLocationsFromFirestore() {
        db.collection("ParksLocations")
            .get()
            .addOnSuccessListener { result ->
                val parks = mutableListOf<Parks>()
                for (document in result) {
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0
                    val address = document.getString("address") ?: "Unknown Address"

                    val park = Parks(address, latitude, longitude)
                    parks.add(park)
                }
                _locationsFlow.value = parks
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }


    //App stores latitude and longitude to Firestore when Map is tapped
    fun storeLocationInFirestore(park: Parks) {
        val locationCollection = db.collection("ParksLocations")
        val locationData = hashMapOf(
            "latitude" to park.latitude,
            "longitude" to park.longitude,
            "address" to park.address // Assuming you also want to store the address
        )

        locationCollection.add(locationData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Location added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding location", e)
            }
    }
}