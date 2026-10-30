package com.pasitos.app.feature.measurement

import com.pasitos.app.core.sync.SyncState
import java.time.LocalDate

/** Una sola pantalla para cargar (sin `measurementId`), editar la propia o ver la de otra persona. */
enum class MeasurementFormMode { NEW, EDIT, READ_ONLY }

/** RF-CRE-01, CA4: ya hay una medición propia en esa fecha y se ofrece editarla. */
data class DuplicateMeasurementUi(val existingId: String, val dateText: String)

data class MeasurementFormUiState(
    val isLoaded: Boolean = false,
    /** La medición ya no está, por ejemplo porque se borró desde otro teléfono. */
    val notFound: Boolean = false,
    val mode: MeasurementFormMode = MeasurementFormMode.NEW,
    /** RF-CRE-01, CA6: el perfil está finalizado y no se ofrece cargar mediciones nuevas. */
    val finalized: Boolean = false,
    val date: LocalDate? = null,
    val minDate: LocalDate? = null,
    val maxDate: LocalDate = LocalDate.MAX,
    val dateError: String? = null,
    val weightKg: String = "",
    val weightError: String? = null,
    val lengthCm: String = "",
    val lengthError: String? = null,
    val standing: Boolean = false,
    /** RF-CRE-10, CA3: desde los 5 años solo se ofrece "de pie". */
    val offersLying: Boolean = true,
    val headCircumferenceCm: String = "",
    val headCircumferenceError: String? = null,
    /** RN-CRE-01: el perímetro cefálico se registra hasta los 5 años. */
    val offersHeadCircumference: Boolean = true,
    val generalError: String? = null,
    val isSaving: Boolean = false,
    val confirmDelete: Boolean = false,
    val duplicate: DuplicateMeasurementUi? = null,
    /** "La cargaste vos", "La cargó la familia" (RF-CRE-02, CA2). */
    val authorText: String? = null,
    val syncState: SyncState? = null,
    /** Motivo del rechazo del servidor, con [SyncState.ERROR]. */
    val syncError: String? = null,
    /** Los valores en modo lectura, como pares de etiqueta y valor. */
    val readOnlyRows: List<Pair<String, String>> = emptyList(),
) {
    val lengthLabel: String get() = if (standing) "Talla (cm)" else "Longitud (cm)"

    val lengthHelp: String get() = if (standing) STANDING_HELP else LYING_HELP

    val canEdit: Boolean get() = mode != MeasurementFormMode.READ_ONLY && !(mode == MeasurementFormMode.NEW && finalized)
}
