package com.example.estudihambres.core.util

import android.content.Context
import android.content.SharedPreferences
import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus

/**
 * Gestor de persistencia local de la sesión del estudiante mediante [SharedPreferences].
 * Permite mantener la sesión activa al reiniciar la app y navegar directamente a Home.
 *
 * @param context Contexto de la aplicación.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "campuspass_session_prefs"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_FULL_NAME = "key_full_name"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_DNI = "key_dni"
        private const val KEY_UNIVERSITY = "key_university"
        private const val KEY_STUDENT_CODE = "key_student_code"
        private const val KEY_VERIFICATION_STATUS = "key_verification_status"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }

        fun getInstanceOrNull(): SessionManager? = instance
    }

    /**
     * Guarda la sesión completa del estudiante y marca el flag de sesión activa.
     */
    fun saveUserSession(user: StudentUser) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_FULL_NAME, user.fullName)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_DNI, user.dni)
            .putString(KEY_UNIVERSITY, user.university)
            .putString(KEY_STUDENT_CODE, user.studentCode)
            .putString(KEY_VERIFICATION_STATUS, user.verificationStatus.name)
            .apply()
    }

    /**
     * Recupera los datos del usuario en sesión, o null si no ha iniciado sesión.
     */
    fun getUserSession(): StudentUser? {
        if (!isUserLoggedIn()) return null

        val id = prefs.getString(KEY_USER_ID, "usr-default") ?: "usr-default"
        val fullName = prefs.getString(KEY_FULL_NAME, "Estudiante Universitario") ?: "Estudiante Universitario"
        val email = prefs.getString(KEY_EMAIL, "alumno@universidad.edu.pe") ?: "alumno@universidad.edu.pe"
        val dni = prefs.getString(KEY_DNI, "") ?: ""
        val university = prefs.getString(KEY_UNIVERSITY, "Universidad Nacional") ?: "Universidad Nacional"
        val studentCode = prefs.getString(KEY_STUDENT_CODE, "") ?: ""
        val statusStr = prefs.getString(KEY_VERIFICATION_STATUS, VerificationStatus.PENDING_VERIFICATION.name)
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
            verificationStatus = status
        )
    }

    /**
     * Retorna verdadero si el estudiante tiene una sesión guardada.
     */
    fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Actualiza únicamente el estado de verificación del estudiante en persistencia.
     */
    fun updateVerificationStatus(status: VerificationStatus) {
        prefs.edit()
            .putString(KEY_VERIFICATION_STATUS, status.name)
            .apply()
    }

    /**
     * Cierra la sesión activa y limpia los datos guardados.
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
