package com.example.estudihambres.core.util

import android.content.Context
import android.content.SharedPreferences
import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus

/**
 * Gestor de persistencia local y autenticación para CampusPass (EstudiHambres).
 *
 * Administra dos almacenes locales desacoplados:
 * 1. [sessionPrefs]: Controla la sesión actualmente activa en el dispositivo.
 * 2. [accountPrefs]: Almacén persistente de cuentas de usuarios registrados con validación de contraseña.
 *
 * @param context Contexto de la aplicación.
 */
class SessionManager(context: Context) {

    private val sessionPrefs: SharedPreferences = context.getSharedPreferences(PREF_SESSION_NAME, Context.MODE_PRIVATE)
    private val accountPrefs: SharedPreferences = context.getSharedPreferences(PREF_ACCOUNTS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_SESSION_NAME = "campuspass_session_prefs"
        private const val PREF_ACCOUNTS_NAME = "campuspass_registered_accounts"

        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_FULL_NAME = "key_full_name"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_DNI = "key_dni"
        private const val KEY_UNIVERSITY = "key_university"
        private const val KEY_STUDENT_CODE = "key_student_code"
        private const val KEY_VERIFICATION_STATUS = "key_verification_status"
        private const val KEY_CAREER = "key_career"
        private const val KEY_IS_DEV = "key_is_dev"
        private const val KEY_SCANNED_CARD_DNI = "key_scanned_card_dni"
        private const val KEY_PROFILE_PHOTO_PATH = "key_profile_photo_path"

        private const val KEY_RADAR_RADIUS = "key_radar_radius"
        private const val KEY_NOTIFICATIONS_ENABLED = "key_notifications_enabled"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also {
                    instance = it
                    it.seedDefaultAccountsIfEmpty()
                }
            }
        }

        fun getInstanceOrNull(): SessionManager? = instance
    }

    init {
        seedDefaultAccountsIfEmpty()
    }

    /**
     * Si no existen cuentas registradas, siembra cuentas semilla para pruebas institucionales.
     */
    private fun seedDefaultAccountsIfEmpty() {
        val seeded = accountPrefs.getBoolean("has_seeded_defaults", false)
        if (!seeded) {
            // Cuenta estudiantil por defecto
            val defaultStudent = StudentUser(
                id = "usr-continental-01",
                fullName = "Manuel Fabrizio Callañaupa",
                email = "alumno@continental.edu.pe",
                dni = "72945602",
                university = "Universidad Continental",
                studentCode = "72945602",
                verificationStatus = VerificationStatus.PENDING_VERIFICATION,
                career = "Ingeniería de Sistemas",
                isDev = false
            )
            saveAccountInternal(defaultStudent, "campus123")

            // Cuenta estudiantil alternativa
            val pucpStudent = StudentUser(
                id = "usr-pucp-02",
                fullName = "Valeria Ramos Mendoza",
                email = "estudiante@pucp.edu.pe",
                dni = "71548920",
                university = "PUCP",
                studentCode = "20221450",
                verificationStatus = VerificationStatus.PENDING_VERIFICATION,
                career = "Ingeniería Industrial",
                isDev = false
            )
            saveAccountInternal(pucpStudent, "123456")

            accountPrefs.edit().putBoolean("has_seeded_defaults", true).apply()
        }
    }

    // --- MÉTODOS DE AUTENTICACIÓN Y REGISTRO ---

    /**
     * Registra una nueva cuenta estudiantil con su contraseña.
     * Retorna error si el correo ya se encuentra registrado.
     */
    fun registerAccount(user: StudentUser, password: String): Result<StudentUser> {
        val normalizedEmail = user.email.trim().lowercase()

        if (isEmailRegistered(normalizedEmail)) {
            return Result.failure(IllegalArgumentException("Ya existe una cuenta registrada con el correo $normalizedEmail"))
        }

        if (isDniRegistered(user.dni)) {
            return Result.failure(IllegalArgumentException("El DNI ${user.dni} ya se encuentra registrado en otra cuenta"))
        }

        saveAccountInternal(user.copy(email = normalizedEmail), password)
        saveUserSession(user.copy(email = normalizedEmail))
        return Result.success(user)
    }

    /**
     * Autentica credenciales comprobando existencia de la cuenta y correspondencia exacta de contraseña.
     */
    fun authenticate(emailInput: String, passwordInput: String): Result<StudentUser> {
        val normalizedEmail = emailInput.trim().lowercase()

        if (!isEmailRegistered(normalizedEmail)) {
            return Result.failure(
                IllegalArgumentException("No existe ninguna cuenta registrada con el correo '$normalizedEmail'. Por favor, regístrate primero.")
            )
        }

        val storedPassword = accountPrefs.getString("pwd_$normalizedEmail", null)
        if (storedPassword == null || storedPassword != passwordInput) {
            return Result.failure(
                IllegalArgumentException("Contraseña incorrecta. Por favor verifica tus credenciales e intenta nuevamente.")
            )
        }

        val user = getAccountByEmail(normalizedEmail)
            ?: return Result.failure(IllegalStateException("Error al recuperar los datos de la cuenta."))

        saveUserSession(user)
        return Result.success(user)
    }

    fun isEmailRegistered(email: String): Boolean {
        val normalized = email.trim().lowercase()
        return accountPrefs.contains("pwd_$normalized")
    }

    fun isDniRegistered(dni: String): Boolean {
        val cleanDni = dni.trim()
        if (cleanDni.isBlank()) return false
        return accountPrefs.contains("dni_to_email_$cleanDni")
    }

    private fun saveAccountInternal(user: StudentUser, password: String) {
        val normalizedEmail = user.email.trim().lowercase()
        accountPrefs.edit()
            .putString("pwd_$normalizedEmail", password)
            .putString("id_$normalizedEmail", user.id)
            .putString("name_$normalizedEmail", user.fullName)
            .putString("dni_$normalizedEmail", user.dni)
            .putString("uni_$normalizedEmail", user.university)
            .putString("code_$normalizedEmail", user.studentCode)
            .putString("career_$normalizedEmail", user.career)
            .putString("status_$normalizedEmail", user.verificationStatus.name)
            .putBoolean("isDev_$normalizedEmail", user.isDev)
            .putString("scanned_card_$normalizedEmail", user.scannedCardDni)
            .putString("photo_$normalizedEmail", user.profilePhotoPath)
            .putString("dni_to_email_${user.dni}", normalizedEmail)
            .apply()
    }

    private fun getAccountByEmail(email: String): StudentUser? {
        val normalizedEmail = email.trim().lowercase()
        if (!isEmailRegistered(normalizedEmail)) return null

        val id = accountPrefs.getString("id_$normalizedEmail", "usr-${System.currentTimeMillis()}") ?: "usr"
        val name = accountPrefs.getString("name_$normalizedEmail", "Estudiante Universitario") ?: "Estudiante"
        val dni = accountPrefs.getString("dni_$normalizedEmail", "") ?: ""
        val uni = accountPrefs.getString("uni_$normalizedEmail", "Universidad Continental") ?: "Universidad"
        val code = accountPrefs.getString("code_$normalizedEmail", "") ?: ""
        val career = accountPrefs.getString("career_$normalizedEmail", "Ingeniería de Sistemas") ?: "Carrera"
        val statusStr = accountPrefs.getString("status_$normalizedEmail", VerificationStatus.PENDING_VERIFICATION.name)
        val isDev = accountPrefs.getBoolean("isDev_$normalizedEmail", false)
        val scannedCard = accountPrefs.getString("scanned_card_$normalizedEmail", null)
        val photo = accountPrefs.getString("photo_$normalizedEmail", null)

        val status = try {
            VerificationStatus.valueOf(statusStr ?: VerificationStatus.PENDING_VERIFICATION.name)
        } catch (_: Exception) {
            VerificationStatus.PENDING_VERIFICATION
        }

        return StudentUser(
            id = id,
            fullName = name,
            email = normalizedEmail,
            dni = dni,
            university = uni,
            studentCode = code,
            verificationStatus = status,
            career = career,
            isDev = isDev,
            scannedCardDni = scannedCard,
            profilePhotoPath = photo
        )
    }

    // --- MÉTODOS DE SESIÓN ACTIVA ---

    /**
     * Guarda la sesión activa en [sessionPrefs].
     */
    fun saveUserSession(user: StudentUser) {
        sessionPrefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_FULL_NAME, user.fullName)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_DNI, user.dni)
            .putString(KEY_UNIVERSITY, user.university)
            .putString(KEY_STUDENT_CODE, user.studentCode)
            .putString(KEY_VERIFICATION_STATUS, user.verificationStatus.name)
            .putString(KEY_CAREER, user.career)
            .putBoolean(KEY_IS_DEV, user.isDev)
            .putString(KEY_SCANNED_CARD_DNI, user.scannedCardDni)
            .putString(KEY_PROFILE_PHOTO_PATH, user.profilePhotoPath)
            .apply()

        // También sincronizar en el almacén de cuentas si existe
        if (!user.isDev && isEmailRegistered(user.email)) {
            val normalized = user.email.trim().lowercase()
            accountPrefs.edit()
                .putString("name_$normalized", user.fullName)
                .putString("uni_$normalized", user.university)
                .putString("career_$normalized", user.career)
                .putString("status_$normalized", user.verificationStatus.name)
                .putString("scanned_card_$normalized", user.scannedCardDni)
                .putString("photo_$normalized", user.profilePhotoPath)
                .apply()
        }
    }

    /**
     * Actualiza la foto de perfil del usuario en sesión activa y su cuenta persistente.
     */
    fun updateProfilePhoto(path: String) {
        val current = getUserSession() ?: return
        val updated = current.copy(profilePhotoPath = path)
        saveUserSession(updated)
    }

    /**
     * Actualiza los datos editables del perfil universitario.
     * Por estricta seguridad y normativa SUNEDU, los campos [dni] y [university] son inmutables
     * y se preservan exactamente como fueron validados o registrados.
     */
    fun updateProfile(
        fullName: String,
        email: String,
        career: String,
        studentCode: String = ""
    ) {
        val current = getUserSession() ?: return
        val preservedDni = current.dni
        val preservedUni = current.university

        sessionPrefs.edit()
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_EMAIL, email)
            .putString(KEY_DNI, preservedDni)
            .putString(KEY_UNIVERSITY, preservedUni)
            .putString(KEY_STUDENT_CODE, studentCode)
            .putString(KEY_CAREER, career)
            .apply()

        if (!current.isDev && isEmailRegistered(email)) {
            val normalized = email.trim().lowercase()
            accountPrefs.edit()
                .putString("name_$normalized", fullName)
                .putString("uni_$normalized", preservedUni)
                .putString("career_$normalized", career)
                .putString("code_$normalized", studentCode)
                .apply()
        }
    }

    /**
     * Sobrecarga de compatibilidad para llamadas previas de updateProfile.
     * Preserva inmutables el DNI y la Universidad.
     */
    fun updateProfile(
        fullName: String,
        email: String,
        dni: String,
        university: String,
        studentCode: String,
        career: String
    ) {
        updateProfile(fullName, email, career, studentCode)
    }

    /**
     * Recupera el usuario en sesión activa, o null si no hay sesión iniciada.
     */
    fun getUserSession(): StudentUser? {
        if (!isUserLoggedIn()) return null

        val id = sessionPrefs.getString(KEY_USER_ID, "usr-default") ?: "usr-default"
        val fullName = sessionPrefs.getString(KEY_FULL_NAME, "Estudiante Universitario") ?: "Estudiante Universitario"
        val email = sessionPrefs.getString(KEY_EMAIL, "alumno@universidad.edu.pe") ?: "alumno@universidad.edu.pe"
        val dni = sessionPrefs.getString(KEY_DNI, "") ?: ""
        val university = sessionPrefs.getString(KEY_UNIVERSITY, "Universidad Continental") ?: "Universidad Continental"
        val studentCode = sessionPrefs.getString(KEY_STUDENT_CODE, "") ?: ""
        val career = sessionPrefs.getString(KEY_CAREER, "Ingeniería de Sistemas") ?: "Ingeniería de Sistemas"
        val isDev = sessionPrefs.getBoolean(KEY_IS_DEV, false)
        val scannedCard = sessionPrefs.getString(KEY_SCANNED_CARD_DNI, null)
        val photo = sessionPrefs.getString(KEY_PROFILE_PHOTO_PATH, null)
        val statusStr = sessionPrefs.getString(KEY_VERIFICATION_STATUS, VerificationStatus.PENDING_VERIFICATION.name)
        val status = try {
            VerificationStatus.valueOf(statusStr ?: VerificationStatus.PENDING_VERIFICATION.name)
        } catch (_: Exception) {
            VerificationStatus.PENDING_VERIFICATION
        }

        return StudentUser(
            id = id,
            fullName = fullName,
            email = email,
            dni = dni,
            university = university,
            studentCode = studentCode,
            verificationStatus = status,
            career = career,
            isDev = isDev,
            scannedCardDni = scannedCard,
            profilePhotoPath = photo
        )
    }

    fun isUserLoggedIn(): Boolean {
        return sessionPrefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun isDevMode(): Boolean {
        return sessionPrefs.getBoolean(KEY_IS_DEV, false)
    }

    /**
     * Vincula el carnet escaneado (por su DNI) al estudiante activo.
     */
    fun linkScannedCard(scannedDni: String?) {
        val user = getUserSession() ?: return
        val dniToLink = if (!scannedDni.isNullOrBlank()) scannedDni else user.dni
        val updated = user.copy(
            verificationStatus = VerificationStatus.VERIFIED,
            scannedCardDni = dniToLink
        )
        saveUserSession(updated)
    }

    /**
     * Valida si el usuario activo tiene permitido escanear un nuevo carnet.
     * Regla:
     * - Si es cuenta Dev (isDev = true): puede escanear cualquier carnet tantas veces como desee para pruebas.
     * - Si es un estudiante regular y ya está VERIFIED: sólo puede tener 1 carnet verificado en su cuenta.
     * - Si el carnet escaneado no coincide con su DNI registrado: se le advierte del conflicto de identidad.
     */
    fun validateCardScanningEligibility(scannedDni: String?): Pair<Boolean, String?> {
        val user = getUserSession() ?: return Pair(true, null)

        // Modo Dev: libertad total de prueba
        if (user.isDev) {
            return Pair(true, null)
        }

        // Estudiante regular ya verificado anteriormente
        if (user.verificationStatus == VerificationStatus.VERIFIED && user.scannedCardDni != null) {
            if (scannedDni != null && scannedDni != user.scannedCardDni) {
                return Pair(
                    false,
                    "Tu cuenta ya cuenta con un carnet validado oficialmente (DNI: ${user.scannedCardDni}). Como estudiante universitario, solo tienes permitido registrar 1 carnet en tu cuenta."
                )
            }
        }

        // Estudiante regular escaneando carnet de otra persona
        if (scannedDni != null && user.dni.isNotBlank() && scannedDni != user.dni) {
            return Pair(
                false,
                "El DNI del carnet escaneado ($scannedDni) no coincide con el DNI de tu cuenta registrada (${user.dni}). Por seguridad, solo puedes verificar tu propio carnet universitario."
            )
        }

        return Pair(true, null)
    }

    fun updateVerificationStatus(status: VerificationStatus) {
        sessionPrefs.edit()
            .putString(KEY_VERIFICATION_STATUS, status.name)
            .apply()

        val user = getUserSession()
        if (user != null && !user.isDev && isEmailRegistered(user.email)) {
            val normalized = user.email.trim().lowercase()
            accountPrefs.edit()
                .putString("status_$normalized", status.name)
                .apply()
        }
    }

    fun getRadarRadiusKm(): Float = sessionPrefs.getFloat(KEY_RADAR_RADIUS, 30.0f)
    fun setRadarRadiusKm(radius: Float) {
        sessionPrefs.edit().putFloat(KEY_RADAR_RADIUS, radius).apply()
    }

    fun getNotificationsEnabled(): Boolean = sessionPrefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    fun setNotificationsEnabled(enabled: Boolean) {
        sessionPrefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    /**
     * Cierra la sesión activa borrando únicamente [sessionPrefs], preservando las cuentas registradas en [accountPrefs].
     */
    fun clearSession() {
        sessionPrefs.edit().clear().apply()
    }
}
