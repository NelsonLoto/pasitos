package com.pasitos.backend.auth

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Tabla "usuarios" (V2): solo el secreto de login, 1-a-1 con [Personas] (persona_id es a la
 * vez PK y FK). Un hijo no tiene fila acá. El login busca primero la persona por email y recién
 * después valida la contraseña contra esta tabla.
 */
object Usuarios : Table("usuarios") {
    val personaId = uuid("persona_id")
    val passwordHash = text("password_hash").nullable()
    val authProvider = text("auth_provider").nullable()
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(personaId)
}
