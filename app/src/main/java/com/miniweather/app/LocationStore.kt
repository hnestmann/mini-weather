package com.miniweather.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_prefs")

data class SavedLocation(
    val name: String,
    val lat: Double,
    val lon: Double
)

class LocationStore(private val context: Context) {

    private val gson = Gson()
    private val listType = object : TypeToken<List<SavedLocation>>() {}.type
    private val locationsKey = stringPreferencesKey("locations")
    private val currentIndexKey = intPreferencesKey("current_index")

    fun getLocations(): Flow<List<SavedLocation>> = context.dataStore.data.map { prefs ->
        val json = prefs[locationsKey] ?: "[]"
        try {
            gson.fromJson(json, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getCurrentIndex(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[currentIndexKey] ?: 0
    }

    suspend fun saveLocations(locations: List<SavedLocation>, currentIndex: Int) {
        context.dataStore.edit { prefs ->
            prefs[locationsKey] = gson.toJson(locations)
            prefs[currentIndexKey] = currentIndex
        }
    }

    suspend fun saveCurrentIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[currentIndexKey] = index
        }
    }
}
