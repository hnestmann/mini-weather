package com.miniweather.app

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniweather.app.api.AirQualityResponse
import com.miniweather.app.api.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

data class WeatherUiState(
    val locations: List<SavedLocation> = emptyList(),
    val currentIndex: Int = 0,
    val weather: WeatherResponse? = null,
    val airQuality: AirQualityResponse? = null,
    val airQualityError: String? = null,  // For debugging when air quality fails
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTab: Int = 0,  // 0=Home, 1=Description, 2=Details, 3=Pollen
    val hourlyOffset: Int = 0,  // which set of 5 hours (0, 1, 2, ...)
    val dailyOffset: Int = 0,    // which set of 5 days (0, 1, 2, ...)
    val showAddCity: Boolean = false,
    val showMenu: Boolean = false,
    val searchQuery: String = ""
)

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val locationStore: LocationStore
) : ViewModel() {

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    fun setLocations(locations: List<SavedLocation>, currentIndex: Int) {
        _state.update { it.copy(locations = locations, currentIndex = currentIndex) }
        viewModelScope.launch {
            locationStore.saveLocations(locations, currentIndex)
        }
        fetchWeather()
    }

    fun nextTab() {
        _state.update {
            it.copy(selectedTab = (it.selectedTab + 1) % 4)
        }
    }

    fun setTab(index: Int) {
        _state.update {
            it.copy(selectedTab = index.coerceIn(0, 3))
        }
    }

    fun nextCity() {
        val locations = _state.value.locations
        if (locations.size < 2) return
        val newIndex = (_state.value.currentIndex + 1) % locations.size
        _state.update { it.copy(currentIndex = newIndex) }
        viewModelScope.launch {
            locationStore.saveCurrentIndex(newIndex)
        }
        fetchWeather()
    }

    fun nextHourlySet() {
        val hourlyCount = _state.value.weather?.hourly?.time?.size ?: 24
        val maxOffset = maxOf(0, (hourlyCount - 5) / 5)
        _state.update {
            it.copy(hourlyOffset = (it.hourlyOffset + 1).coerceIn(0, maxOffset))
        }
    }

    fun prevHourlySet() {
        _state.update {
            it.copy(hourlyOffset = (it.hourlyOffset - 1).coerceAtLeast(0))
        }
    }

    fun nextDailySet() {
        val dailyCount = _state.value.weather?.daily?.time?.size ?: 8
        val maxOffset = maxOf(0, (dailyCount - 1 - 5 + 4) / 5)
        _state.update {
            it.copy(dailyOffset = (it.dailyOffset + 1).coerceIn(0, maxOffset))
        }
    }

    fun prevDailySet() {
        _state.update {
            it.copy(dailyOffset = (it.dailyOffset - 1).coerceAtLeast(0))
        }
    }

    fun refresh() {
        fetchWeather()
    }

    fun showAddCity() {
        _state.update { it.copy(showAddCity = true, showMenu = false) }
    }

    fun hideAddCity() {
        _state.update { it.copy(showAddCity = false) }
    }

    fun showMenu() {
        _state.update { it.copy(showMenu = true, showAddCity = false) }
    }

    fun hideMenu() {
        _state.update { it.copy(showMenu = false) }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun searchCity() {
        val query = _state.value.searchQuery.trim()
        if (query.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.searchCity(query)
                .onSuccess { result ->
                    result?.let { loc ->
                        val name = "${loc.name}, ${loc.countryCode}"
                        val newLoc = SavedLocation(name, loc.latitude, loc.longitude)
                        val current = _state.value.locations.toMutableList()
                        val existing = current.indexOfFirst { it.name == name }
                        val newList = if (existing >= 0) {
                            current[existing] = newLoc
                            current
                        } else {
                            current + newLoc
                        }
                        val idx = if (existing >= 0) existing else newList.size - 1
                        setLocations(newList, idx)
                        hideAddCity()
                        _state.update { it.copy(searchQuery = "") }
                    } ?: _state.update { it.copy(error = "Not found", isLoading = false) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            error = e.message ?: "Error",
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun addCurrentLocation() {
        // Will be handled by Activity with permission + callback
    }

    fun addLocationFromGps(lat: Double, lon: Double) {
        val newLoc = SavedLocation("Your Location", lat, lon)
        val current = _state.value.locations.toMutableList()
        val existing = current.indexOfFirst { it.name == "Your Location" }
        val newList = if (existing >= 0) {
            current[existing] = newLoc
            current
        } else {
            listOf(newLoc) + current
        }
        setLocations(newList, 0)
        hideAddCity()
    }

    fun deleteLocation(index: Int) {
        val current = _state.value.locations.toMutableList()
        if (index !in current.indices) return
        current.removeAt(index)
        val newIdx = _state.value.currentIndex.let { idx ->
            when {
                current.isEmpty() -> 0
                idx >= current.size -> current.size - 1
                idx > index -> idx - 1
                else -> idx
            }
        }
        setLocations(current, newIdx)
        if (current.isEmpty()) showAddCity()
    }

    fun selectLocation(index: Int) {
        _state.update { it.copy(currentIndex = index) }
        viewModelScope.launch {
            locationStore.saveCurrentIndex(index)
        }
        fetchWeather()
        hideMenu()
    }

    private fun fetchWeather() {
        val loc = _state.value.locations.getOrNull(_state.value.currentIndex) ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            kotlinx.coroutines.coroutineScope {
                val weatherResult = repository.getWeather(loc.lat, loc.lon)
                val airResult = repository.getAirQuality(loc.lat, loc.lon)
                weatherResult
                    .onSuccess { w ->
                        airResult.onSuccess { a ->
                            _state.update {
                                it.copy(
                                    weather = w,
                                    airQuality = a,
                                    airQualityError = null,
                                    isLoading = false,
                                    error = null,
                                    hourlyOffset = 0,
                                    dailyOffset = 0
                                )
                            }
                        }
                        airResult.onFailure { e ->
                            android.util.Log.e("Weather", "Air quality API failed", e)
                            _state.update {
                                it.copy(
                                    weather = w,
                                    airQuality = null,
                                    airQualityError = "${e.javaClass.simpleName}: ${e.message}",
                                    isLoading = false,
                                    error = null,
                                    hourlyOffset = 0,
                                    dailyOffset = 0
                                )
                            }
                        }
                    }
                    .onFailure { e ->
                        _state.update { state ->
                            state.copy(
                                isLoading = false,
                                error = e.message ?: "Error"
                            )
                        }
                    }
            }
        }
    }

    fun handleKeyEvent(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (event?.action != KeyEvent.ACTION_DOWN) return false
        when {
            _state.value.showAddCity -> {
                when (keyCode) {
                    KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> {
                        hideAddCity()
                        return true
                    }
                    KeyEvent.KEYCODE_ENTER -> {
                        searchCity()
                        return true
                    }
                }
            }
            _state.value.showMenu -> {
                when (keyCode) {
                    KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> {
                        hideMenu()
                        return true
                    }
                }
            }
            else -> {
                when (keyCode) {
                    KeyEvent.KEYCODE_SPACE -> {
                        nextTab()
                        return true
                    }
                    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                        nextCity()
                        return true
                    }
                    KeyEvent.KEYCODE_R -> {
                        refresh()
                        return true
                    }
                    KeyEvent.KEYCODE_A -> {
                        showAddCity()
                        return true
                    }
                    KeyEvent.KEYCODE_L -> {
                        showMenu()
                        return true
                    }
                }
            }
        }
        return false
    }
}
