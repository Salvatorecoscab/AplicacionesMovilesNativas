package com.example.guessthenum // Asegúrate de que este sea tu paquete correcto

// Data class to represent a saved game entry
data class SavedGame(
    val id: String, // Unique ID for the game, e.g., "game_1_timestamp"
    val gameNumber: String, // e.g., "Juego #1" - Podríamos hacerlo más dinámico
    val dateTime: String,   // e.g., "15/05/2024 10:30"
    val difficulty: String,  // e.g., "Fácil", "Medio", "Difícil"
    val attemptsLeft: Int, // Cambiado de 'attempts' para coincidir con la variable de GameActivity
    val secretWord: String,
    val guessedLetters: List<String>, // Cambiado de 'usedLetters'
    val displayedWord: String // Para saber qué letras ya se revelaron
)
