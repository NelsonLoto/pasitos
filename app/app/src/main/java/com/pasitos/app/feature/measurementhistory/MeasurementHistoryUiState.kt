package com.pasitos.app.feature.measurementhistory

import com.pasitos.app.core.sync.SyncState

/** Una fila del historial (RF-CRE-03, CA1 y CA2). Percentiles, IMC y estado llegan en I2. */
data class MeasurementHistoryRowUi(
    /** Nulo en las medidas al nacer, que se editan en el perfil del niño. */
    val measurementId: String?,
    val dateText: String,
    /** "4 meses · 2 meses corregidos", o "Al nacer". */
    val ageText: String,
    /** "7,25 kg", "68,5 cm acostado · PC 44 cm". */
    val primaryValue: String?,
    val secondaryValues: String?,
    /** "Familia" o "Pediatra"; nulo en las medidas al nacer. */
    val authorText: String?,
    val syncState: SyncState = SyncState.SYNCED,
)

data class MeasurementHistoryUiState(
    val childName: String = "",
    /** RF-CRE-01, CA6: un perfil finalizado no ofrece cargar. */
    val canAdd: Boolean = false,
    /** De la más reciente a la más antigua; las medidas al nacer, si hay, van al final (RN-CRE-14). */
    val rows: List<MeasurementHistoryRowUi> = emptyList(),
)
