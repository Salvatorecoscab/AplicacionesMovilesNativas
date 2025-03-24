package com.example.apirest

import retrofit2.Call
import retrofit2.http.GET

data class Message(val message: String)

interface ApiService {
    @GET("/api/hello")
    fun getHelloWorld(): Call<Message>
}