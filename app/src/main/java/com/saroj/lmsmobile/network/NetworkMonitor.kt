package com.saroj.lmsmobile.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkMonitor {
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    @Volatile
    private var connectivityManager: ConnectivityManager? = null

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        val manager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager = manager
        _isOnline.value = manager.hasInternetConnection()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                update(manager)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                update(manager)
            }

            override fun onLost(network: Network) {
                update(manager)
            }

            override fun onUnavailable() {
                update(manager)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            manager.registerDefaultNetworkCallback(callback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            manager.registerNetworkCallback(request, callback)
        }
        initialized = true
    }

    fun isCurrentlyOnline(): Boolean =
        connectivityManager?.hasInternetConnection() ?: _isOnline.value

    private fun update(manager: ConnectivityManager) {
        _isOnline.value = manager.hasInternetConnection()
    }

    private fun ConnectivityManager.hasInternetConnection(): Boolean {
        val network = activeNetwork ?: return false
        val capabilities = getNetworkCapabilities(network) ?: return false
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            true
        }
        return hasInternet && isValidated
    }
}
