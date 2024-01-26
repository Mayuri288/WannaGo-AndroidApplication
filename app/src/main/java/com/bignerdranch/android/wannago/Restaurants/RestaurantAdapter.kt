package com.bignerdranch.android.wannago.Restaurants

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.wannago.databinding.RestaurantListItemBinding
import com.google.android.gms.maps.model.LatLng

class RestaurantAdapter(
    private var restaurants: List<Restaurant>,
    private val onLocationClickListener: (LatLng, String) -> Unit
) : RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RestaurantListItemBinding.inflate(inflater, parent, false)
        return RestaurantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        holder.bind(restaurants[position])
    }

    override fun getItemCount() = restaurants.size

    inner class RestaurantViewHolder(private val binding: RestaurantListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(restaurant: Restaurant) {
            binding.placeLatitudeValue.text = restaurant.latitude.toString()
            binding.placeLongitudeValue.text = restaurant.longitude.toString()
            binding.placeAddressValue.text = restaurant.address
            // Tapping a cell in the RecyclerView centers the Map on that location
            // Set up click listener on the itemView
            binding.root.setOnClickListener {
                // Notify the fragment when a cell is tapped
                onLocationClickListener.invoke(
                    LatLng(restaurant.latitude, restaurant.longitude),
                    restaurant.address

                )
            }
        }
    }
}
