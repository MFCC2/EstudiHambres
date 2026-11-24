package com.example.estudihambres.core.util

import android.content.Context
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.io.Closeable
import java.util.concurrent.ExecutorService

/**
 * Utilidades para liberar los recursos de los visores CameraX + ML Kit.
 *
 * Los visores se vinculan al `LifecycleOwner` de la pantalla, por lo que al salir del
 * composable (cerrar un diálogo, cambiar de pestaña o pasar a modo manual) la cámara seguía
 * encendida analizando frames hasta que la pantalla completa se destruía. Estas funciones
 * desvinculan sólo los casos de uso propios del visor y cierran los detectores nativos.
 */
object CameraScannerResources {

    /**
     * Desvincula de CameraX únicamente los [useCases] del visor que sale de composición,
     * sin afectar a otros visores que pudieran estar activos.
     */
    fun unbind(context: Context, useCases: MutableList<UseCase>) {
        if (useCases.isEmpty()) return
        val toUnbind = useCases.toTypedArray()
        useCases.clear()
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching { future.get().unbind(*toUnbind) }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Detiene el hilo del analizador y cierra los detectores ML Kit (recursos nativos).
     */
    fun release(executor: ExecutorService?, vararg detectors: Closeable) {
        executor?.shutdown()
        detectors.forEach { runCatching { it.close() } }
    }
}
