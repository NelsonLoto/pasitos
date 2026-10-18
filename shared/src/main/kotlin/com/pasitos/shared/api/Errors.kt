package com.pasitos.shared.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Formato único de los errores de la API (11 · Backend §5). */
@Serializable
data class ErrorResponse(
    val code: ErrorCode,
    /** En español y listo para mostrar. */
    val message: String,
    /** Solo en 400: mensaje por campo del request. */
    val fields: Map<String, String>? = null,
    /** Solo en 422 (`business_rule`): ID de la regla de la SPEC, por ejemplo `RN-CRE-12`. */
    val rule: String? = null,
)

/**
 * Código de error de la API, en JSON como `snake_case` (`child_not_found`). Una versión vieja de
 * la app que recibe un código que no conoce lo lee como [UNKNOWN] en vez de fallar.
 */
@Serializable(with = ErrorCodeSerializer::class)
enum class ErrorCode(val value: String) {
    /** 400: formato inválido. */
    INVALID_INPUT("invalid_input"),

    /** 401: sin token, token vencido o sesión revocada. */
    UNAUTHORIZED("unauthorized"),

    /** 401: email o contraseña incorrectos. */
    INVALID_CREDENTIALS("invalid_credentials"),

    /** 403: con acceso al recurso pero sin permiso para esa acción. */
    FORBIDDEN("forbidden"),

    /** 403: credenciales correctas, pero el email todavía no se verificó (RF-CTA-02). */
    EMAIL_NOT_VERIFIED("email_not_verified"),

    /** 404: el niño no existe o quien pide no tiene acceso. */
    CHILD_NOT_FOUND("child_not_found"),

    /** 409: ya existe una cuenta con ese email. */
    EMAIL_ALREADY_IN_USE("email_already_in_use"),

    /** 422: regla de negocio incumplida; el ID de la regla va en [ErrorResponse.rule]. */
    BUSINESS_RULE("business_rule"),

    /** 423: cuenta bloqueada por intentos fallidos. */
    ACCOUNT_LOCKED("account_locked"),

    /** 429: límite de requests. */
    RATE_LIMITED("rate_limited"),

    /** 500: error inesperado. */
    INTERNAL_ERROR("internal_error"),

    /** Código que esta versión no conoce. El backend nunca lo envía. */
    UNKNOWN("unknown"),
}

internal object ErrorCodeSerializer : KSerializer<ErrorCode> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.pasitos.shared.api.ErrorCode", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ErrorCode) = encoder.encodeString(value.value)

    override fun deserialize(decoder: Decoder): ErrorCode {
        val value = decoder.decodeString()
        return ErrorCode.entries.firstOrNull { it.value == value } ?: ErrorCode.UNKNOWN
    }
}
