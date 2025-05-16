package com.example.guessthenum // Asegúrate de que este sea tu paquete correcto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LoadGameAdapter(
    private val savedGames: MutableList<SavedGame>,
    // Asegúrate de que los nombres de estos parámetros (onItemClick, onDeleteClick)
    // y sus tipos ((SavedGame) -> Unit) sean exactamente estos.
    private val onItemClick: (game: SavedGame) -> Unit,
    private val onDeleteClick: (game: SavedGame) -> Unit
) : RecyclerView.Adapter<LoadGameAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gameNumberTextView: TextView = view.findViewById(R.id.textViewGameNumber)
        val gameDateTimeTextView: TextView = view.findViewById(R.id.textViewGameDateTime)
        val gameDifficultyTextView: TextView = view.findViewById(R.id.textViewGameDifficulty)
        val deleteButton: ImageButton = view.findViewById(R.id.buttonDeleteGame)

        // Método para vincular los datos con los listeners
        fun bind(game: SavedGame, onItemClick: (SavedGame) -> Unit, onDeleteClick: (SavedGame) -> Unit) {
            gameNumberTextView.text = game.gameNumber
            gameDateTimeTextView.text = game.dateTime
            gameDifficultyTextView.text = "Dificultad: ${game.difficulty}"

            deleteButton.setOnClickListener {
                onDeleteClick(game)
            }

            itemView.setOnClickListener {
                onItemClick(game)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_load_game, parent, false) // Asegúrate que este layout exista
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = savedGames[position]
        // Llama al método bind del ViewHolder
        holder.bind(game, onItemClick, onDeleteClick)
    }

    override fun getItemCount() = savedGames.size
}
