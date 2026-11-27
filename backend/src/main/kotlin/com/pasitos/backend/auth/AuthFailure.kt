package com.pasitos.backend.auth

import com.pasitos.shared.api.ErrorCode

/** Error de autenticación sin dependencia de HTTP. Sus mensajes no contienen datos de entrada. */
class AuthFailure(val code: ErrorCode, override val message: String, val fields: Map<String, String>? = null) : RuntimeException(code.wireName)
