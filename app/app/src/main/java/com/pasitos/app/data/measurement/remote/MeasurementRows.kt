package com.pasitos.app.data.measurement.remote

import com.pasitos.app.core.sync.SyncState
import com.pasitos.app.data.measurement.local.MeasurementEntity
import com.pasitos.app.data.measurement.model.LengthPosition
import com.pasitos.shared.api.AuthorRole
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Sin nulos explícitos: un valor que no se cargó no viaja (`MeasurementSyncEntity` lo lee como ausente). */
internal val measurementJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Fila `MEASUREMENT` de la sincronización, tal como la arma `MeasurementSyncEntity` en el backend. */
@Serializable
internal data class MeasurementRow(
    val id: String,
    val childId: String,
    val measuredOn: String,
    val weightKg: Double? = null,
    val lengthCm: Double? = null,
    val lengthPosition: LengthPosition? = null,
    val headCircumferenceCm: Double? = null,
    val outlierConfirmed: Boolean = false,
    val authorAccountId: String,
    val authorRole: AuthorRole,
    val deleted: Boolean = false,
) {
    /** La versión del servidor, que también queda como copia para descartar un cambio rechazado. */
    fun toEntity() = MeasurementEntity(
        id = id,
        childId = childId,
        measuredOn = measuredOn.toLocalDate(),
        weightKg = weightKg,
        lengthCm = lengthCm,
        lengthPosition = lengthPosition,
        headCircumferenceCm = headCircumferenceCm,
        outlierConfirmed = outlierConfirmed,
        authorAccountId = authorAccountId,
        authorRole = authorRole,
        syncState = SyncState.SYNCED,
        serverCopy = measurementJson.encodeToString(serializer(), this),
    )
}

/** Campo `data` de un `UPSERT` de `MEASUREMENT` en `/sync/push`. El autor lo pone el servidor. */
@Serializable
internal data class MeasurementPushData(
    val id: String,
    val childId: String,
    val measuredOn: String,
    val weightKg: Double? = null,
    val lengthCm: Double? = null,
    val lengthPosition: LengthPosition? = null,
    val headCircumferenceCm: Double? = null,
    /** Siempre falso en I1: la confirmación de valores atípicos llega con el puntaje Z (I2). */
    val outlierConfirmed: Boolean = false,
) {
    /** Lo que quedó en el servidor cuando aplicó este cambio. */
    fun toRow(authorAccountId: String, authorRole: AuthorRole) = MeasurementRow(
        id = id,
        childId = childId,
        measuredOn = measuredOn,
        weightKg = weightKg,
        lengthCm = lengthCm,
        lengthPosition = lengthPosition,
        headCircumferenceCm = headCircumferenceCm,
        outlierConfirmed = outlierConfirmed,
        authorAccountId = authorAccountId,
        authorRole = authorRole,
    )
}

/** Campo `data` de un `DELETE` en `/sync/push`. */
@Serializable
internal data class MeasurementDeleteData(val id: String)

internal fun MeasurementEntity.toPushData() = MeasurementPushData(
    id = id,
    childId = childId,
    measuredOn = measuredOn.toString(),
    weightKg = weightKg,
    lengthCm = lengthCm,
    lengthPosition = lengthPosition,
    headCircumferenceCm = headCircumferenceCm,
    outlierConfirmed = outlierConfirmed,
)

/** Una fecha mal formada es una fila ilegible: la sincronización la saltea (`SyncedEntity.apply`). */
private fun String.toLocalDate(): LocalDate =
    try {
        LocalDate.parse(this)
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException("Fecha inválida", e)
    }
