package com.pasitos.app.feature.measurementhistory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pasitos.app.core.navigation.PsRoute
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.toDecimalText
import com.pasitos.app.data.measurement.MeasurementRepository
import com.pasitos.app.data.measurement.model.Measurement
import com.pasitos.app.feature.measurement.MeasurementDateFormatter
import com.pasitos.app.feature.measurement.headCircumferenceText
import com.pasitos.app.feature.measurement.historyLabel
import com.pasitos.app.feature.measurement.lengthText
import com.pasitos.app.feature.measurement.weightText
import com.pasitos.shared.time.formatAge
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Historial de mediciones de un hijo (RF-CRE-03), sin conexión. */
@HiltViewModel
class MeasurementHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    childRepository: ChildRepository,
    measurementRepository: MeasurementRepository,
) : ViewModel() {

    val childId: String = savedStateHandle.toRoute<PsRoute.MeasurementHistory>().childId

    val uiState: StateFlow<MeasurementHistoryUiState> = combine(
        childRepository.children.map { children -> children.firstOrNull { it.id == childId } },
        measurementRepository.observeMeasurements(childId),
    ) { child, measurements ->
        if (child == null) return@combine MeasurementHistoryUiState()
        MeasurementHistoryUiState(
            childName = child.firstName,
            canAdd = !child.finalized,
            rows = measurements.map { it.toRow(child) } + listOfNotNull(child.birthRow()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeasurementHistoryUiState())
}

private fun Measurement.toRow(child: Child): MeasurementHistoryRowUi {
    val (primary, secondary) = values(weightText(), lengthText(), headCircumferenceText())
    return MeasurementHistoryRowUi(
        measurementId = id,
        dateText = date.format(MeasurementDateFormatter),
        // RF-CRE-03, CA2: en un prematuro, las dos edades
        ageText = formatAge(child.birthDate, maxOf(date, child.birthDate), child.gestationalWeeks?.takeIf { it in 22..42 }).text,
        primaryValue = primary,
        secondaryValues = secondary,
        authorText = authorRole.historyLabel(),
        syncState = syncState,
    )
}

/** RN-CRE-14: las medidas al nacer son el primer punto del historial. */
private fun Child.birthRow(): MeasurementHistoryRowUi? {
    if (birthWeightKg == null && birthLengthCm == null && birthHeadCircumferenceCm == null) return null
    val (primary, secondary) = values(
        birthWeightKg?.let { "${it.toDecimalText()} kg" },
        birthLengthCm?.let { "${it.toDecimalText()} cm" },
        birthHeadCircumferenceCm?.let { "${it.toDecimalText()} cm" },
    )
    return MeasurementHistoryRowUi(
        measurementId = null,
        dateText = birthDate.format(MeasurementDateFormatter),
        ageText = "Al nacer",
        primaryValue = primary,
        secondaryValues = secondary,
        authorText = null,
    )
}

/** El peso arriba, si hay; la longitud o talla y el perímetro cefálico ("PC") abajo. */
private fun values(weight: String?, length: String?, headCircumference: String?): Pair<String?, String?> {
    val rest = listOfNotNull(length, headCircumference?.let { "PC $it" })
    return if (weight != null) {
        weight to rest.joinToString(" · ").ifEmpty { null }
    } else {
        rest.firstOrNull() to rest.drop(1).joinToString(" · ").ifEmpty { null }
    }
}
