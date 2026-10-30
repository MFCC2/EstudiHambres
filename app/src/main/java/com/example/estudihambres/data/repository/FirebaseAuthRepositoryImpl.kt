package com.example.estudihambres.data.repository

import com.example.estudihambres.core.util.SessionManager
import com.example.estudihambres.domain.model.PointTransactionType
import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus
import com.example.estudihambres.domain.repository.AuthRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

/**
 * Implementación de producción para [AuthRepository] con sincronización en tiempo real
 * en Firebase Authentication y Cloud Firestore (`users/{userId}`), manteniendo
 * soporte híbrido y resiliencia offline a través de [SessionManager].
 *
 * Características:
 * 1. Autenticación en la nube multi-dispositivo (permite iniciar sesión en cualquier teléfono).
 * 2. Registro seguro con creación de perfil en Cloud Firestore auditado por [firestore.rules].
 * 3. Acreditación del bono de bienvenida en el ledger inmutable de transacciones.
 * 4. Fallback y tolerancia a fallos offline en caso de baja conectividad.
 * 5. Compatibilidad total con entornos JVM sin Android para pruebas unitarias automatizadas.
 *
 * @param auth Instancia de Firebase Authentication (nula en pruebas unitarias JVM).
 * @param firestore Instancia de Cloud Firestore (nula en pruebas unitarias JVM).
 * @param sessionManager Gestor de persistencia local en SharedPreferences.
 */
