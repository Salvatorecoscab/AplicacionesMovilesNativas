package com.example.apirest

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView) // Asegúrate de tener un TextView en tu layout

        // Configuración de Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080") // Usa la dirección IP del emulador
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // Realizar la llamada a la API
        fetchHelloWorld()
    }

    private fun fetchHelloWorld() {
        val call = apiService.getHelloWorld()

        call.enqueue(object : Callback<Message> {
            override fun onResponse(call: Call<Message>, response: Response<Message>) {
                if (response.isSuccessful) {
                    val message = response.body()?.message
                    textView.text = message ?: "Mensaje no disponible"
                    Log.d("Retrofit", "Respuesta exitosa: $message")
                } else {
                    textView.text = "Error: ${response.code()}"
                    Log.e("Retrofit", "Error en la respuesta: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Message>, t: Throwable) {
                textView.text = "Error de red: ${t.message}"
                Log.e("Retrofit", "Error de red: ${t.message}")
            }
        })
    }
}