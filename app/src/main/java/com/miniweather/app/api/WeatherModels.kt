package com.miniweather.app.api

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val current: CurrentWeather,
    val hourly: HourlyWeather,
    val daily: DailyWeather
)

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature2m: Double,
    val is_day: Int,
    @SerializedName("relative_humidity_2m") val humidity: Int,
    @SerializedName("surface_pressure") val pressure: Double,
    @SerializedName("wind_speed_10m") val windSpeed: Double,
    @SerializedName("wind_direction_10m") val windDirection: Double,
    @SerializedName("weather_code") val weatherCode: Int
)

data class HourlyWeather(
    val time: List<String>,
    @SerializedName("temperature_2m") val temperature: List<Double>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("precipitation_probability") val precipitationProbability: List<Int?>,
    @SerializedName("is_day") val isDay: List<Int>
)

data class DailyWeather(
    val time: List<String>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("temperature_2m_max") val tempMax: List<Double>,
    @SerializedName("temperature_2m_min") val tempMin: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>,
    @SerializedName("uv_index_max") val uvIndexMax: List<Double>,
    @SerializedName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?>
)

/** Raw API response with hourly arrays */
data class AirQualityApiResponse(
    val hourly: AirQualityHourly
)

data class AirQualityHourly(
    val time: List<String>,
    @SerializedName("us_aqi") val usAqi: List<Int?>?,
    @SerializedName("alder_pollen") val alderPollen: List<Double?>?,
    @SerializedName("birch_pollen") val birchPollen: List<Double?>?,
    @SerializedName("grass_pollen") val grassPollen: List<Double?>?,
    @SerializedName("olive_pollen") val olivePollen: List<Double?>?,
    @SerializedName("ragweed_pollen") val ragweedPollen: List<Double?>?,
    val pm10: List<Double?>?,
    @SerializedName("pm2_5") val pm25: List<Double?>?,
    val ozone: List<Double?>?
)

/** Extracted from first hourly value for UI display */
data class AirQualityResponse(
    val current: AirQualityCurrent
)

data class AirQualityCurrent(
    val usAqi: Int?,
    val alderPollen: Double?,
    val birchPollen: Double?,
    val grassPollen: Double?,
    val olivePollen: Double?,
    val ragweedPollen: Double?,
    val pm10: Double?,
    val pm25: Double?,
    val ozone: Double?
)
