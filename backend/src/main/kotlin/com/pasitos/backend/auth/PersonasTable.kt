package com.pasitos.backend.auth

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Mapea la tabla "personas" (V1__esquema_inicial.sql) — tutor e hijo conviven en la misma
 * tabla (tipoPersona). Este dominio (auth) solo usa filas tipoPersona="tutor"; el dominio
 * child (futuro PR) reusará esta misma tabla para las filas "hijo".
 *
 * Las columnas de texto no tienen largo definido en el DDL real (varchar sin longitud) —
 * se usa text(...) en vez de varchar(n) para no inventar un límite que no existe.
 */
object Personas : Table("personas") {
    val id = uuid("id")
    val tipoPersona = text("tipo_persona")
    val tutorId = uuid("tutor_id").nullable()
    val nombre = text("nombre")
    val email = text("email").nullable()
    val passwordHash = text("password_hash").nullable()
    val authProvider = text("auth_provider").nullable()
    val fechaNacimiento = date("fecha_nacimiento").nullable()
    val sexo = text("sexo").nullable()
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
