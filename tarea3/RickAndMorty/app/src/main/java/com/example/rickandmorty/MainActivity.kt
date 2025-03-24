package com.example.rickandmorty
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.SearchView // Esto es correcto
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var searchView: SearchView

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: CharacterAdapter // Necesitarás crear este adaptador
    private lateinit var apiService: RickAndMortyApiService
    private var currentPage = 1
    private var isLoading = false
    private val characters = mutableListOf<Character>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        searchView = findViewById(R.id.searchView)

        recyclerView = findViewById(R.id.recyclerView) // Asegúrate de tener un RecyclerView en tu layout
        progressBar = findViewById(R.id.progressBar) // Asegúrate de tener un ProgressBar en tu layout
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CharacterAdapter(characters) { character -> // Implementa el listener para los clics
            // Aquí iría la lógica para navegar a la pantalla de detalles
            Log.d("Character Click", "Clicked on ${character.name}")
        }

        recyclerView.adapter = adapter

        // Configuración de Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://rickandmortyapi.com/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(RickAndMortyApiService::class.java)

        // Cargar la lista inicial de personajes
        loadCharacters()

        // Implementar la paginación (ejemplo básico)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    currentPage++
                    loadCharacters()
                }
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Se llama cuando el usuario presiona "Enter" o el botón de búsqueda
                query?.let { searchCharacters(it) }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                // Se llama cuando el texto en el SearchView cambia
                newText?.let { searchCharacters(it) }
                return true
            }
        })
    }

    private fun loadCharacters() {
        if (isLoading) return
        isLoading = true
        progressBar.visibility = View.VISIBLE

        val call = apiService.getCharacters(currentPage)
        call.enqueue(object : Callback<CharactersResponse> {
            override fun onResponse(call: Call<CharactersResponse>, response: Response<CharactersResponse>) {
                progressBar.visibility = View.GONE
                isLoading = false
                if (response.isSuccessful) {
                    response.body()?.results?.let { newCharacters ->
                        characters.addAll(newCharacters)
                        adapter.notifyDataSetChanged() // Notificar al adaptador que los datos han cambiado
                    }
                } else {
                    Log.e("API Error", "Error al cargar personajes: ${response.code()}")
                    // Aquí podrías mostrar un mensaje de error al usuario
                }
            }

            override fun onFailure(call: Call<CharactersResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                isLoading = false
                Log.e("Network Error", "Error de red al cargar personajes: ${t.message}")
                // Aquí podrías mostrar un mensaje de error al usuario
            }
        })
    }
    private fun searchCharacters(query: String) {
        if (query.isBlank()) {
            // Si el query está vacío, cargar todos los personajes
            currentPage = 1
            characters.clear()
            loadCharacters()
            return
        }

        isLoading = true
        progressBar.visibility = View.VISIBLE

        val call = apiService.searchCharacters(query)
        call.enqueue(object : Callback<CharactersResponse> {
            override fun onResponse(call: Call<CharactersResponse>, response: Response<CharactersResponse>) {
                progressBar.visibility = View.GONE
                isLoading = false
                if (response.isSuccessful) {
                    response.body()?.results?.let { newCharacters ->
                        characters.clear() // Limpiar la lista anterior
                        characters.addAll(newCharacters)
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    Log.e("API Error", "Error al buscar personajes: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<CharactersResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                isLoading = false
                Log.e("Network Error", "Error de red al buscar personajes: ${t.message}")
            }
        })
    }


}