package com.example.locationmaps.api

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApiService {
    @GET("api/v1/places")
    suspend fun getNearbyPlaces(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radius: Int = 1000, // 1 km
        @Query("categories") categories: String = "restaurant,museum,landmark"
    ): Response<PlacesResponse>

    companion object {
        private const val BASE_URL = "https://opentripmap.io/" // Ejemplo, puedes usar cualquier API de POIs

        fun create(): PlacesApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlacesApiService::class.java)
        }
    }
}

data class PlacesResponse(
    val features: List<PlaceFeature>
)

data class PlaceFeature(
    val properties: PlaceProperties,
    val geometry: PlaceGeometry
)

data class PlaceProperties(
    val name: String,
    val rate: Int?,
    val kinds: String // comma-separated categories
)

data class PlaceGeometry(
    val coordinates: List<Double> // [longitude, latitude]
)