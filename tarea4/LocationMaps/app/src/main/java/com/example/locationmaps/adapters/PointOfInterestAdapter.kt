package com.example.locationmaps.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.locationmaps.R
import com.example.locationmaps.data.PointOfInterest
import java.io.File

class PointOfInterestAdapter(
    private var pois: List<PointOfInterest>,
    private val onNavigateClicked: (PointOfInterest) -> Unit,
    private val onDeleteClicked: (PointOfInterest) -> Unit,
    private val onPoiClicked: ((PointOfInterest) -> Unit)? = null
) : RecyclerView.Adapter<PointOfInterestAdapter.POIViewHolder>() {

    class POIViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.poiNameTextView)
        val categoryTextView: TextView = itemView.findViewById(R.id.poiCategoryTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.poiDescriptionTextView)
        val navigateButton: Button = itemView.findViewById(R.id.poiNavigateButton)
        val deleteButton: Button = itemView.findViewById(R.id.poiDeleteButton)
        // Añadir el thumbnail ImageView
        val thumbnailImageView: ImageView = itemView.findViewById(R.id.poiThumbnailImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poi, parent, false)
        return POIViewHolder(view)
    }

    override fun onBindViewHolder(holder: POIViewHolder, position: Int) {
        val poi = pois[position]

        holder.nameTextView.text = poi.name
        holder.categoryTextView.text = poi.category
        holder.descriptionTextView.text = poi.description

        // Manejar la foto si existe
        poi.photoPath?.let { photoPath ->
            val photoFile = File(photoPath)
            if (photoFile.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(photoPath)
                    holder.thumbnailImageView.setImageBitmap(bitmap)
                    holder.thumbnailImageView.visibility = View.VISIBLE
                } catch (e: Exception) {
                    holder.thumbnailImageView.visibility = View.GONE
                }
            } else {
                holder.thumbnailImageView.visibility = View.GONE
            }
        } ?: run {
            holder.thumbnailImageView.visibility = View.GONE
        }

        // Configurar listeners de botones
        holder.navigateButton.setOnClickListener {
            onNavigateClicked(poi)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClicked(poi)
        }

        // Listener para el elemento completo
        holder.itemView.setOnClickListener {
            onPoiClicked?.invoke(poi)
        }
    }

    override fun getItemCount() = pois.size

    fun updatePOIs(newPOIs: List<PointOfInterest>) {
        pois = newPOIs
        notifyDataSetChanged()
    }
}