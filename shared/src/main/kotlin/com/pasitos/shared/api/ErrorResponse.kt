package com.pasitos.shared.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Cuerpo de toda respuesta de error de la API (11 · Backend §5). [message] está en español y listo para
 * mostrar; [fields] detalla los errores por campo de un `invalid_input`.
 */
@Serializable
data class ErrorResponse(
    val code: ErrorCode,
    val message: String,
    val fields: Map<String, String>? = null,
)

/**
 * Códigos de error de la API, que viajan en snake_case. Acá están los generales de 11 · Backend §5;
 * cada dominio agrega los suyos. Agregar un código es un cambio compatible: una versión de la app que
 * no lo conoce lo lee como [UNKNOWN] (10 · Módulo compartido §5).
 */
@Serializable(with = ErrorCodeSerializer::class)
enum class ErrorCode(val wireName: String) {
    INVALID_INPUT("invalid_input"),
    UNAUTHORIZED("unauthorized"),
    FORBIDDEN("forbidden"),
    NOT_FOUND("not_found"),
    BUSINESS_RULE("business_rule"),
    RATE_LIMITED("rate_limited"),
    INTERNAL_ERROR("internal_error"),

    /** Un código que esta versión no conoce. El backend nunca lo envía. */
    UNKNOWN("unknown"),
}

/** Lee un código desconocido como [ErrorCode.UNKNOWN], sin depender de la configuración de Json. */
object ErrorCodeSerializer : KSerializer<ErrorCode> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.pasitos.shared.api.ErrorCode", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ErrorCode) = encoder.encodeString(value.wireName)

    override fun deserialize(decoder: Decoder): ErrorCode {
        val wireName = decoder.decodeString()
        return ErrorCode.entries.firstOrNull { it.wireName == wireName } ?: ErrorCode.UNKNOWN
    }
}