class FirebaseAuthRepositoryImpl(
    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    },
    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (_: Exception) {
        null
    },
    sessionManager: SessionManager? = null
) : AuthRepository {

    private val session: SessionManager? = sessionManager ?: SessionManager.getInstanceOrNull()

    // Memoria volátil para pruebas unitarias sin Android Context
    private val inMemoryAccounts = mutableMapOf<String, Pair<StudentUser, String>>()

    init {
        // Semilla para pruebas locales
        val seedUser = StudentUser(
            id = "u001",
            fullName = "Manuel Fabrizio Callañaupa",
            email = "alumno@continental.edu.pe",
            dni = "72945602",
            university = "Universidad Continental",
            studentCode = "72945602",
            verificationStatus = VerificationStatus.PENDING_VERIFICATION,
            career = "Ingeniería de Sistemas",
            isDev = false
        )
        inMemoryAccounts["alumno@continental.edu.pe"] = Pair(seedUser, "campus123")
    }

    private val currentUserState = MutableStateFlow<StudentUser?>(
        session?.getUserSession() ?: StudentUser(
            id = "u001",
            fullName = "Manuel Fabrizio Callañaupa",
            email = "alumno@continental.edu.pe",
            dni = "72945602",
            university = "Universidad Continental",
            studentCode = "72945602",
            verificationStatus = VerificationStatus.PENDING_VERIFICATION,
            career = "Ingeniería de Sistemas",
            isDev = false
        )
    )

    init {
        // Si hay una sesión previa en Firebase Auth, sincronizar en segundo plano
        val currentFirebaseUser = try { auth?.currentUser } catch (_: Exception) { null }
        if (currentFirebaseUser != null && firestore != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val uid = currentFirebaseUser.uid
                    val doc = firestore.collection(COLLECTION_USERS).document(uid).get().await()
                    if (doc.exists() && doc.data != null) {
                        val cloudUser = documentToStudentUser(doc.data!!, uid)
                        currentUserState.value = cloudUser
                        session?.saveUserSession(cloudUser)
                    }
                } catch (_: Exception) {
                    // Ignorar errores de sync en arranque para no bloquear UI
                }
            }
        }
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val SUBCOLLECTION_POINT_TRANSACTIONS = "point_transactions"
    }

    override fun getCurrentUser(): Flow<StudentUser?> {
        val saved = session?.getUserSession()
        if (saved != null && currentUserState.value != saved) {
            currentUserState.value = saved
        }
        return currentUserState.asStateFlow()
    }

    override suspend fun authenticate(email: String, password: String): Result<StudentUser> {
        val normEmail = email.trim().lowercase()

        // 1. Autenticación primaria en Firebase Cloud
        if (auth != null && firestore != null) {
            try {
                val authResult = auth.signInWithEmailAndPassword(normEmail, password).await()
                val firebaseUser = authResult.user
                    ?: return Result.failure(IllegalStateException("No se pudo obtener el usuario autenticado de Firebase."))
                val uid = firebaseUser.uid

                // Cargar documento de perfil desde Firestore
                val doc = firestore.collection(COLLECTION_USERS).document(uid).get().await()
                val studentUser: StudentUser

                if (doc.exists() && doc.data != null) {
                    studentUser = documentToStudentUser(doc.data!!, uid)
                } else {
                    // Inicialización de perfil en Firestore si fue creado por consola o primer login
                    val initialUser = StudentUser(
                        id = uid,
                        fullName = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "Estudiante",
                        email = normEmail,
                        dni = "",
                        university = "Universidad",
                        studentCode = "",
                        verificationStatus = VerificationStatus.UNVERIFIED,
                        career = "Ingeniería de Sistemas",
                        isDev = false,
                        studiPuntos = 200
                    )
                    val docMap = studentUserToFirestoreMap(initialUser, uid, isCreate = true)
                    firestore.collection(COLLECTION_USERS).document(uid).set(docMap).await()
                    studentUser = initialUser
                }

                // Sincronizar con el almacenamiento local del teléfono
                session?.saveAccountInternal(studentUser, password)
                session?.saveUserSession(studentUser)
                currentUserState.value = studentUser
                return Result.success(studentUser)

            } catch (e: FirebaseAuthInvalidUserException) {
                return Result.failure(
                    IllegalArgumentException("No existe ninguna cuenta registrada con el correo '$normEmail'. Por favor, regístrate primero.")
                )
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                return Result.failure(
                    IllegalArgumentException("Contraseña incorrecta o credenciales inválidas. Por favor verifica tus datos.")
                )
            } catch (e: FirebaseAuthException) {
                val msg = when (e.errorCode) {
                    "ERROR_OPERATION_NOT_ALLOWED", "OPERATION_NOT_ALLOWED" ->
                        "El proveedor Correo/Contraseña está deshabilitado en Firebase. Habilítalo en Firebase Console -> Authentication -> Sign-in method."
                    "ERROR_TOO_MANY_REQUESTS" ->
                        "Demasiados intentos fallidos. Por seguridad, la cuenta fue temporalmente bloqueada. Intenta más tarde."
                    else -> e.localizedMessage ?: "Error de autenticación: ${e.errorCode}"
                }
                return Result.failure(IllegalArgumentException(msg))
            } catch (e: Exception) {
                // Tolerancia offline si ya se había logueado previamente en este celular
                if (session != null) {
                    val localResult = session.authenticate(normEmail, password)
                    if (localResult.isSuccess) {
                        currentUserState.value = localResult.getOrNull()
                        return localResult
                    }
                }
                return Result.failure(
                    IllegalStateException("No se pudo conectar con el servidor de autenticación: ${e.localizedMessage ?: "Verifica tu conexión a internet."}")
                )
            }
        }

        // 2. Fallback local / JVM Testing
        if (session != null) {
            val result = session.authenticate(normEmail, password)
            if (result.isSuccess) {
                currentUserState.value = result.getOrNull()
            }
            return result
        }

        val account = inMemoryAccounts[normEmail]
            ?: return Result.failure(
                IllegalArgumentException("No existe ninguna cuenta registrada con el correo '$normEmail'. Por favor, regístrate primero.")
            )

        if (account.second != password) {
            return Result.failure(
                IllegalArgumentException("Contraseña incorrecta. Por favor verifica tus credenciales.")
            )
        }

        currentUserState.value = account.first
        return Result.success(account.first)
    }

    override suspend fun register(user: StudentUser, password: String): Result<StudentUser> {
        val normEmail = user.email.trim().lowercase()

        // 1. Registro primario en Firebase Auth + Firestore
        if (auth != null && firestore != null) {
            try {
                val authResult = auth.createUserWithEmailAndPassword(normEmail, password).await()
                val firebaseUser = authResult.user
                    ?: return Result.failure(IllegalStateException("No se pudo obtener el usuario registrado de Firebase."))
                val uid = firebaseUser.uid

                val cleanPoints = if (user.studiPuntos <= 0) 200 else user.studiPuntos
                val cloudUser = user.copy(
                    id = uid,
                    email = normEmail,
                    studiPuntos = cleanPoints
                )

                // Guardar documento en Firestore con el esquema estricto auditado
                val docMap = studentUserToFirestoreMap(cloudUser, uid, isCreate = true)
                firestore.collection(COLLECTION_USERS).document(uid).set(docMap).await()

                // Registrar transacción inicial de bienvenida en ledger inmutable de Firestore
                try {
                    val txId = "tx-welcome-$uid"
                    val welcomeTx = mapOf(
                        "id" to txId,
                        "title" to "Bono de Bienvenida Institucional",
                        "description" to "Bienvenido a EstudiHambres. +200 StudiPuntos acreditados para canje.",
                        "delta" to 200,
                        "pointsDelta" to 200,
                        "type" to "WELCOME_BONUS",
                        "txHash" to generateSha256("welcome-$uid-${System.currentTimeMillis()}"),
                        "timestampMillis" to System.currentTimeMillis().toDouble(),
                        "createdAt" to Timestamp.now()
                    )
                    firestore.collection(COLLECTION_USERS).document(uid)
                        .collection(SUBCOLLECTION_POINT_TRANSACTIONS)
                        .document(txId)
                        .set(welcomeTx)
                        .await()
                } catch (_: Exception) {
                    // No bloquear el registro si la transacción del ledger tarda
                }

                // Guardar en la sesión local del teléfono
                session?.saveAccountInternal(cloudUser, password)
                session?.saveUserSession(cloudUser)
                session?.recordPointTransaction(
                    title = "Bono de Bienvenida Institucional",
                    description = "Bienvenido a EstudiHambres. +200 StudiPuntos acreditados para canje.",
                    delta = 200,
                    type = PointTransactionType.WELCOME_BONUS
                )

                currentUserState.value = cloudUser
                return Result.success(cloudUser)

            } catch (e: FirebaseAuthUserCollisionException) {
                return Result.failure(
                    IllegalArgumentException("Ya existe una cuenta registrada con el correo $normEmail. Por favor inicia sesión.")
                )
            } catch (e: FirebaseAuthException) {
                val msg = when (e.errorCode) {
                    "ERROR_OPERATION_NOT_ALLOWED", "OPERATION_NOT_ALLOWED" ->
                        "El registro con correo/contraseña está deshabilitado en Firebase. Habilítalo en Firebase Console -> Authentication -> Sign-in method."
                    "ERROR_WEAK_PASSWORD" ->
                        "La contraseña es demasiado débil. Usa al menos 6 caracteres."
                    "ERROR_INVALID_EMAIL" ->
                        "El formato del correo institucional es inválido."
                    else -> e.localizedMessage ?: "Error de registro en Firebase: ${e.errorCode}"
                }
                return Result.failure(IllegalArgumentException(msg))
            } catch (e: Exception) {
                return Result.failure(
                    IllegalStateException("No se pudo completar el registro en la nube: ${e.localizedMessage ?: "Error de conexión."}")
                )
            }
        }

        // 2. Fallback local / JVM Testing
        if (session != null) {
            val result = session.registerAccount(user.copy(email = normEmail), password)
            if (result.isSuccess) {
                currentUserState.value = result.getOrNull()
            }
            return result
        }

        if (inMemoryAccounts.containsKey(normEmail)) {
            return Result.failure(IllegalArgumentException("Ya existe una cuenta registrada con el correo $normEmail"))
        }

        val newUser = user.copy(email = normEmail)
        inMemoryAccounts[normEmail] = Pair(newUser, password)
        currentUserState.value = newUser
        return Result.success(newUser)
    }

    override suspend fun updateVerificationStatus(status: VerificationStatus): Result<Unit> {
        val current = currentUserState.value ?: return Result.failure(IllegalStateException("No hay un usuario en sesión"))
        val updated = current.copy(verificationStatus = status)

        if (auth != null && firestore != null) {
            val uid = auth.currentUser?.uid ?: current.id
            try {
                firestore.collection(COLLECTION_USERS).document(uid).update(
                    mapOf(
                        "verificationStatus" to status.name,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            } catch (_: Exception) {
                // Tolerar fallos de red silenciosos en actualización secundaria
            }
        }

        session?.updateVerificationStatus(status)
        currentUserState.value = updated
        return Result.success(Unit)
    }

    override suspend fun updateProfile(fullName: String, career: String, studentCode: String): Result<Unit> {
        val current = currentUserState.value ?: return Result.failure(IllegalStateException("No hay un usuario en sesión"))
        val updated = current.copy(
            fullName = fullName.trim().take(100),
            career = career.trim().take(100),
            studentCode = studentCode.trim().take(50)
        )

        if (auth != null && firestore != null) {
            val uid = auth.currentUser?.uid ?: current.id
            try {
                firestore.collection(COLLECTION_USERS).document(uid).update(
                    mapOf(
                        "fullName" to updated.fullName,
                        "career" to updated.career,
                        "studentCode" to updated.studentCode,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            } catch (_: Exception) {
                // Tolerar fallo de red
            }
        }

        session?.updateProfile(
            fullName = updated.fullName,
            email = updated.email,
            career = updated.career,
            studentCode = updated.studentCode
        )
        currentUserState.value = updated
        return Result.success(Unit)
    }

    override suspend fun saveSession(user: StudentUser): Result<Unit> {
        currentUserState.value = user
        session?.saveUserSession(user)
        return Result.success(Unit)
    }

    override suspend fun clearSession(): Result<Unit> {
        try {
            auth?.signOut()
        } catch (_: Exception) {
            // Ignorar
        }
        currentUserState.value = null
        session?.clearSession()
        return Result.success(Unit)
    }

    // --- Mapeadores Auxiliares de Esquema Firestore ---

    private fun studentUserToFirestoreMap(
        user: StudentUser,
        uid: String,
        isCreate: Boolean
    ): Map<String, Any?> {
        val cleanDni = user.dni.filter { it.isDigit() }.take(8)
        val cleanScannedDni = user.scannedCardDni?.filter { it.isDigit() }?.take(8)

        val map = mutableMapOf<String, Any?>(
            "id" to uid,
            "fullName" to user.fullName.trim().take(100),
            "email" to user.email.trim().lowercase().take(100),
            "dni" to cleanDni,
            "university" to user.university.trim().take(100),
            "studentCode" to user.studentCode.trim().take(50),
            "verificationStatus" to user.verificationStatus.name,
            "career" to user.career.trim().take(100),
            "isDev" to false,
            "studiPuntos" to user.studiPuntos.coerceIn(0, 1000000),
            "updatedAt" to Timestamp.now()
        )

        cleanScannedDni?.let { map["scannedCardDni"] = it }
        user.profilePhotoPath?.take(500)?.let { map["profilePhotoPath"] = it }
        user.faculty?.take(100)?.let { map["faculty"] = it }
        user.validityYear?.take(10)?.let { map["validityYear"] = it }
        user.studentStatus?.take(100)?.let { map["studentStatus"] = it }

        if (isCreate) {
            map["createdAt"] = Timestamp.now()
        }

        return map
    }

    private fun documentToStudentUser(data: Map<String, Any>, uid: String): StudentUser {
        val statusStr = data["verificationStatus"] as? String ?: "UNVERIFIED"
        val status = try {
            VerificationStatus.valueOf(statusStr)
        } catch (_: Exception) {
            VerificationStatus.UNVERIFIED
        }

        return StudentUser(
            id = data["id"] as? String ?: uid,
            fullName = data["fullName"] as? String ?: "Estudiante",
            email = data["email"] as? String ?: "",
            dni = data["dni"] as? String ?: "",
            university = data["university"] as? String ?: "Universidad",
            studentCode = data["studentCode"] as? String ?: "",
            verificationStatus = status,
            career = data["career"] as? String ?: "Ingeniería de Sistemas",
            isDev = data["isDev"] as? Boolean ?: false,
            scannedCardDni = data["scannedCardDni"] as? String,
            profilePhotoPath = data["profilePhotoPath"] as? String,
            faculty = data["faculty"] as? String,
            validityYear = data["validityYear"] as? String,
            studentStatus = data["studentStatus"] as? String,
            studiPuntos = (data["studiPuntos"] as? Number)?.toInt() ?: 200
        )
    }

    private fun generateSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
