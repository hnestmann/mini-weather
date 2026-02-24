package com.miniweather.app

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = when {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
    }
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (!cont.isCancelled) {
                try {
                    locationManager.removeUpdates(this)
                } catch (_: SecurityException) {}
                cont.resume(location.latitude to location.longitude)
            }
        }
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
    }
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            locationManager.getCurrentLocation(
                provider,
                null,
                java.util.concurrent.Executors.newSingleThreadExecutor(),
                { location ->
                    if (!cont.isCancelled) {
                        if (location != null) {
                            cont.resume(location.latitude to location.longitude)
                        } else {
                            cont.resume(null)
                        }
                    }
                }
            )
        } else {
            @Suppress("DEPRECATION")
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    } catch (e: SecurityException) {
        cont.resume(null)
    } catch (e: Exception) {
        cont.resume(null)
    }
}
