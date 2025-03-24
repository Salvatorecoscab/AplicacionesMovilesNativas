package com.example.rickandmorty

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Asegúrate de agregar la dependencia de Glide en build.gradle

class CharacterAdapter(
    private val characters: List<Character>,
    private val onItemClick: (Character) -> Unit // Callback para el clic en un elemento
) : RecyclerView.Adapter<CharacterAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.characterImageView) // Asegúrate de tener este ID en el layout del item
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView) // Asegúrate de tener este ID
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView) // Asegúrate de tener este ID

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(characters[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_character, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = characters[position]
        holder.nameTextView.text = currentItem.name
        holder.statusTextView.text = currentItem.status

        // Cargar la imagen usando Glide
        Glide.with(holder.itemView.context)
            .load(currentItem.image)
            .placeholder(android.R.drawable.ic_menu_gallery) // Opcional: imagen de placeholder
            .error(android.R.drawable.ic_delete) // Opcional: imagen de error
            .into(holder.imageView)
    }

    override fun getItemCount() = characters.size
}