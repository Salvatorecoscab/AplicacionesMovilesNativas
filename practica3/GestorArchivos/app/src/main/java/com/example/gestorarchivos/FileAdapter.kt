package com.example.gestorarchivos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import coil.load
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import coil.request.CachePolicy

class FileAdapter(
    private var files: List<FileModel>,
    private val onItemClick: (FileModel) -> Unit,
    private val onFavoriteClick: ((FileModel) -> Unit)? = null, // Nuevo callback para favoritos
    private val showFavoriteIcon: Boolean = true // Opción para mostrar/ocultar icono de favorito
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    // Mapa para rastrear qué archivos son favoritos
    private val favoriteStatus = mutableMapOf<String, Boolean>()

    // Actualizar el estado de favorito de un archivo específico
    fun updateFavoriteStatus(filePath: String, isFavorite: Boolean) {
        favoriteStatus[filePath] = isFavorite
        // Buscar el ítem en la lista y notificar su cambio
        val position = files.indexOfFirst { it.file.absolutePath == filePath }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    // Actualizar todos los estados de favoritos a la vez
    fun setFavoritesStatus(favorites: Map<String, Boolean>) {
        favoriteStatus.clear()
        favoriteStatus.putAll(favorites)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view, showFavoriteIcon)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileModel = files[position]
        val isFavorite = favoriteStatus[fileModel.file.absolutePath] ?: false

        holder.bind(fileModel, isFavorite)
        holder.itemView.setOnClickListener { onItemClick(fileModel) }

        // Configurar el clic en el icono de favorito si corresponde
        holder.setFavoriteClickListener {
            onFavoriteClick?.invoke(fileModel)
        }
    }

    override fun getItemCount(): Int = files.size

    fun updateFiles(newFiles: List<FileModel>) {
        files = newFiles
        notifyDataSetChanged()
    }

    class FileViewHolder(
        itemView: View,
        private val showFavoriteIcon: Boolean
    ) : RecyclerView.ViewHolder(itemView) {
        private val ivFileIcon: ImageView = itemView.findViewById(R.id.ivFileIcon)
        private val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        private val ivFavorite: ImageView = itemView.findViewById(R.id.ivFavorite) // Nuevo ImageView para favorito

        private var favoriteClickListener: (() -> Unit)? = null

        fun bind(fileModel: FileModel, isFavorite: Boolean) {
            tvFileName.text = fileModel.name

            when {
                fileModel.isDirectory -> {
                    // Si es un directorio, mostrar icono de carpeta
                    ivFileIcon.setImageResource(R.drawable.ic_folder)
                }
                isImageFile(fileModel.file) -> {
                    // Si es una imagen, cargar miniatura con Coil
                    ivFileIcon.load(fileModel.file) {
                        crossfade(true)
                        placeholder(R.drawable.ic_image)
                        error(R.drawable.ic_image)
                        size(150, 150) // Tamaño de la miniatura
                        scale(Scale.FIT)

                        // Configuración de caché
                        memoryCacheKey(fileModel.file.absolutePath)
                        diskCacheKey(fileModel.file.absolutePath)
                        memoryCachePolicy(CachePolicy.ENABLED)
                        diskCachePolicy(CachePolicy.ENABLED)

                        // Opcional: Añadir bordes redondeados a las miniaturas
                        transformations(RoundedCornersTransformation(8f))
                    }
                }
                else -> {
                    // Para cualquier otro tipo de archivo
                    ivFileIcon.setImageResource(R.drawable.ic_file)
                }
            }

            // Configurar visibilidad e icono del favorito
            if (showFavoriteIcon) {
                ivFavorite.visibility = View.VISIBLE
                ivFavorite.setImageResource(
                    if (isFavorite) R.drawable.ic_star_filled
                    else R.drawable.ic_star_outline
                )

                // Configurar clic en el icono de favorito
                ivFavorite.setOnClickListener {
                    favoriteClickListener?.invoke()
                }
            } else {
                ivFavorite.visibility = View.GONE
            }
        }

        fun setFavoriteClickListener(listener: () -> Unit) {
            favoriteClickListener = listener
        }

        private fun isImageFile(file: File): Boolean {
            val extension = file.extension.lowercase()
            return extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        }
    }
}
