package com.miniweather.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.miniweather.app.api.AirQualityCurrent
import com.miniweather.app.api.DailyWeather
import com.miniweather.app.api.HourlyWeather
import com.miniweather.app.api.WeatherResponse

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    onAddCity: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    if (state.showAddCity) {
        AddCityOverlay(
            viewModel = viewModel,
            state = state,
            onDismiss = { viewModel.hideAddCity() },
            onSearch = { viewModel.searchCity() }
        )
        return
    }

    if (state.showMenu) {
        MenuOverlay(
            viewModel = viewModel,
            state = state,
            onDismiss = { viewModel.hideMenu() }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(vertical = 16.dp)
    ) {
        TopBar(
            onMenu = onOpenMenu,
            onRefresh = { viewModel.refresh() },
            onAdd = onAddCity
        )

        LocationDots(
            count = state.locations.size,
            selectedIndex = state.currentIndex
        )

        when {
            state.locations.isEmpty() -> EmptyState(onAddCity = onAddCity)
            state.isLoading && state.weather == null -> LoadingState()
            state.weather != null -> WeatherContent(
                state = state,
                viewModel = viewModel
            )
            else -> ErrorState(
                message = state.error ?: "Add a city to begin",
                onRetry = { viewModel.refresh() }
            )
        }
    }
}

@Composable
private fun TopBar(
    onMenu: () -> Unit,
    onRefresh: () -> Unit,
    onAdd: () -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 8.dp)
            .border(2.dp, contentColor)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu) {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Menu",
                tint = contentColor
            )
        }
        Text(
            "Mini Weather",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = contentColor,
            modifier = Modifier.padding(8.dp)
        )
        Row {
            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = contentColor
                )
            }
            IconButton(onClick = onAdd) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add city",
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
private fun LocationDots(count: Int, selectedIndex: Int) {
    if (count <= 1) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (i == selectedIndex) 2.dp else 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (i == selectedIndex) 1f else 0.3f
                        )
                    )
            )
        }
    }
}

@Composable
private fun EmptyState(onAddCity: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Add a city to begin.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddCity) {
                Text("ADD CITY")
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading...")
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text("RETRY")
            }
        }
    }
}

