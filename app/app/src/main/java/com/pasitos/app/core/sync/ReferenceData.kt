package com.pasitos.app.core.sync

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Datos de referencia que no son de ningún niño, como el Calendario Nacional (y desde I2 las
 * tablas de crecimiento). Se piden en los mismos momentos que la sincronización, con `ETag`, para
 * no bajarlos si no cambiaron (14 · Sincronización). Cada dominio registra los suyos con
 * `@IntoSet`, igual que sus [SyncedEntity]: el motor no conoce ninguno en particular.
 */
interface ReferenceData {

    /** Nombre para los logs, por ejemplo `"VACCINATION_CALENDAR"`. */
    val name: String

    /** Trae la versión del servidor si cambió. Lanza `NoConnectionException` o `ApiException`. */
    suspend fun refresh()
}

/** Refresca todos los [ReferenceData], cada uno por su lado: si uno falla, los demás se refrescan igual. */
@Singleton
class ReferenceDataRefresher @Inject constructor(
    private val referenceData: Set<@JvmSuppressWildcards ReferenceData>,
) {

    /** Devuelve los nombres de los que fallaron, que se vuelven a pedir en el próximo disparador. */
    suspend fun refreshAll(): List<String> = referenceData.mapNotNull { data ->
        try {
            data.refresh()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Solo el nombre y el tipo: los datos de referencia no tienen datos personales, pero
            // el mensaje de una excepción podría traer la respuesta del servidor
            Log.w(TAG, "No se pudo refrescar ${data.name}: ${e::class.simpleName}")
            data.name
        }
    }

    private companion object {
        const val TAG = "ReferenceData"
    }
}
