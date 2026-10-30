package com.pasitos.app.data.measurement

import com.pasitos.app.data.measurement.model.Measurement
import com.pasitos.app.data.measurement.model.MeasurementInput
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Mediciones de los hijos (RF-CRE-01 a 03). Lee de la base local y escribe en ella y en la bandeja
 * de salida, así que todo funciona sin conexión (RNF-OFF-02): el envío lo hace la sincronización.
 */
interface MeasurementRepository {

    /** De la más reciente a la más antigua (RF-CRE-03, CA1). */
    fun observeMeasurements(childId: String): Flow<List<Measurement>>

    /** Nula si no existe o se borró. */
    fun observeMeasurement(id: String): Flow<Measurement?>

    /** RN-CRE-12: la medición que el usuario ya cargó en esa fecha, salvo [excludeId]. */
    suspend fun findOwnOn(childId: String, date: LocalDate, excludeId: String? = null): Measurement?

    /** RF-CRE-01. Falla con [DuplicateMeasurementException] si ya cargó una en esa fecha (CA4). */
    suspend fun addMeasurement(childId: String, input: MeasurementInput): Result<Measurement>

    /** RF-CRE-02: solo la propia. Falla con [DuplicateMeasurementException] si la fecha choca con otra. */
    suspend fun updateMeasurement(id: String, input: MeasurementInput): Result<Measurement>

    /** RF-CRE-02: solo la propia. */
    suspend fun deleteMeasurement(id: String): Result<Unit>

    /**
     * Descarta un cambio que el servidor rechazó (14 · Sincronización §3): una medición nueva se
     * borra y una edición vuelve a la última versión del servidor.
     */
    suspend fun discardChange(id: String): Result<Unit>
}

/** RN-CRE-12: el usuario ya cargó [existingId] en esa fecha. */
class DuplicateMeasurementException(val existingId: String) : IllegalStateException("Ya cargaste una medición en esa fecha")
