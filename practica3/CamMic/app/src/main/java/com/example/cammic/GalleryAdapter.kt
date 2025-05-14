package com.example.cammic // Asegúrate que este sea tu paquete correcto

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Necesitarás añadir Glide como dependencia si no lo has hecho

class GalleryAdapter(
    private val context: Context,
    private var imageUris: List<Uri>,
    private val onItemClick: (Uri) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.galleryImageView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(imageUris[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Asegúrate que no haya espacios extra aquí: R.layout.item_gallery_image
        val view = LayoutInflater.from(context).inflate(R.layout.item_gallery_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = imageUris[position]
        Glide.with(context)
            .load(uri)
            // Puedes crear drawables simples de color si no tienes estos específicos
            // Por ejemplo, en res/drawable/ crea placeholder_background.xml: <shape xmlns:android...><solid android:color="#CCCCCC"/></shape>
            .placeholder(android.R.drawable.ic_menu_gallery) // Placeholder genérico de Android
            .error(android.R.drawable.ic_dialog_alert) // Error genérico de Android
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = imageUris.size

    fun updateImages(newImageUris: List<Uri>) {
        imageUris = newImageUris
        notifyDataSetChanged()
    }
}
