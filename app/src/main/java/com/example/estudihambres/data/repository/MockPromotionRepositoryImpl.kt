package com.example.estudihambres.data.repository

import com.example.estudihambres.data.remote.api.DefaultPromotionApiServiceImpl
import com.example.estudihambres.data.remote.api.PromotionApiService
import com.example.estudihambres.domain.model.Promotion
import com.example.estudihambres.domain.repository.PromotionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Implementación del repositorio de promociones conectado al servicio de API de convenios universitarios [PromotionApiService].
 * Filtra y valida que cada promoción cuente con enlace oficial seguro HTTPS hacia la web del aliado o términos de la oferta.
 *
 * @param apiService Servicio de API para captar promociones validadas.
 */
class MockPromotionRepositoryImpl(
    private val apiService: PromotionApiService = DefaultPromotionApiServiceImpl()
) : PromotionRepository {

    override fun getPromotions(): Flow<List<Promotion>> = flow {
        val remoteDtos = try {
            apiService.getVerifiedPromotions()
        } catch (_: Exception) {
            emptyList()
        }

        val domainPromos = remoteDtos.map { dto ->
            val isSecured = apiService.isOfferUrlSecureAndVerified(dto.officialUrl)
            dto.toDomain().copy(
                isValidated = dto.isValidated && isSecured,
                verificationSource = if (isSecured) dto.verificationSource else "No validado"
            )
        }

        emit(domainPromos)
    }
}
