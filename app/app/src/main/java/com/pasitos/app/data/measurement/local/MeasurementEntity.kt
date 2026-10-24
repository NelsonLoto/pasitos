package com.pasitos.app.data.measurement.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pasitos.app.core.sync.SyncState
import com.pasitos.app.data.measurement.model.LengthPosition
import com.pasitos.app.data.measurement.model.Measurement
import com.pasitos.shared.api.AuthorRole
import java.time.LocalDate

/**
 * Copia local de una medición: llega por la sincronización o la carga el tutor en el teléfono, con
 * o sin conexión (RNF-OFF-02). El `id` lo genera quien la crea, así reenviarla no la duplica.
 */
@Entity(
    tableName = "measurements",
    indices = [Index(value = ["child_id", "author_account_id", "measured_on"])],
)
data class MeasurementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "measured_on") val measuredOn: LocalDate,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    @ColumnInfo(name = "length_cm") val lengthCm: Double?,
    @ColumnInfo(name = "length_position") val lengthPosition: LengthPosition?,
    @ColumnInfo(name = "head_circumference_cm") val headCircumferenceCm: Double?,
    @ColumnInfo(name = "outlier_confirmed") val outlierConfirmed: Boolean,
    @ColumnInfo(name = "author_account_id") val authorAccountId: String,
    @ColumnInfo(name = "author_role") val authorRole: AuthorRole,
    @ColumnInfo(name = "sync_state") val syncState: SyncState,
    @ColumnInfo(name = "sync_error") val syncError: String? = null,
    /**
     * La última versión que confirmó el servidor, como fila `MEASUREMENT` en JSON. Nula si el
     * servidor todavía no la tiene. Descartar un cambio rechazado vuelve a esta versión.
     */
    @ColumnInfo(name = "server_copy") val serverCopy: String? = null,
) {
    fun toDomain() = Measurement(
        id = id,
        childId = childId,
        date = measuredOn,
        weightKg = weightKg,
        lengthCm = lengthCm,
        lengthPosition = lengthPosition,
        headCircumferenceCm = headCircumferenceCm,
        authorAccountId = authorAccountId,
        authorRole = authorRole,
        syncState = syncState,
        syncError = syncError,
    )
}
