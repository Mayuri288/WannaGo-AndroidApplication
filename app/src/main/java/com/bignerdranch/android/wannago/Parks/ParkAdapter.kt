package com.bignerdranch.android.wannago.Parks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.wannago.databinding.ParkListItemBinding
import com.google.android.gms.maps.model.LatLng

class ParkAdapter(
    private var parks: List<Parks>,
    private val onLocationClickListener: (LatLng, String) -> Unit
) : RecyclerView.Adapter<ParkAdapter.ParkViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ParkListItemBinding.inflate(inflater, parent, false)
        return ParkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParkViewHolder, position: Int) {
        holder.bind(parks[position])
    }

    override fun getItemCount() = parks.size

    inner class ParkViewHolder(private val binding: ParkListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(park: Parks) {
            binding.placeLatitudeValue.text = park.latitude.toString()
            binding.placeLongitudeValue.text = park.longitude.toString()
            binding.placeAddressValue.text = park.address
            // Tapping a cell in the RecyclerView centers the Map on that location
            // Set up click listener on the itemView
            binding.root.setOnClickListener {
                // Notify the fragment when a cell is tapped
                onLocationClickListener.invoke(
                    LatLng(park.latitude, park.longitude),
                    park.address
                )
            }
        }
    }
}
