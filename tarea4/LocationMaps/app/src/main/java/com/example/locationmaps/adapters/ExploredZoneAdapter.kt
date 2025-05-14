package com.example.locationmaps.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.locationmaps.R
import com.example.locationmaps.data.ExploredZone
import java.text.SimpleDateFormat
import java.util.Locale

class ExploredZoneAdapter(
    private var zones: List<ExploredZone>,
    private val onZoneClicked: (ExploredZone) -> Unit
) : RecyclerView.Adapter<ExploredZoneAdapter.ZoneViewHolder>() {

    class ZoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.findViewById(R.id.zoneCard)
        val nameTextView: TextView = itemView.findViewById(R.id.zoneNameTextView)
        val statusTextView: TextView = itemView.findViewById(R.id.zoneStatusTextView)
        val coordinatesTextView: TextView = itemView.findViewById(R.id.zoneCoordinatesTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.zoneExploredDateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_zone, parent, false)
        return ZoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        val zone = zones[position]
        val context = holder.itemView.context

        holder.nameTextView.text = zone.name

        if (zone.isExplored) {
            holder.statusTextView.text = "EXPLORADO"
            holder.statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.exploredZoneBackground))

            zone.dateExplored?.let { date ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                holder.dateTextView.text = "Explorado el: ${dateFormat.format(date)}"
                holder.dateTextView.visibility = View.VISIBLE
            } ?: run {
                holder.dateTextView.visibility = View.GONE
            }
        } else {
            holder.statusTextView.text = "POR EXPLORAR"
            holder.statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.unexploredZoneBackground))
            holder.dateTextView.visibility = View.GONE
        }

        holder.coordinatesTextView.text = "Lat: ${zone.centerLatitude}, Lng: ${zone.centerLongitude}"

        holder.itemView.setOnClickListener {
            onZoneClicked(zone)
        }
    }

    override fun getItemCount() = zones.size

    fun updateZones(newZones: List<ExploredZone>) {
        zones = newZones
        notifyDataSetChanged()
    }
}