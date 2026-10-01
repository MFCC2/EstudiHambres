package com.example.estudihambres.domain.repository

import com.example.estudihambres.domain.model.Commerce
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio para la consulta de comercios y restaurantes aliados.
 */
interface CommerceRepository {
    /**
     * Obtiene el listado completo o flujo reactivo de comercios afiliados.
     */
    fun getCommerces(): Flow<List<Commerce>>

    /**
     * Busca un comercio específico a partir de su identificador único.
     *
     * @param id Identificador único del comercio.
     */
    suspend fun getCommerceById(id: String): Commerce?
}
