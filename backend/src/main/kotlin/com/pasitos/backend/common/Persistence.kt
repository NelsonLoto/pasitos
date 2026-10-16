package com.pasitos.backend.common

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

/** Ejecuta [block] en una transacción de Exposed, fuera de los hilos de Ktor. */
suspend fun <T> tx(block: Transaction.() -> T): T =
    withContext(Dispatchers.IO) { transaction { block() } }

/** Perfil desde el que se cargó un registro (13 · Base de datos, autoría). */
enum class AuthorRole { GUARDIAN, PEDIATRICIAN }

/** Los momentos se guardan en UTC (ADR-26). */
fun Instant.toUtc(): OffsetDateTime = atOffset(ZoneOffset.UTC)

fun String.toUuidOrNull(): UUID? = try {
    UUID.fromString(this)
} catch (e: IllegalArgumentException) {
    null
}

/** Fecha del calendario en formato ISO (`2026-10-14`). */
fun String.toLocalDateOrNull(): LocalDate? = try {
    LocalDate.parse(this)
} catch (e: DateTimeParseException) {
    null
}
