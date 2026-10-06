package com.pasitos.backend.auth

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Tabla "sesiones" (V2): un login activo por fila. Se guarda el hash del JWT, nunca el token.
 * Revocar una sesión = borrar la fila. Apunta a usuarios.persona_id (no a personas.id) para que
 * la base impida que un hijo tenga sesiones.
 */
object Sesiones : Table("sesiones") {
    val id = uuid("id")
    val usuarioId = uuid("usuario_id")
    val tokenHash = text("token_hash")
    val createdAt = datetime("created_at")
    val expiresAt = datetime("expires_at")

    override val primaryKey = PrimaryKey(id)
}
