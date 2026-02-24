package com.miniweather.app.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    @GET("forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,is_day,relative_humidity_2m,surface_pressure,wind_speed_10m,wind_direction_10m,weather_code",
        @Query("hourly") hourly: String = "temperature_2m,weather_code,precipitation_probability,is_day",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 16,
        @Query("temperature_unit") tempUnit: String = "celsius",
        @Query("wind_speed_unit") windUnit: String = "kmh"
    ): WeatherResponse

    @GET("air-quality")
    suspend fun getAirQuality(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("hourly") hourly: String = "us_aqi,alder_pollen,birch_pollen,grass_pollen,olive_pollen,ragweed_pollen,pm10,pm2_5,ozone",
        @Query("forecast_days") forecastDays: Int = 1
    ): AirQualityApiResponse
}
