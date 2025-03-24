package com.example.rickandmorty

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
data class Character(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val origin: Location,
    val location: Location,
    val image: String,
    val episode: List<String>, // URLs de los episodios
    val url: String,
    val created: String
)
data class Location(
    val name: String,
    val url: String
)
data class CharactersResponse(
    val info: Info,
    val results: List<Character>
)
data class Info(
    val count: Int,
    val pages: Int,
    val next: String?,
    val prev: String?
)
interface RickAndMortyApiService {
    @GET("character")
    fun getCharacters(@Query("page") page: Int = 1): Call<CharactersResponse>

    @GET("character/{id}")
    fun getCharacterById(@Path("id") id: Int): Call<Character>

    @GET("character")
    fun searchCharacters(@Query("name") name: String): Call<CharactersResponse>

}