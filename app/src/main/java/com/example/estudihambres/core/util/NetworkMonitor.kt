package com.example.estudihambres.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitor de conectividad a internet en tiempo real para EstudiHambres.
 *
 * Cumple con la directiva estricta de la aplicación:
 * "La app no debe funcionar por ningún motivo si no tiene conexión a internet".
 *
 * Supervisa las capacidades de red en tiempo real mediante [ConnectivityManager.NetworkCallback]
 * y expone un [StateFlow] observado a nivel de la raíz en [MainActivity].
 */
class NetworkMonitor(private val context: Context?) {

    private val connectivityManager: ConnectivityManager? = try {
        context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    } catch (_: Exception) {
        null
    }

    private val _isOnline = MutableStateFlow(isCurrentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        try {
            if (connectivityManager != null) {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

                connectivityManager.registerNetworkCallback(
                    request,
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            _isOnline.value = isCurrentlyOnline()
                        }

                        override fun onLost(network: Network) {
                            _isOnline.value = isCurrentlyOnline()
                        }

                        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                            val online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            _isOnline.value = online
                        }
                    }
                )
            }
        } catch (_: Exception) {
            // Manejo tolerante en entornos JVM o sin permisos en pruebas
        }
    }

    fun isCurrentlyOnline(): Boolean {
        // En pruebas unitarias JVM puras (sin Android Context), permitir ejecución de pruebas
        if (context == null || connectivityManager == null) {
            return true
        }
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    fun checkConnection(): Boolean {
        val online = isCurrentlyOnline()
        _isOnline.value = online
        return online
    }

    companion object {
        @Volatile
        private var instance: NetworkMonitor? = null

        fun getInstance(context: Context): NetworkMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkMonitor(context.applicationContext).also { instance = it }
            }
        }

        fun getInstanceOrNull(): NetworkMonitor? = instance

        /**
         * Verificación rápida de disponibilidad de red para repositorios y casos de uso.
         */
        fun isOnline(context: Context?): Boolean {
            if (context == null) return true
            return try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
                val active = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(active) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } catch (_: Exception) {
                false
            }
        }
    }
}
