
import android.util.Log
import androidx.lifecycle.ViewModel
import com.bignerdranch.android.wannago.Restaurants.Restaurant
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "Restaurant ViewModel"
class RestaurantViewModel() : ViewModel() {
    private val db = Firebase.firestore
    private var _locationsFlow = MutableStateFlow<List<Restaurant>>(emptyList())
    var locationsFlow = _locationsFlow.asStateFlow()

    fun getLocations(): StateFlow<List<Restaurant>> = locationsFlow


    fun addLocation(restaurant: Restaurant) {
        val list = listOf(restaurant) + locationsFlow.value
        _locationsFlow.value = list
    }


    // RecyclerView is populated with records from Firestore
    fun getLocationsFromFirestore() {
        db.collection("RestaurantLocations")
            .get()
            .addOnSuccessListener { result ->
                val restaurants = mutableListOf<Restaurant>()
                for (document in result) {
                    val id = document.id
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0
                    val address = document.getString("address") ?: "Unknown Address"

                    val restaurant = Restaurant(address, latitude, longitude)
                    restaurants.add(restaurant)
                }
                _locationsFlow.value = restaurants
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }


    //App stores latitude and longitude to Firestore when Map is tapped
    fun storeLocationInFirestore(restaurant: Restaurant) {
        val locationCollection = db.collection("RestaurantLocations")
        val locationData = hashMapOf(
            "latitude" to restaurant.latitude,
            "longitude" to restaurant.longitude,
            "address" to restaurant.address // Assuming you also want to store the address
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
