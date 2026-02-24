package com.miniweather.app

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

/**
 * Maps Open-Meteo weather codes to Material monochrome icons.
 * Uses filled icons for high contrast on e-ink.
 */
fun weatherCodeToIcon(code: Int, isDay: Boolean): ImageVector = when (code) {
    0 -> if (isDay) Icons.Default.LightMode else Icons.Default.DarkMode
    1, 2 -> if (isDay) Icons.Default.Cloud else Icons.Default.Cloud
    3 -> Icons.Default.Cloud
    45, 48 -> Icons.Default.Grain  // fog
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Default.WaterDrop  // rain
    71, 73, 75, 85, 86 -> Icons.Default.AcUnit  // snow
    95, 96, 99 -> Icons.Default.Thunderstorm
    else -> Icons.Default.Help
}
