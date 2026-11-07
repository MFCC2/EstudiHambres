package com.example.estudihambres.data.repository

import com.example.estudihambres.core.util.Resource
import com.example.estudihambres.core.util.SessionManager
import com.example.estudihambres.data.remote.api.DefaultRewardsApiServiceImpl
import com.example.estudihambres.data.remote.api.RewardsApiService
import com.example.estudihambres.domain.model.PointTransaction
import com.example.estudihambres.domain.model.PointTransactionType
import com.example.estudihambres.domain.model.ReceiptValidationResult
import com.example.estudihambres.domain.model.RedeemedReward
import com.example.estudihambres.domain.model.RewardItem
import com.example.estudihambres.domain.model.SunatReceiptData
import com.example.estudihambres.domain.repository.ReceiptCollisionRepository
import com.example.estudihambres.domain.repository.RewardsRepository
import com.example.estudihambres.domain.usecase.ValidateSunatReceiptUseCase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Implementación robusta del repositorio de recompensas de StudiPuntos.
 * Integra el catálogo oficial [RewardsApiService], el validador fiscal SUNAT [ValidateSunatReceiptUseCase],
 * el registro atómico anti-colisiones en la nube [ReceiptCollisionRepository] (Cloud Firestore)
 * y el libro mayor inmutable respaldado en [SessionManager] con fallback en memoria para pruebas unitarias.
 *
 * @param sessionManager Gestor de persistencia y registro multi-usuario de huellas.
 * @param apiService Servicio de consulta de catálogo oficial de convenios.
 * @param sunatValidator Validador de boletas electrónicas peruanas.
 * @param collisionRepository Repositorio de verificación y registro atómico en la nube.
 * @param auth Instancia de Firebase Authentication para sincronización multi-dispositivo en la nube.
 * @param firestore Instancia de Cloud Firestore para persistencia distribuida de vales y transacciones.
 */
