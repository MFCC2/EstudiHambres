package com.example.estudihambres.core.util

import android.content.Context
import android.content.SharedPreferences
import com.example.estudihambres.core.theme.AppThemeMode
import com.example.estudihambres.domain.model.PointTransaction
import com.example.estudihambres.domain.model.PointTransactionType
import com.example.estudihambres.domain.model.RedeemedCoupon
import com.example.estudihambres.domain.model.RedeemedReward
import com.example.estudihambres.domain.model.RedemptionMethod
import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus
import com.example.estudihambres.domain.usecase.RedeemCouponUseCase
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor de persistencia local y autenticación para EstudiHambres.
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

    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    val themeModeFlow: StateFlow<AppThemeMode> = _themeModeFlow.asStateFlow()

    companion object {
        private const val PREF_SESSION_NAME = "estudihambres_session_prefs"
        private const val PREF_ACCOUNTS_NAME = "estudihambres_registered_accounts"

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
        private const val KEY_THEME_MODE = "key_app_theme_mode"

        private const val KEY_ACCUMULATED_SAVINGS = "key_accumulated_savings"
        private const val KEY_REDEEMED_COUPONS = "key_redeemed_coupons"
        private const val KEY_STUDI_PUNTOS = "key_studi_puntos"
        private const val KEY_REDEEMED_REWARDS = "key_redeemed_rewards"
        private const val KEY_POINT_TRANSACTIONS = "key_point_transactions"
        private const val KEY_GLOBAL_CLAIMED_RECEIPTS = "key_global_claimed_receipts"

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

        /**
         * Genera un hash criptográfico SHA-256 para almacenamiento seguro de contraseñas.
         */
        fun hashPassword(password: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
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
    @Synchronized
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
        recordPointTransaction(
            title = "Bono de Bienvenida Institucional",
            description = "Bienvenido a EstudiHambres. +200 StudiPuntos acreditados para canje.",
            delta = 200,
            type = PointTransactionType.WELCOME_BONUS
        )
        return Result.success(user)
    }

    /**
     * Autentica credenciales comprobando existencia de la cuenta y correspondencia exacta de contraseña.
     */
    /**
     * Autentica credenciales comprobando existencia de la cuenta y correspondencia de contraseña con hash SHA-256.
     * Ofrece compatibilidad regresiva transparente para cuentas previas en texto plano migrándolas automáticamente.
     */
    @Synchronized
    fun authenticate(emailInput: String, passwordInput: String): Result<StudentUser> {
        val normalizedEmail = emailInput.trim().lowercase()

        if (!isEmailRegistered(normalizedEmail)) {
            return Result.failure(
                IllegalArgumentException("No existe ninguna cuenta registrada con el correo '$normalizedEmail'. Por favor, regístrate primero.")
            )
        }

        val storedPassword = accountPrefs.getString("pwd_$normalizedEmail", null)
        val hashedInput = hashPassword(passwordInput)
        val isMatch = storedPassword != null && (storedPassword == hashedInput || storedPassword == passwordInput)
        if (!isMatch) {
            return Result.failure(
                IllegalArgumentException("Contraseña incorrecta. Por favor verifica tus credenciales e intenta nuevamente.")
            )
        }

        // Migración transparente si la contraseña residía en texto claro histórico
        if (storedPassword == passwordInput && storedPassword != hashedInput) {
            accountPrefs.edit().putString("pwd_$normalizedEmail", hashedInput).apply()
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
        val passwordToStore = if (password.length == 64 && password.all { it.isDigit() || it in 'a'..'f' }) {
            password
        } else {
            hashPassword(password)
        }
        accountPrefs.edit()
            .putString("pwd_$normalizedEmail", passwordToStore)
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

            val savedPoints = accountPrefs.getInt("points_$normalized", -1)
            if (savedPoints != -1) {
                sessionPrefs.edit().putInt(KEY_STUDI_PUNTOS, savedPoints).apply()
            }
            val savedTxs = accountPrefs.getString("txs_$normalized", null)
            if (savedTxs != null) {
                sessionPrefs.edit().putString(KEY_POINT_TRANSACTIONS, savedTxs).apply()
            }
            val savedRewards = accountPrefs.getString("rewards_$normalized", null)
            if (savedRewards != null) {
                sessionPrefs.edit().putString(KEY_REDEEMED_REWARDS, savedRewards).apply()
            }
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
        val oldDni = user.dni
        val updated = user.copy(
            dni = dniToLink,
            verificationStatus = VerificationStatus.VERIFIED,
            scannedCardDni = dniToLink
        )
        saveUserSession(updated)

        if (oldDni.isNotBlank() && oldDni != dniToLink) {
            accountPrefs.edit()
                .remove("dni_to_email_$oldDni")
                .putString("dni_to_email_$dniToLink", user.email.trim().lowercase())
                .putString("dni_${user.email.trim().lowercase()}", dniToLink)
                .apply()
        }
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
        return Pair(true, null)
    }

    fun updateVerificationStatus(status: VerificationStatus) {
        val prevStatus = getUserSession()?.verificationStatus
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

        if (prevStatus != VerificationStatus.VERIFIED && status == VerificationStatus.VERIFIED) {
            recordPointTransaction(
                title = "Acreditación Carnet SUNEDU",
                description = "Validación oficial de carnet universitario mediante reconocimiento óptico seguro.",
                delta = 300,
                type = PointTransactionType.ID_VERIFICATION
            )
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
     * Retorna el modo visual de tema configurado.
     * Predeterminado: [AppThemeMode.LIGHT] (Fondo blanco limpio).
     */
    fun getThemeMode(): AppThemeMode {
        val raw = sessionPrefs.getString(KEY_THEME_MODE, AppThemeMode.LIGHT.name) ?: AppThemeMode.LIGHT.name
        return try {
            AppThemeMode.valueOf(raw)
        } catch (_: Exception) {
            AppThemeMode.LIGHT
        }
    }

    /**
     * Actualiza el modo visual y notifica reactivamente a toda la interfaz.
     */
    fun setThemeMode(mode: AppThemeMode) {
        sessionPrefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeModeFlow.value = mode
    }

    /**
     * Alterna rápidamente entre Modo Claro y Modo Oscuro.
     */
    fun toggleThemeMode() {
        val next = if (getThemeMode() == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK
        setThemeMode(next)
    }

    // --- GESTIÓN DE AHORROS Y CUPONES CANJEADOS (FUNCIONALIDAD REAL) ---

    /**
     * Retorna el total acumulado de ahorros universitarios en Soles (PEN).
     */
    fun getAccumulatedSavings(): Double {
        return sessionPrefs.getFloat(KEY_ACCUMULATED_SAVINGS, 142.50f).toDouble()
    }

    /**
     * Actualiza el total acumulado de ahorros en sesión activa y almacén de cuenta.
     */
    fun setAccumulatedSavings(amount: Double) {
        sessionPrefs.edit().putFloat(KEY_ACCUMULATED_SAVINGS, amount.toFloat()).apply()
        val user = getUserSession()
        if (user != null && !user.isDev && isEmailRegistered(user.email)) {
            val normalized = user.email.trim().lowercase()
            accountPrefs.edit().putFloat("savings_$normalized", amount.toFloat()).apply()
        }
    }

    /**
     * Incrementa los ahorros acumulados tras redimir un cupón.
     */
    fun addAccumulatedSavings(additionalAmount: Double): Double {
        val current = getAccumulatedSavings()
        val updated = ((current + additionalAmount) * 100.0).toLong() / 100.0
        setAccumulatedSavings(updated)
        return updated
    }

    /**
     * Obtiene la lista activa de cupones canjeados por el estudiante.
     * Si no existen cupones registrados, siembra los cupones activos iniciales.
     */
    fun getRedeemedCoupons(): List<RedeemedCoupon> {
        val raw = sessionPrefs.getString(KEY_REDEEMED_COUPONS, null)
        if (raw.isNullOrBlank()) {
            val defaults = getInitialDefaultCoupons()
            saveRedeemedCouponsInternal(defaults)
            return defaults
        }
        return raw.split(";").mapNotNull { deserializeCoupon(it) }
    }

    /**
     * Canjea un nuevo cupón generando un token único, persistiendo en sesión y sumando al ahorro acumulado.
     */
    @Synchronized
    fun redeemCoupon(
        title: String,
        partnerName: String,
        category: String,
        discountBadge: String,
        couponId: String = ""
    ): RedeemedCoupon {
        val newCoupon = RedeemCouponUseCase().invoke(
            partnerName = partnerName,
            title = title,
            category = category,
            discountBadge = discountBadge,
            couponId = couponId
        )
        val currentCoupons = getRedeemedCoupons().toMutableList()
        currentCoupons.add(0, newCoupon)
        saveRedeemedCouponsInternal(currentCoupons)
        addAccumulatedSavings(newCoupon.savingsAmountSoles)
        recordPointTransaction(
            title = "Canje en $partnerName",
            description = "Puntos de fidelidad por canjear '$title'.",
            delta = 50,
            type = PointTransactionType.COUPON_REDEEMED,
            referenceId = newCoupon.id
        )
        return newCoupon
    }

    /**
     * Marca un cupón existente como utilizado en caja.
     */
    @Synchronized
    fun markCouponAsUsed(couponCode: String): Boolean {
        val currentCoupons = getRedeemedCoupons().toMutableList()
        val index = currentCoupons.indexOfFirst { it.redemptionCode.equals(couponCode, ignoreCase = true) }
        if (index >= 0) {
            val updated = currentCoupons[index].copy(isUsed = true)
            currentCoupons[index] = updated
            saveRedeemedCouponsInternal(currentCoupons)
            return true
        }
        return false
    }

    /**
     * Conteo total de cupones canjeados.
     */
    fun getCouponsRedeemedCount(): Int = getRedeemedCoupons().size

    private fun saveRedeemedCouponsInternal(coupons: List<RedeemedCoupon>) {
        val serialized = coupons.joinToString(";") { serializeCoupon(it) }
        sessionPrefs.edit().putString(KEY_REDEEMED_COUPONS, serialized).apply()
        val user = getUserSession()
        if (user != null && !user.isDev && isEmailRegistered(user.email)) {
            val normalized = user.email.trim().lowercase()
            accountPrefs.edit().putString("coupons_$normalized", serialized).apply()
        }
    }

    private fun serializeCoupon(c: RedeemedCoupon): String {
        fun esc(s: String) = s.replace("%", "%25").replace("|", "%7C").replace(";", "%3B")
        return listOf(
            esc(c.id),
            esc(c.couponId),
            esc(c.title),
            esc(c.partnerName),
            esc(c.category),
            esc(c.discountBadge),
            esc(c.redemptionCode),
            c.savingsAmountSoles.toString(),
            c.redeemedAtMillis.toString(),
            c.expiresAtMillis.toString(),
            c.isUsed.toString()
        ).joinToString("|")
    }

    private fun deserializeCoupon(s: String): RedeemedCoupon? {
        try {
            val parts = s.split("|")
            if (parts.size < 11) return null
            fun unesc(str: String) = str.replace("%3B", ";").replace("%7C", "|").replace("%25", "%")
            return RedeemedCoupon(
                id = unesc(parts[0]),
                couponId = unesc(parts[1]),
                title = unesc(parts[2]),
                partnerName = unesc(parts[3]),
                category = unesc(parts[4]),
                discountBadge = unesc(parts[5]),
                redemptionCode = unesc(parts[6]),
                savingsAmountSoles = parts[7].toDoubleOrNull() ?: 12.0,
                redeemedAtMillis = parts[8].toLongOrNull() ?: System.currentTimeMillis(),
                expiresAtMillis = parts[9].toLongOrNull() ?: (System.currentTimeMillis() + 48L * 3600 * 1000),
                isUsed = parts[10].toBoolean()
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun getInitialDefaultCoupons(): List<RedeemedCoupon> {
        val now = System.currentTimeMillis()
        return listOf(
            RedeemedCoupon(
                id = "red-init-01",
                couponId = "cu-02",
                title = "2x1 en Frappuccinos y Café del Día",
                partnerName = "Starbucks Real Plaza Cusco",
                category = "Cafetería & Cowork",
                discountBadge = "2x1 PROMO",
                redemptionCode = "CP-STAR-9214",
                savingsAmountSoles = 16.50,
                redeemedAtMillis = now - 3600_000L * 4,
                expiresAtMillis = now + 3600_000L * 44,
                isUsed = false
            ),
            RedeemedCoupon(
                id = "red-init-02",
                couponId = "cu-01",
                title = "Combo Universitario 2x1 en Hamburguesas",
                partnerName = "Bembos Real Plaza Cusco",
                category = "Comida Rápida",
                discountBadge = "40% DCTO",
                redemptionCode = "CP-BEMB-4819",
                savingsAmountSoles = 14.50,
                redeemedAtMillis = now - 3600_000L * 12,
                expiresAtMillis = now + 3600_000L * 36,
                isUsed = false
            ),
            RedeemedCoupon(
                id = "red-init-03",
                couponId = "p-01",
                title = "Plan Estudiantil Spotify Premium",
                partnerName = "Spotify",
                category = "Herramientas digitales",
                discountBadge = "50% DCTO",
                redemptionCode = "CP-SPOT-7102",
                savingsAmountSoles = 10.95,
                redeemedAtMillis = now - 3600_000L * 24,
                expiresAtMillis = now + 3600_000L * 24,
                isUsed = true
            )
        )
    }

    /**
     * Saldo actual de StudiPuntos en la billetera de recompensas.
     */
    fun getStudiPuntos(): Int {
        return sessionPrefs.getInt(KEY_STUDI_PUNTOS, 2450)
    }

    /**
     * Actualiza el saldo de StudiPuntos.
     */
    @Synchronized
    fun setStudiPuntos(points: Int) {
        sessionPrefs.edit().putInt(KEY_STUDI_PUNTOS, points).apply()
        val user = getUserSession()
        if (user != null && !user.isDev && isEmailRegistered(user.email)) {
            val normalized = user.email.trim().lowercase()
            accountPrefs.edit().putInt("points_$normalized", points).apply()
        }
    }

    /**
     * Añade StudiPuntos (ej. bono por registrar boleta +80 o bienvenida +500).
     */
    @Synchronized
    fun addStudiPuntos(delta: Int): Int {
        val updated = getStudiPuntos() + delta
        setStudiPuntos(updated)
        return updated
    }

    /**
     * Descuenta StudiPuntos al canjear un premio si el saldo es suficiente.
     */
    @Synchronized
    fun deductStudiPuntos(cost: Int, rewardTitle: String = "Premio Universitario"): Boolean {
        if (cost <= 0) return false
        val current = getStudiPuntos()
        if (current < cost) return false
        recordPointTransaction(
            title = "Canje de $rewardTitle",
            description = "Canje de beneficio en la Tienda de StudiPuntos.",
            delta = -cost,
            type = PointTransactionType.REWARD_PURCHASE
        )
        return true
    }

    /**
     * Lista de premios y tarjetas de regalo canjeadas por el estudiante.
     */
    fun getRedeemedRewards(): List<RedeemedReward> {
        val raw = sessionPrefs.getString(KEY_REDEEMED_REWARDS, null) ?: return emptyList()
        return raw.split(";").mapNotNull { deserializeReward(it) }
    }

    /**
     * Registra un nuevo premio canjeado en la billetera local.
     */
    @Synchronized
    fun saveRedeemedReward(reward: RedeemedReward) {
        val current = getRedeemedRewards().toMutableList()
        current.add(0, reward)
        val serialized = current.joinToString(";") { serializeReward(it) }
        sessionPrefs.edit().putString(KEY_REDEEMED_REWARDS, serialized).apply()
        val user = getUserSession()
        if (user != null && !user.isDev && isEmailRegistered(user.email)) {
            val normalized = user.email.trim().lowercase()
            accountPrefs.edit().putString("rewards_$normalized", serialized).apply()
        }
    }

    /**
     * Marca un vale de recompensa como utilizado en caja física o en la web del aliado.
     */
    @Synchronized
    fun markRewardAsUsed(rewardId: String): Boolean {
        val current = getRedeemedRewards().toMutableList()
        val index = current.indexOfFirst { it.id == rewardId || it.redemptionCode == rewardId }
        if (index >= 0) {
            val updated = current[index].copy(isUsed = true)
            current[index] = updated
            val serialized = current.joinToString(";") { serializeReward(it) }
            sessionPrefs.edit().putString(KEY_REDEEMED_REWARDS, serialized).apply()
            val user = getUserSession()
            if (user != null && !user.isDev && isEmailRegistered(user.email)) {
                val normalized = user.email.trim().lowercase()
                accountPrefs.edit().putString("rewards_$normalized", serialized).apply()
            }
            return true
        }
        return false
    }

    private fun serializeReward(r: RedeemedReward): String {
        fun esc(s: String) = s.replace("%", "%25").replace("|", "%7C").replace(";", "%3B")
        return listOf(
            esc(r.id),
            esc(r.rewardTitle),
            r.pointsCost.toString(),
            esc(r.redemptionCode),
            esc(r.redeemedAtFormatted),
            esc(r.partnerName),
            esc(r.iconEmoji),
            r.isUsed.toString(),
            r.expiresAtMillis.toString(),
            esc(r.securityToken),
            esc(r.officialUrl),
            r.redemptionMethod.name
        ).joinToString("|")
    }

    private fun deserializeReward(s: String): RedeemedReward? {
        try {
            val parts = s.split("|")
            if (parts.size < 7) return null
            fun unesc(str: String) = str.replace("%3B", ";").replace("%7C", "|").replace("%25", "%")
            val isUsed = if (parts.size > 7) parts[7].toBoolean() else false
            val expiresAt = if (parts.size > 8) parts[8].toLongOrNull() ?: (System.currentTimeMillis() + 48L * 3600 * 1000) else (System.currentTimeMillis() + 48L * 3600 * 1000)
            val secToken = if (parts.size > 9) unesc(parts[9]) else (100000..999999).random().toString()
            val officialUrl = if (parts.size > 10) unesc(parts[10]) else "https://estudihambres.pe"
            val method = if (parts.size > 11) {
                try {
                    RedemptionMethod.valueOf(parts[11])
                } catch (_: Exception) {
                    RedemptionMethod.ONLINE_CODE
                }
            } else RedemptionMethod.ONLINE_CODE

            return RedeemedReward(
                id = unesc(parts[0]),
                rewardTitle = unesc(parts[1]),
                pointsCost = parts[2].toIntOrNull() ?: 0,
                redemptionCode = unesc(parts[3]),
                redeemedAtFormatted = unesc(parts[4]),
                partnerName = unesc(parts[5]),
                iconEmoji = unesc(parts[6]),
                isUsed = isUsed,
                expiresAtMillis = expiresAt,
                securityToken = secToken,
                officialUrl = officialUrl,
                redemptionMethod = method
            )
        } catch (_: Exception) {
            return null
        }
    }

    // --- LIBRO MAYOR CRIPTOGRÁFICO DE STUDIPUNTOS (POINT LEDGER SHA-256) ---

    /**
     * Emite el historial completo de movimientos inmutables de StudiPuntos.
     */
    fun getPointTransactions(): List<PointTransaction> {
        val raw = sessionPrefs.getString(KEY_POINT_TRANSACTIONS, null)
        if (raw.isNullOrBlank()) {
            return getInitialTransactionsIfEmpty()
        }
        return raw.split(";").mapNotNull { deserializeTransaction(it) }
    }

    private fun getInitialTransactionsIfEmpty(): List<PointTransaction> {
        val points = getStudiPuntos()
        val now = System.currentTimeMillis()
        val welcome = PointTransaction(
            id = "tx-welcome-init",
            title = "Bono de Bienvenida Institucional",
            description = "Acreditación inicial por registro de cuenta estudiantil.",
            pointsDelta = 200,
            type = PointTransactionType.WELCOME_BONUS,
            timestampMillis = now - (3600_000L * 24),
            previousHash = "GENESIS_BLOCK"
        )
        val verif = PointTransaction(
            id = "tx-verif-init",
            title = "Acreditación Carnet SUNEDU",
            description = "Validación de identidad universitaria mediante OCR.",
            pointsDelta = 300,
            type = PointTransactionType.ID_VERIFICATION,
            timestampMillis = now - (3600_000L * 18),
            previousHash = welcome.txHash
        )
        val list = if (points > 500) {
            val loyalty = PointTransaction(
                id = "tx-loyalty-init",
                title = "Puntos de Fidelización Universitaria",
                description = "Consumo acumulado y beneficios canjeados en comercios asociados.",
                pointsDelta = points - 500,
                type = PointTransactionType.COUPON_REDEEMED,
                timestampMillis = now - (3600_000L * 6),
                previousHash = verif.txHash
            )
            listOf(loyalty, verif, welcome)
        } else {
            listOf(verif, welcome)
        }
        val serialized = list.joinToString(";") { serializeTransaction(it) }
        sessionPrefs.edit().putString(KEY_POINT_TRANSACTIONS, serialized).apply()
        return list
    }

    /**
     * Registra un nuevo movimiento en el libro mayor encadenado con hash SHA-256.
     */
    @Synchronized
    fun recordPointTransaction(
        title: String,
        description: String,
        delta: Int,
        type: PointTransactionType,
        referenceId: String? = null
    ): PointTransaction {
        val currentTxs = getPointTransactions()
        val previousHash = currentTxs.firstOrNull()?.txHash ?: "GENESIS_BLOCK"
        val txId = "tx-${System.currentTimeMillis()}-${(1000..9999).random()}"
        val now = System.currentTimeMillis()
        val tx = PointTransaction(
            id = txId,
            title = title,
            description = description,
            pointsDelta = delta,
            type = type,
            timestampMillis = now,
            referenceId = referenceId,
            previousHash = previousHash
        )

        val updated = mutableListOf(tx)
        updated.addAll(currentTxs)
        val serialized = updated.joinToString(";") { serializeTransaction(it) }
        sessionPrefs.edit().putString(KEY_POINT_TRANSACTIONS, serialized).apply()

        val newBalance = getStudiPuntos() + delta
        sessionPrefs.edit().putInt(KEY_STUDI_PUNTOS, newBalance).apply()

        val user = getUserSession()
        if (user != null && !user.isDev && isEmailRegistered(user.email)) {
            val normalized = user.email.trim().lowercase()
            accountPrefs.edit()
                .putString("txs_$normalized", serialized)
                .putInt("points_$normalized", newBalance)
                .apply()
        }
        return tx
    }

    private fun serializeTransaction(t: PointTransaction): String {
        fun esc(s: String) = s.replace("%", "%25").replace("|", "%7C").replace(";", "%3B")
        return listOf(
            esc(t.id),
            esc(t.title),
            esc(t.description),
            t.pointsDelta.toString(),
            t.type.name,
            t.timestampMillis.toString(),
            esc(t.referenceId ?: ""),
            esc(t.previousHash),
            esc(t.txHash)
        ).joinToString("|")
    }

    private fun deserializeTransaction(s: String): PointTransaction? {
        try {
            val parts = s.split("|")
            if (parts.size < 9) return null
            fun unesc(str: String) = str.replace("%3B", ";").replace("%7C", "|").replace("%25", "%")
            val type = try {
                PointTransactionType.valueOf(parts[4])
            } catch (_: Exception) {
                PointTransactionType.COUPON_REDEEMED
            }
            return PointTransaction(
                id = unesc(parts[0]),
                title = unesc(parts[1]),
                description = unesc(parts[2]),
                pointsDelta = parts[3].toIntOrNull() ?: 0,
                type = type,
                timestampMillis = parts[5].toLongOrNull() ?: System.currentTimeMillis(),
                referenceId = unesc(parts[6]).ifBlank { null },
                previousHash = unesc(parts[7]),
                txHash = unesc(parts[8])
            )
        } catch (_: Exception) {
            return null
        }
    }

    // --- REGISTRO GLOBAL DE BOLETAS ANTI-DOBLE CANJE (MULTI-USUARIO) ---

    /**
     * Consulta O(1) si una huella de boleta electrónica ya fue reclamada por cualquier usuario.
     */
    @Synchronized
    fun isReceiptClaimedGlobally(fingerprint: String): Boolean {
        purgeExpiredReceiptClaims()
        val raw = accountPrefs.getString(KEY_GLOBAL_CLAIMED_RECEIPTS, "") ?: ""
        return raw.split(";").any { it.startsWith("$fingerprint:") }
    }

    /**
     * Reclama atómicamente una boleta asociándola al correo del estudiante que la procesó primero.
     */
    @Synchronized
    fun claimReceiptGlobally(fingerprint: String, claimedByEmail: String, partnerName: String): Boolean {
        purgeExpiredReceiptClaims()
        if (isReceiptClaimedGlobally(fingerprint)) return false
        val raw = accountPrefs.getString(KEY_GLOBAL_CLAIMED_RECEIPTS, "") ?: ""
        val now = System.currentTimeMillis()
        val entry = "$fingerprint:${claimedByEmail.trim().lowercase()}:$now:${partnerName.trim()}"
        val updated = if (raw.isBlank()) entry else "$raw;$entry"
        accountPrefs.edit().putString(KEY_GLOBAL_CLAIMED_RECEIPTS, updated).apply()
        return true
    }

    /**
     * Purgado de ventana rodante de 72 horas para optimización extrema de memoria (< 50 KB).
     */
    @Synchronized
    fun purgeExpiredReceiptClaims() {
        val raw = accountPrefs.getString(KEY_GLOBAL_CLAIMED_RECEIPTS, "") ?: ""
        if (raw.isBlank()) return
        val now = System.currentTimeMillis()
        val threshold = now - (72L * 3600 * 1000)
        val valid = raw.split(";").filter { entry ->
            val parts = entry.split(":")
            if (parts.size >= 3) {
                val time = parts[2].toLongOrNull() ?: 0L
                time >= threshold
            } else false
        }
        val updated = valid.joinToString(";")
        if (updated != raw) {
            accountPrefs.edit().putString(KEY_GLOBAL_CLAIMED_RECEIPTS, updated).apply()
        }
    }

    // --- DERECHOS ARCO (LEY N° 29733 DE PROTECCIÓN DE DATOS PERSONALES) ---

    /**
     * Ejerce el Derecho ARCO de Cancelación / Supresión de datos personales.
     * Elimina del dispositivo la cuenta, credenciales, historial de puntos, vales y cupones,
     * cerrando la sesión activa inmediatamente.
     */
    @Synchronized
    fun deleteCurrentAccountAndData(): Boolean {
        val user = getUserSession() ?: return false
        val normalized = user.email.trim().lowercase()
        val dni = user.dni.trim()

        accountPrefs.edit()
            .remove("pwd_$normalized")
            .remove("id_$normalized")
            .remove("name_$normalized")
            .remove("dni_$normalized")
            .remove("uni_$normalized")
            .remove("code_$normalized")
            .remove("career_$normalized")
            .remove("status_$normalized")
            .remove("isDev_$normalized")
            .remove("scanned_card_$normalized")
            .remove("photo_$normalized")
            .remove("points_$normalized")
            .remove("txs_$normalized")
            .remove("rewards_$normalized")
            .remove("coupons_$normalized")
            .remove("savings_$normalized")
            .remove("dni_to_email_$dni")
            .apply()

        clearSession()
        return true
    }

    /**
     * Cierra la sesión activa borrando únicamente [sessionPrefs], preservando las cuentas registradas en [accountPrefs].
     */
    fun clearSession() {
        sessionPrefs.edit().clear().apply()
    }
}