@Composable
private fun WeatherContent(state: WeatherUiState, viewModel: WeatherViewModel) {
    val weather = state.weather!!
    val loc = state.locations.getOrNull(state.currentIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        when (state.selectedTab) {
            0 -> {
                // Home tab: current weather, hourly, daily forecast
                CurrentWeatherSection(
                    name = loc?.name ?: "--",
                    weather = weather
                )
        HourlySection(
            hourly = weather.hourly,
            offset = state.hourlyOffset,
            onNext = { viewModel.nextHourlySet() },
            onPrev = { viewModel.prevHourlySet() }
        )
        FiveDayForecastHorizontal(
            daily = weather.daily,
            offset = state.dailyOffset,
            onNext = { viewModel.nextDailySet() },
            onPrev = { viewModel.prevDailySet() }
        )
            }
            else -> {
                // Other tabs: only tab content
                TabContent(
                    tab = state.selectedTab,
                    locationName = loc?.name ?: "--",
                    weather = weather,
                    airQuality = state.airQuality?.current,
                    airQualityError = state.airQualityError
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        BottomTabs(
            selectedTab = state.selectedTab,
            onTabSelected = { viewModel.setTab(it) }
        )
    }
}

@Composable
private fun CurrentWeatherSection(name: String, weather: WeatherResponse) {
    val cur = weather.current
    val daily = weather.daily
    val icon = weatherCodeToIcon(cur.weatherCode, cur.is_day == 1)
    val conditionText = weatherCodeToText[cur.weatherCode] ?: "Unknown"
    val dateStr = java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.getDefault())
        .format(java.util.Date())
    val precipChance = daily.precipitationProbabilityMax.getOrNull(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .border(2.dp, MaterialTheme.colorScheme.onSurface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                name.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                dateStr,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "${Math.round(cur.temperature2m)}°",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        conditionText.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (precipChance != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "$precipChance%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private val ForecastCardWidth = 75.dp
private val ForecastCardSpacing = 4.dp
private val ForecastArrowSize = 40.dp

@Composable
private fun HourlySection(
    hourly: HourlyWeather,
    offset: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val now = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val startIdx = (now + offset * 5).coerceIn(0, (hourly.time.size - 5).coerceAtLeast(0))
    val endIdx = (startIdx + 5).coerceAtMost(hourly.time.size)
    val hasNext = endIdx < hourly.time.size
    val hasPrev = offset > 0

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ForecastArrowButton(
                onClick = onPrev,
                enabled = hasPrev,
                isLeft = true
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(ForecastCardSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (startIdx until endIdx).forEach { i ->
                    if (i < hourly.time.size) {
                        HourlyItem(
                            time = hourly.time[i],
                            temp = hourly.temperature[i],
                            code = hourly.weatherCode[i],
                            isDay = hourly.isDay.getOrElse(i) { 1 }
                        )
                    }
                }
            }
            if (hasNext) {
                ForecastArrowButton(onClick = onNext, enabled = true, isLeft = false)
            } else {
                Spacer(modifier = Modifier.width(ForecastArrowSize))
            }
        }
    }
}

@Composable
private fun ForecastArrowButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLeft: Boolean
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(ForecastArrowSize)
    ) {
        Icon(
            if (isLeft) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun HourlyItem(
    time: String,
    temp: Double,
    code: Int,
    isDay: Int
) {
    val timeStr = try {
        val dt = java.time.Instant.parse(time + "Z")
        java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
            .format(dt)
    } catch (_: Exception) {
        time.takeLast(5)
    }
    Column(
        modifier = Modifier
            .width(ForecastCardWidth)
            .padding(4.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            timeStr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Icon(
            weatherCodeToIcon(code, isDay == 1),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Text(
            "${Math.round(temp)}°",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FiveDayForecastHorizontal(
    daily: DailyWeather,
    offset: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val startIdx = (1 + offset * 5).coerceIn(1, daily.time.size - 1)
    val endIdx = (startIdx + 5).coerceAtMost(daily.time.size)
    val hasNext = endIdx < daily.time.size
    val hasPrev = offset > 0

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ForecastArrowButton(
                onClick = onPrev,
                enabled = hasPrev,
                isLeft = true
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(ForecastCardSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (startIdx until endIdx).forEach { i ->
                    if (i < daily.time.size) {
                        val date = java.time.LocalDate.parse(daily.time[i])
                        val dayName = days[date.dayOfWeek.value % 7]
                        val dateStr = "${date.monthValue.toString().padStart(2, '0')}.${date.dayOfMonth.toString().padStart(2, '0')}"
                        DayForecastItem(
                            dayName = dayName,
                            dateStr = dateStr,
                            icon = weatherCodeToIcon(daily.weatherCode[i], true),
                            highTemp = daily.tempMax[i],
                            lowTemp = daily.tempMin[i],
                            width = ForecastCardWidth
                        )
                    }
                }
            }
            if (hasNext) {
                ForecastArrowButton(onClick = onNext, enabled = true, isLeft = false)
            } else {
                Spacer(modifier = Modifier.width(ForecastArrowSize))
            }
        }
    }
}

@Composable
private fun DayForecastItem(
    dayName: String,
    dateStr: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highTemp: Double,
    lowTemp: Double,
    width: androidx.compose.ui.unit.Dp = 64.dp
) {
    Column(
        modifier = Modifier
            .width(width)
            .padding(4.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            dayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            dateStr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal
        )
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Text(
            "${Math.round(highTemp)}°/${Math.round(lowTemp)}°",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TabContent(
    tab: Int,
    locationName: String,
    weather: WeatherResponse,
    airQuality: AirQualityCurrent?,
    airQualityError: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            locationName.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp)
        )
        when (tab) {
            1 -> DescriptionTab(weather, airQuality)
            2 -> DetailsTab(weather)
            3 -> PollenTab(airQuality, airQualityError)
            else -> { /* Home handled separately */ }
        }
    }
}

private fun generateAssistantSummary(
    temp: Double,
    code: Int,
    windKmh: Double,
    uv: Double,
    rainProb: Int?,
    isDay: Int,
    daily: DailyWeather,
    aqi: Int?
): String {
    val tempC = temp
    val isRainy = code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95)
    val isSnowy = code in listOf(71, 73, 75, 85, 86)
    val timeOfDay = if (isDay == 1) "day" else "night"
    val rainProbVal = rainProb ?: 0

    var text = ""
    when {
        tempC < 0 -> text += "It's freezing this $timeOfDay. Keep warm! "
        tempC < 10 -> text += "A rather cold $timeOfDay. "
        tempC < 18 -> text += "Cool and crisp conditions. "
        tempC < 25 -> text += "Pleasantly mild right now. "
        tempC < 30 -> text += "It is quite warm outside. "
        else -> text += "Hot conditions currently. "
    }

    when {
        tempC < 5 -> text += "Heavy winter gear (thermal layers, gloves) recommended. "
        tempC < 12 -> text += "Wear a warm coat. "
        tempC < 18 -> text += "A sweater or light jacket is advised. "
        tempC < 24 -> text += "Long sleeves or a t-shirt should suffice. "
        else -> text += "Shorts and light clothing are best. "
    }

    if (isRainy || rainProbVal > 40) text += "Rain is likely today. Don't forget an umbrella. "
    if (isSnowy) text += "Snow is expected. Water-resistant boots are a good idea. "
    if (windKmh > 20) text += "It's breezy outside. "
    if (isDay == 1 && uv > 6 && !isRainy) text += "UV index is high; wear sunscreen. "

    if (daily.tempMax.size > 1) {
        val todayMax = daily.tempMax[0]
        val tomorrowMax = daily.tempMax[1]
        val diff = tomorrowMax - todayMax
        when {
            diff > 2 -> text += "Temperatures will rise tomorrow. "
            diff < -2 -> text += "It will get noticeably colder tomorrow. "
            else -> text += "Tomorrow will have similar temperatures. "
        }

        var rainDays = 0
        for (i in 1..3) {
            if (i < daily.precipitationProbabilityMax.size) {
                val pop = daily.precipitationProbabilityMax.getOrNull(i)
                if (pop != null && pop > 40) rainDays++
            }
        }
        if (rainDays > 0) text += "Expect some precipitation in the coming days. "
        else text += "The next few days look mostly dry. "
    }

    val badWeather = isRainy || isSnowy || rainProbVal >= 50
    when {
        badWeather -> text += "Outdoor activities are not recommended due to precipitation. "
        tempC > 33 -> text += "Avoid strenuous outdoor exercise due to high heat. "
        windKmh > 35 -> text += "Strong winds make outdoor activities difficult. "
        else -> text += "Conditions are good for outdoor activities like walking or jogging. "
    }

    if (aqi != null) {
        val aqiDesc = when {
            aqi > 150 -> "Unhealthy"
            aqi > 100 -> "Unhealthy for Sensitive Groups"
            aqi > 50 -> "Moderate"
            else -> "Good"
        }
        text += "Air quality is $aqiDesc. "
    }

    return text.trim()
}

@Composable
private fun DescriptionTab(weather: WeatherResponse, airQuality: AirQualityCurrent?) {
    val cur = weather.current
    val daily = weather.daily
    val todayRainProb = daily.precipitationProbabilityMax.getOrNull(0)
    val uv = daily.uvIndexMax.getOrNull(0) ?: 0.0

    val summary = generateAssistantSummary(
        temp = cur.temperature2m,
        code = cur.weatherCode,
        windKmh = cur.windSpeed,
        uv = uv,
        rainProb = todayRainProb,
        isDay = cur.is_day,
        daily = daily,
        aqi = airQuality?.usAqi
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Text(
            "DESCRIPTION",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            summary,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
        )
    }
}

@Composable
private fun DetailsTab(weather: WeatherResponse) {
    val cur = weather.current
    val daily = weather.daily
    val sunrise = if (daily.sunrise.isNotEmpty()) daily.sunrise[0].takeLast(5) else "--"
    val sunset = if (daily.sunset.isNotEmpty()) daily.sunset[0].takeLast(5) else "--"
    val uv = if (daily.uvIndexMax.isNotEmpty()) daily.uvIndexMax[0].toInt().toString() else "--"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Text(
            "DETAILS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow("Wind", "${Math.round(cur.windSpeed)} km/h")
        DetailRow("Humidity", "${cur.humidity}%")
        DetailRow("UV Index", uv)
        DetailRow("Pressure", "${Math.round(cur.pressure)} hPa")
        DetailRow("Sunrise", sunrise)
        DetailRow("Sunset", sunset)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PollenTab(air: AirQualityCurrent?, airQualityError: String? = null) {
    if (air == null) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("No air quality data available.")
            if (!airQualityError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Error: $airQualityError",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        return
    }
    val treeVal = maxOf(
        air.alderPollen ?: 0.0,
        air.birchPollen ?: 0.0,
        air.olivePollen ?: 0.0
    )
    fun pollenLevel(v: Double) = when {
        v < 10 -> "Low"
        v < 30 -> "Mod"
        v < 80 -> "High"
        else -> "Extreme"
    }
    fun pm25Level(v: Double?) = when {
        v == null -> "--"
        v < 10 -> "Good"
        v < 25 -> "Fair"
        else -> "Poor"
    }
    fun ozoneLevel(v: Double?) = when {
        v == null -> "--"
        v < 60 -> "Good"
        v < 120 -> "Fair"
        else -> "Poor"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Text(
            "AIR QUALITY",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow("Tree", pollenLevel(treeVal))
        DetailRow("Grass", pollenLevel(air.grassPollen ?: 0.0))
        DetailRow("Ragweed", pollenLevel(air.ragweedPollen ?: 0.0))
        DetailRow("PM10", if (air.pm10 != null) pollenLevel(air.pm10) else "--")
        DetailRow("PM2.5", pm25Level(air.pm25))
        DetailRow("Ozone", ozoneLevel(air.ozone))
        DetailRow("AQI", air.usAqi?.toString() ?: "--")
    }
}

@Composable
private fun BottomTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Home", "Description", "Details", "Air Quality")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 16.dp)
            .border(2.dp, MaterialTheme.colorScheme.onSurface),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        tabs.forEachIndexed { i, label ->
            val selected = i == selectedTab
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.surface,
                onClick = { onTabSelected(i) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AddCityOverlay(
    viewModel: WeatherViewModel,
    state: WeatherUiState,
    onDismiss: () -> Unit,
    onSearch: () -> Unit
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            scope.launch {
                val loc = getCurrentLocation(context)
                if (loc != null) {
                    viewModel.addLocationFromGps(loc.first, loc.second)
                } else {
                    viewModel.setSearchQuery("")  // Clear to show error via state
                    viewModel.state  // Trigger - we need error state
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ADD LOCATION", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text("City name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onSearch
            ) {
                Text("SEARCH")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    } else {
                        scope.launch {
                            val loc = getCurrentLocation(context)
                            if (loc != null) {
                                viewModel.addLocationFromGps(loc.first, loc.second)
                            }
                        }
                    }
                }
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        state.error?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
fun MenuOverlay(
    viewModel: WeatherViewModel,
    state: WeatherUiState,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("LOCATIONS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        state.locations.forEachIndexed { i, loc ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(2.dp, MaterialTheme.colorScheme.onSurface)
                    .clickable { viewModel.selectLocation(i) }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    loc.name,
                    fontWeight = if (i == state.currentIndex) FontWeight.Black else FontWeight.Normal
                )
                IconButton(onClick = { viewModel.deleteLocation(i) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
