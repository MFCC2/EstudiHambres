package com.example.estudihambres.core.util

/**
 * Encapsulador genérico de estados para respuestas de repositorios y flujos asíncronos.
 *
 * @param T Tipo de dato devuelto.
 * @property data Datos del resultado en caso de éxito.
 * @property message Mensaje de error en caso de fallo.
 */
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
