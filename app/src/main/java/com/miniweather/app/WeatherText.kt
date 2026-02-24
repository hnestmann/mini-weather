package com.miniweather.app

/**
 * Open-Meteo weather code to human-readable text.
 */
val weatherCodeToText = mapOf(
    0 to "Clear",
    1 to "Mostly Clear",
    2 to "Partly Cloudy",
    3 to "Overcast",
    45 to "Fog",
    48 to "Rime Fog",
    51 to "Light Drizzle",
    53 to "Drizzle",
    55 to "Heavy Drizzle",
    61 to "Light Rain",
    63 to "Rain",
    65 to "Heavy Rain",
    71 to "Light Snow",
    73 to "Snow",
    75 to "Heavy Snow",
    80 to "Showers",
    81 to "Showers",
    82 to "Heavy Showers",
    85 to "Snow Showers",
    86 to "Heavy Snow Showers",
    95 to "Thunderstorm",
    96 to "Thunderstorm",
    99 to "Thunderstorm"
)
