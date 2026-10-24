package com.example.estudihambres.data.remote.dto

import com.example.estudihambres.domain.model.RedemptionMethod
import com.example.estudihambres.domain.model.RewardItem

/**
 * Data Transfer Object (DTO) para la serialización y recepción remota del catálogo de premios.
 */
data class RewardItemDto(
    val id: String,
    val title: String,
    val category: String,
    val pointsCost: Int,
    val description: String,
    val partnerName: String,
    val iconEmoji: String,
    val sampleCode: String,
    val officialUrl: String,
    val terms: String,
    val redemptionMethod: String = "ONLINE_CODE",
    val isAvailable: Boolean = true
) {
    /**
     * Mapea el DTO remoto a la entidad inmutable de dominio [RewardItem].
     */
    fun toDomain(): RewardItem {
        val method = when (redemptionMethod.uppercase()) {
            "IN_STORE_QR" -> RedemptionMethod.IN_STORE_QR
            "DIRECT_LINK" -> RedemptionMethod.DIRECT_LINK
            else -> RedemptionMethod.ONLINE_CODE
        }

        return RewardItem(
            id = id,
            title = title,
            category = category,
            pointsCost = pointsCost,
            description = description,
            partnerName = partnerName,
            iconEmoji = iconEmoji,
            sampleCode = sampleCode,
            officialUrl = officialUrl,
            terms = terms,
            redemptionMethod = method,
            isAvailable = isAvailable
        )
    }
}
