package com.pasitos.app.data.account

import kotlinx.coroutines.flow.StateFlow

/** Cuenta propia: datos personales y términos (RF-CTA-11, RN-CTA-02). Todo necesita conexión. */
interface AccountRepository {

    /**
     * Versión de los términos que falta aceptar, o nula (RN-CTA-02). La actualiza [refresh]; mientras
     * no sea nula, la app muestra "Actualizamos los términos" sobre todo lo demás.
     */
    val pendingTermsVersion: StateFlow<String?>

    /**
     * `GET /me`: guarda nombre, email y si tiene contraseña tal como están en el servidor, y los
     * términos pendientes. Se llama al abrir la app con sesión y al iniciarla.
     */
    suspend fun refresh(): Result<Unit>

    /** RF-CTA-11: se guarda al instante. */
    suspend fun updateName(firstName: String, lastName: String): Result<Unit>

    /** RF-CTA-11, CA1: manda la verificación al email nuevo; hasta confirmarla sigue el actual. */
    suspend fun requestEmailChange(newEmail: String, password: String): Result<Unit>

    /** RF-CTA-11, CA2: cierra las demás sesiones, no esta. */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    /**
     * RF-CTA-11, CA4: una cuenta sin contraseña recibe el email de recuperación para crearla. Al
     * crearla se cierran todas sus sesiones, también esta (RF-CTA-05, CA2).
     */
    suspend fun requestPasswordCreation(): Result<Unit>

    /** RN-CTA-02: acepta la versión vigente de los términos. */
    suspend fun acceptTerms(): Result<Unit>
}
