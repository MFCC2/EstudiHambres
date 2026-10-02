package com.example.estudihambres.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.example.estudihambres.domain.repository.LocationRepository
import com.example.estudihambres.domain.repository.UserCoordinates
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementación de [LocationRepository] utilizando Google Play Services Location ([FusedLocationProviderClient]).
 *
 * @param context Contexto de la aplicación para inicializar el cliente de ubicación.
 */
class PlayServicesLocationRepositoryImpl(
    private val context: Context,
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
) : LocationRepository {

    @SuppressLint("MissingPermission")
    override fun getUserLocation(): Flow<UserCoordinates?> = callbackFlow {
        try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                .addOnSuccessListener { location: Location? ->
                    val coords = location?.let {
                        UserCoordinates(latitude = it.latitude, longitude = it.longitude)
                    }
                    trySend(coords)
                }
                .addOnFailureListener {
                    trySend(null)
                }

            awaitClose {
                cancellationTokenSource.cancel()
            }
        } catch (_: SecurityException) {
            trySend(null)
            close()
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): UserCoordinates? {
        return try {
            val lastLoc = suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                    .addOnCanceledListener {
                        cont.cancel()
                    }
            }
            if (lastLoc != null) {
                return UserCoordinates(latitude = lastLoc.latitude, longitude = lastLoc.longitude)
            }

            // Si lastLocation es nulo (p. ej. GPS recién encendido), obtenemos la ubicación activa actual
            val tokenSource = CancellationTokenSource()
            val freshLoc = suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                    .addOnCanceledListener {
                        tokenSource.cancel()
                        cont.cancel()
                    }
            }
            freshLoc?.let {
                UserCoordinates(latitude = it.latitude, longitude = it.longitude)
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
