package com.pasitos.backend.auth

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Mapea la tabla "personas" (V1 + V2) — tutor e hijo conviven en la misma tabla
 * (tipoPersona). Acá vive quién es la persona (nombre, email, documentos); el secreto de
 * login (password_hash, auth_provider) está en [Usuarios] desde V2.
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
    val fechaNacimiento = date("fecha_nacimiento").nullable()
    val sexo = text("sexo").nullable()
    val createdAt = datetime("created_at")
    val dni = text("dni").nullable()
    val matricula = text("matricula").nullable()

    override val primaryKey = PrimaryKey(id)
}