class MockRewardsRepositoryImpl(
    private val sessionManager: SessionManager? = SessionManager.getInstanceOrNull(),
    private val apiService: RewardsApiService = DefaultRewardsApiServiceImpl(),
    private val sunatValidator: ValidateSunatReceiptUseCase = ValidateSunatReceiptUseCase(),
    private val collisionRepository: ReceiptCollisionRepository = FirebaseReceiptCollisionRepositoryImpl(sessionManager = sessionManager),
    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    },
    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (_: Exception) {
        null
    }
) : RewardsRepository {

    private val inMemoryRedeemed = mutableListOf<RedeemedReward>()
    private val inMemoryTransactions = mutableListOf<PointTransaction>()
    private val inMemoryClaimedReceipts = mutableSetOf<String>()

    private val pointsFlow = MutableStateFlow(sessionManager?.getStudiPuntos() ?: 2450)
    private val transactionsFlow = MutableStateFlow(sessionManager?.getPointTransactions() ?: emptyList())
    private val redeemedFlow = MutableStateFlow(sessionManager?.getRedeemedRewards() ?: emptyList())

    override fun getAvailableRewards(): Flow<List<RewardItem>> = flow {
        val dtos = try {
            apiService.getVerifiedRewardsCatalog()
        } catch (_: Exception) {
            emptyList()
        }
        val domainRewards = dtos.map { it.toDomain() }
        emit(domainRewards)
    }

    override fun getRedeemedRewards(): Flow<List<RedeemedReward>> {
        val current = sessionManager?.getRedeemedRewards() ?: inMemoryRedeemed.toList()
        redeemedFlow.value = current
        return redeemedFlow.asStateFlow()
    }

    override fun getPointTransactions(): Flow<List<PointTransaction>> {
        val current = sessionManager?.getPointTransactions() ?: inMemoryTransactions.toList()
        transactionsFlow.value = current
        return transactionsFlow.asStateFlow()
    }

    override fun getUserStudiPuntos(): Flow<Int> {
        val current = sessionManager?.getStudiPuntos() ?: pointsFlow.value
        pointsFlow.value = current
        return pointsFlow.asStateFlow()
    }

    override suspend fun redeemReward(reward: RewardItem): Resource<RedeemedReward> {
        val currentPoints = sessionManager?.getStudiPuntos() ?: pointsFlow.value
        if (currentPoints < reward.pointsCost) {
            return Resource.Error("Saldo insuficiente de StudiPuntos para este premio. Te faltan ${reward.pointsCost - currentPoints} pts.")
        }

        val codePrefix = reward.partnerName.take(4).uppercase().replace(" ", "X")
        val uniqueCode = "$codePrefix-${UUID.randomUUID().toString().take(6).uppercase()}"
        val now = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.forLanguageTag("es-PE")).format(Date())

        val redeemed = RedeemedReward(
            id = UUID.randomUUID().toString(),
            rewardTitle = reward.title,
            pointsCost = reward.pointsCost,
            redemptionCode = uniqueCode,
            redeemedAtFormatted = now,
            partnerName = reward.partnerName,
            iconEmoji = reward.iconEmoji,
            isUsed = false,
            expiresAtMillis = System.currentTimeMillis() + (48L * 3600 * 1000),
            officialUrl = reward.officialUrl,
            redemptionMethod = reward.redemptionMethod
        )

        if (sessionManager != null) {
            sessionManager.saveRedeemedReward(redeemed)
            sessionManager.recordPointTransaction(
                title = "Canje: ${reward.title}",
                description = "Vale redimido para ${reward.partnerName} (${reward.redemptionMethod.name}).",
                delta = -reward.pointsCost,
                type = PointTransactionType.REWARD_PURCHASE,
                referenceId = redeemed.id
            )
            val updated = sessionManager.getStudiPuntos()
            pointsFlow.value = updated
            transactionsFlow.value = sessionManager.getPointTransactions()
            redeemedFlow.value = sessionManager.getRedeemedRewards()

            // Sincronización en la nube con Cloud Firestore
            val currentUid = auth?.currentUser?.uid ?: sessionManager.getUserSession()?.id
            if (firestore != null && currentUid != null && auth?.currentUser != null) {
                try {
                    val rewardMap = mapOf(
                        "id" to redeemed.id,
                        "rewardTitle" to redeemed.rewardTitle.take(100),
                        "pointsCost" to redeemed.pointsCost,
                        "redemptionCode" to redeemed.redemptionCode.take(50),
                        "redeemedAtFormatted" to redeemed.redeemedAtFormatted.take(50),
                        "partnerName" to redeemed.partnerName.take(100),
                        "iconEmoji" to redeemed.iconEmoji.take(20),
                        "isUsed" to false,
                        "expiresAtMillis" to redeemed.expiresAtMillis.toDouble(),
                        "securityToken" to redeemed.securityToken.take(20),
                        "officialUrl" to redeemed.officialUrl.take(300),
                        "redemptionMethod" to redeemed.redemptionMethod.name,
                        "createdAt" to Timestamp.now()
                    )
                    firestore.collection("users").document(currentUid)
                        .collection("redeemed_rewards")
                        .document(redeemed.id)
                        .set(rewardMap)
                        .await()

                    val txId = "tx-redeem-${redeemed.id.take(8)}"
                    val txPrevHash = sessionManager.getPointTransactions().firstOrNull()?.txHash ?: "GENESIS_BLOCK"
                    val txHash = PointTransaction.calculateHash(
                        txPrevHash,
                        txId,
                        -reward.pointsCost,
                        PointTransactionType.REWARD_PURCHASE.name,
                        System.currentTimeMillis()
                    )
                    val txMap = mapOf(
                        "id" to txId,
                        "title" to "Canje: ${reward.title}".take(120),
                        "description" to "Vale redimido para ${reward.partnerName} (${reward.redemptionMethod.name}).".take(300),
                        "delta" to -reward.pointsCost,
                        "pointsDelta" to -reward.pointsCost,
                        "type" to PointTransactionType.REWARD_PURCHASE.name,
                        "txHash" to txHash,
                        "previousHash" to txPrevHash,
                        "referenceId" to redeemed.id,
                        "timestampMillis" to System.currentTimeMillis().toDouble(),
                        "createdAt" to Timestamp.now()
                    )
                    firestore.collection("users").document(currentUid)
                        .collection("point_transactions")
                        .document(txId)
                        .set(txMap)
                        .await()

                    firestore.collection("users").document(currentUid)
                        .update(
                            mapOf(
                                "studiPuntos" to updated,
                                "updatedAt" to Timestamp.now()
                            )
                        )
                        .await()
                } catch (_: Exception) {
                    // Fallback silencioso si falla red temporalmente
                }
            }
        } else {
            val updated = pointsFlow.value - reward.pointsCost
            pointsFlow.value = updated
            val tx = PointTransaction(
                id = UUID.randomUUID().toString(),
                title = "Canje: ${reward.title}",
                description = "Vale redimido para ${reward.partnerName}.",
                pointsDelta = -reward.pointsCost,
                type = PointTransactionType.REWARD_PURCHASE,
                timestampMillis = System.currentTimeMillis(),
                referenceId = redeemed.id
            )
            inMemoryTransactions.add(0, tx)
            transactionsFlow.value = inMemoryTransactions.toList()
            inMemoryRedeemed.add(0, redeemed)
            redeemedFlow.value = inMemoryRedeemed.toList()
        }

        return Resource.Success(redeemed)
    }

    override suspend fun registerReceipt(
        ruc: String,
        serie: String,
        correlativo: String,
        amount: Double,
        dateMillis: Long
    ): Resource<ReceiptValidationResult> {
        val user = sessionManager?.getUserSession()
        val userId = user?.id ?: "usr_student_${System.currentTimeMillis()}"
        val userEmail = user?.email ?: "estudiante@campus.pe"

        val fingerprint = SunatReceiptData.calculateFingerprint(ruc, serie, correlativo)
        val isAlreadyClaimed = collisionRepository.isReceiptClaimed(fingerprint) ||
                (sessionManager?.isReceiptClaimedGlobally(fingerprint) ?: inMemoryClaimedReceipts.contains(fingerprint))

        val result = sunatValidator(
            ruc = ruc,
            serie = serie,
            correlativo = correlativo,
            montoTotal = amount,
            fechaEmisionMillis = dateMillis,
            isFingerprintAlreadyClaimed = { isAlreadyClaimed }
        )

        if (!result.isValid) {
            return Resource.Error(result.rejectionReason ?: "El comprobante no superó la auditoría de SUNAT.")
        }

        val receiptData = result.receiptData
            ?: return Resource.Error("No se pudieron extraer los datos fiscales.")

        // Registrar reclamo atómico global (Cloud Firestore + Local)
        val claimResult = collisionRepository.claimReceipt(
            fingerprint = receiptData.fingerprint,
            userId = userId,
            userEmail = userEmail,
            partnerName = receiptData.partnerName,
            amount = receiptData.montoTotal
        )

        if (claimResult.isFailure) {
            return Resource.Error(claimResult.exceptionOrNull()?.message ?: "Error al registrar el comprobante.")
        }

        if (sessionManager != null) {
            sessionManager.claimReceiptGlobally(
                fingerprint = receiptData.fingerprint,
                claimedByEmail = userEmail,
                partnerName = receiptData.partnerName
            )
            val tx = sessionManager.recordPointTransaction(
                title = "Boleta en ${receiptData.partnerName}",
                description = "Comprobante ${receiptData.serie}-${receiptData.correlativo} verificado. +80 StudiPuntos acreditados.",
                delta = 80,
                type = PointTransactionType.RECEIPT_SCAN,
                referenceId = receiptData.fingerprint
            )
            val updatedPoints = sessionManager.getStudiPuntos()
            pointsFlow.value = updatedPoints
            transactionsFlow.value = sessionManager.getPointTransactions()

            // Sincronización en Cloud Firestore
            val currentUid = auth?.currentUser?.uid ?: user?.id
            if (firestore != null && currentUid != null && auth?.currentUser != null) {
                try {
                    val txMap = mapOf(
                        "id" to tx.id,
                        "title" to tx.title.take(120),
                        "description" to tx.description.take(300),
                        "delta" to 80,
                        "pointsDelta" to 80,
                        "type" to PointTransactionType.RECEIPT_SCAN.name,
                        "txHash" to tx.txHash,
                        "previousHash" to tx.previousHash,
                        "referenceId" to receiptData.fingerprint.take(100),
                        "timestampMillis" to tx.timestampMillis.toDouble(),
                        "createdAt" to Timestamp.now()
                    )
                    firestore.collection("users").document(currentUid)
                        .collection("point_transactions")
                        .document(tx.id)
                        .set(txMap)
                        .await()

                    firestore.collection("users").document(currentUid)
                        .update(
                            mapOf(
                                "studiPuntos" to updatedPoints,
                                "updatedAt" to Timestamp.now()
                            )
                        )
                        .await()
                } catch (_: Exception) {}
            }
        } else {
            inMemoryClaimedReceipts.add(receiptData.fingerprint)
            val updated = pointsFlow.value + 80
            pointsFlow.value = updated
        }

        return Resource.Success(result)
    }

    override suspend fun registerReceiptQr(qrRawText: String): Resource<ReceiptValidationResult> {
        val user = sessionManager?.getUserSession()
        val userId = user?.id ?: "usr_student_${System.currentTimeMillis()}"
        val userEmail = user?.email ?: "estudiante@campus.pe"

        val result = sunatValidator.parseSunatQr(
            qrRawText = qrRawText,
            isFingerprintAlreadyClaimed = { fingerprint ->
                sessionManager?.isReceiptClaimedGlobally(fingerprint) ?: inMemoryClaimedReceipts.contains(fingerprint)
            }
        )

        if (!result.isValid) {
            return Resource.Error(result.rejectionReason ?: "El código QR no corresponde a una Boleta Electrónica válida de SUNAT.")
        }

        val receiptData = result.receiptData
            ?: return Resource.Error("Error al procesar la firma digital del QR SUNAT.")

        // Doble verificación en Cloud Firestore
        if (collisionRepository.isReceiptClaimed(receiptData.fingerprint)) {
            return Resource.Error("El comprobante fiscal ya fue registrado previamente en la red.")
        }

        val claimResult = collisionRepository.claimReceipt(
            fingerprint = receiptData.fingerprint,
            userId = userId,
            userEmail = userEmail,
            partnerName = receiptData.partnerName,
            amount = receiptData.montoTotal
        )

        if (claimResult.isFailure) {
            return Resource.Error(claimResult.exceptionOrNull()?.message ?: "Error al registrar el comprobante.")
        }

        if (sessionManager != null) {
            sessionManager.claimReceiptGlobally(
                fingerprint = receiptData.fingerprint,
                claimedByEmail = userEmail,
                partnerName = receiptData.partnerName
            )
            val tx = sessionManager.recordPointTransaction(
                title = "Boleta QR en ${receiptData.partnerName}",
                description = "Firma digital del comprobante ${receiptData.serie}-${receiptData.correlativo} verificada.",
                delta = 80,
                type = PointTransactionType.RECEIPT_SCAN,
                referenceId = receiptData.fingerprint
            )
            val updatedPoints = sessionManager.getStudiPuntos()
            pointsFlow.value = updatedPoints
            transactionsFlow.value = sessionManager.getPointTransactions()

            // Sincronización en Cloud Firestore
            val currentUid = auth?.currentUser?.uid ?: user?.id
            if (firestore != null && currentUid != null && auth?.currentUser != null) {
                try {
                    val txMap = mapOf(
                        "id" to tx.id,
                        "title" to tx.title.take(120),
                        "description" to tx.description.take(300),
                        "delta" to 80,
                        "pointsDelta" to 80,
                        "type" to PointTransactionType.RECEIPT_SCAN.name,
                        "txHash" to tx.txHash,
                        "previousHash" to tx.previousHash,
                        "referenceId" to receiptData.fingerprint.take(100),
                        "timestampMillis" to tx.timestampMillis.toDouble(),
                        "createdAt" to Timestamp.now()
                    )
                    firestore.collection("users").document(currentUid)
                        .collection("point_transactions")
                        .document(tx.id)
                        .set(txMap)
                        .await()

                    firestore.collection("users").document(currentUid)
                        .update(
                            mapOf(
                                "studiPuntos" to updatedPoints,
                                "updatedAt" to Timestamp.now()
                            )
                        )
                        .await()
                } catch (_: Exception) {}
            }
        } else {
            inMemoryClaimedReceipts.add(receiptData.fingerprint)
            val updated = pointsFlow.value + 80
            pointsFlow.value = updated
        }

        return Resource.Success(result)
    }

    override suspend fun markRewardAsUsed(rewardId: String): Boolean {
        val currentUid = auth?.currentUser?.uid ?: sessionManager?.getUserSession()?.id
        if (firestore != null && currentUid != null && auth?.currentUser != null) {
            try {
                firestore.collection("users").document(currentUid)
                    .collection("redeemed_rewards")
                    .document(rewardId)
                    .update(
                        mapOf(
                            "isUsed" to true,
                            "usedAt" to Timestamp.now()
                        )
                    )
                    .await()
            } catch (_: Exception) {}
        }

        if (sessionManager != null) {
            val success = sessionManager.markRewardAsUsed(rewardId)
            if (success) {
                redeemedFlow.value = sessionManager.getRedeemedRewards()
            }
            return success
        } else {
            val idx = inMemoryRedeemed.indexOfFirst { it.id == rewardId || it.redemptionCode == rewardId }
            if (idx >= 0) {
                inMemoryRedeemed[idx] = inMemoryRedeemed[idx].copy(isUsed = true)
                redeemedFlow.value = inMemoryRedeemed.toList()
                return true
            }
            return false
        }
    }

    override suspend fun earnPointsFromReceipt(): Resource<Int> {
        val bonus = 80
        if (sessionManager != null) {
            val tx = sessionManager.recordPointTransaction(
                title = "Registro de Boleta Comercial",
                description = "Comprobante de consumo verificado en comercio asociado del campus.",
                delta = bonus,
                type = PointTransactionType.RECEIPT_SCAN
            )
            val updated = sessionManager.getStudiPuntos()
            pointsFlow.value = updated
            transactionsFlow.value = sessionManager.getPointTransactions()

            val currentUid = auth?.currentUser?.uid ?: sessionManager.getUserSession()?.id
            if (firestore != null && currentUid != null && auth?.currentUser != null) {
                try {
                    val txMap = mapOf(
                        "id" to tx.id,
                        "title" to tx.title.take(120),
                        "description" to tx.description.take(300),
                        "delta" to bonus,
                        "pointsDelta" to bonus,
                        "type" to PointTransactionType.RECEIPT_SCAN.name,
                        "txHash" to tx.txHash,
                        "previousHash" to tx.previousHash,
                        "timestampMillis" to tx.timestampMillis.toDouble(),
                        "createdAt" to Timestamp.now()
                    )
                    firestore.collection("users").document(currentUid)
                        .collection("point_transactions")
                        .document(tx.id)
                        .set(txMap)
                        .await()

                    firestore.collection("users").document(currentUid)
                        .update(
                            mapOf(
                                "studiPuntos" to updated,
                                "updatedAt" to Timestamp.now()
                            )
                        )
                        .await()
                } catch (_: Exception) {}
            }

            return Resource.Success(updated)
        } else {
            val updated = pointsFlow.value + bonus
            pointsFlow.value = updated
            return Resource.Success(updated)
        }
    }
}
