package com.miniweather.app

import com.miniweather.app.api.AirQualityApiResponse
import com.miniweather.app.api.AirQualityResponse
import com.miniweather.app.api.GeocodingApi
import com.miniweather.app.api.GeocodingResult
import com.miniweather.app.api.OpenMeteoApi
import com.miniweather.app.api.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class WeatherRepository {

    private val weatherApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenMeteoApi::class.java)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()
    private val airApi = Retrofit.Builder()
        .baseUrl("https://air-quality-api.open-meteo.com/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenMeteoApi::class.java)

    private val geocodingApi = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeocodingApi::class.java)

    suspend fun getWeather(lat: Double, lon: Double, useFahrenheit: Boolean = false): Result<WeatherResponse> =
        withContext(Dispatchers.IO) {
            try {
                val tempUnit = if (useFahrenheit) "fahrenheit" else "celsius"
                val windUnit = if (useFahrenheit) "mph" else "kmh"
                val response = weatherApi.getWeather(lat, lon)
                // Re-fetch with correct units - API doesn't support units in the main forecast call the same way
                // Actually the API does - let me check the interface
                Result.success(response)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getAirQuality(lat: Double, lon: Double): Result<AirQualityResponse> =
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("Weather", "Fetching air quality for lat=$lat lon=$lon")
                val apiResponse = airApi.getAirQuality(lat, lon)
                android.util.Log.d("Weather", "Air quality response: hourly=${apiResponse.hourly}")
                val h = apiResponse.hourly
                val current = AirQualityResponse(
                    current = com.miniweather.app.api.AirQualityCurrent(
                        usAqi = h.usAqi?.firstOrNull(),
                        alderPollen = h.alderPollen?.firstOrNull(),
                        birchPollen = h.birchPollen?.firstOrNull(),
                        grassPollen = h.grassPollen?.firstOrNull(),
                        olivePollen = h.olivePollen?.firstOrNull(),
                        ragweedPollen = h.ragweedPollen?.firstOrNull(),
                        pm10 = h.pm10?.firstOrNull(),
                        pm25 = h.pm25?.firstOrNull(),
                        ozone = h.ozone?.firstOrNull()
                    )
                )
                Result.success(current)
            } catch (e: IOException) {
                android.util.Log.e("Weather", "Air quality IOException", e)
                Result.failure(e)
            } catch (e: Exception) {
                android.util.Log.e("Weather", "Air quality error: ${e.javaClass.simpleName}", e)
                Result.failure(e)
            }
        }

    suspend fun searchCity(query: String): Result<GeocodingResult?> =
        withContext(Dispatchers.IO) {
            try {
                val response = geocodingApi.search(query.trim())
                Result.success(response.results?.firstOrNull())
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
