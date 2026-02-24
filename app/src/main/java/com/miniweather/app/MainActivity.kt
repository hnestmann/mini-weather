package com.miniweather.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var locationStore: LocationStore
    private lateinit var weatherViewModel: WeatherViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        locationStore = LocationStore(this)
        weatherViewModel = WeatherViewModel(WeatherRepository(), locationStore)

        lifecycleScope.launch {
            val locations = locationStore.getLocations().first()
            val idx = locationStore.getCurrentIndex().first()
            if (locations.isEmpty()) {
                weatherViewModel.showAddCity()
            } else {
                weatherViewModel.setLocations(locations, idx)
            }
        }

        setContent {
            MiniWeatherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    WeatherScreen(
                    viewModel = weatherViewModel,
                    onAddCity = { weatherViewModel.showAddCity() },
                    onOpenMenu = { weatherViewModel.showMenu() }
                )
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent?): Boolean {
        if (event != null && weatherViewModel.handleKeyEvent(event.keyCode, event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
