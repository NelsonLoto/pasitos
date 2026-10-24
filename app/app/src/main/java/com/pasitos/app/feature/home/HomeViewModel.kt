package com.pasitos.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.core.time.ArgentinaClock
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.child.ChildRepository
import com.pasitos.app.data.measurement.MeasurementRepository
import com.pasitos.app.data.measurement.model.Measurement
import com.pasitos.app.data.vaccination.VaccinationRepository
import com.pasitos.app.data.vaccination.model.VaccinationSummary
import com.pasitos.app.data.vaccination.model.summary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    childRepository: ChildRepository,
    measurementRepository: MeasurementRepository,
    vaccinationRepository: VaccinationRepository,
    clock: ArgentinaClock,
) : ViewModel() {

    private val activeChild = combine(childRepository.children, childRepository.activeChildId) { children, activeChildId ->
        children.firstOrNull { it.id == activeChildId }
    }.distinctUntilChanged()

    /** El mismo resumen que arriba del calendario (RF-VAC-01, CA2). */
    private val vaccinationSummary: Flow<VaccinationSummary?> = activeChild.flatMapLatest { child ->
        if (child == null) {
            flowOf(null)
        } else {
            combine(vaccinationRepository.calendar, vaccinationRepository.observeAppliedDoses(child.id)) { calendar, applied ->
                calendar.takeUnless { it.isEmpty }?.statusFor(child.birthDate, applied, clock.today())?.summary()
            }
        }
    }

    /**
     * Último peso cargado del hijo activo: puede haber mediciones sin peso (solo talla, por
     * ejemplo), por eso se busca la más reciente que lo tenga.
     */
    private val lastWeight: Flow<Measurement?> = activeChild.flatMapLatest { child ->
        if (child == null) flowOf(null) else measurementRepository.observeMeasurements(child.id).map { list -> list.firstOrNull { it.weightKg != null } }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.currentSession,
        activeChild,
        lastWeight,
        vaccinationSummary,
    ) { session, activeChild, lastWeight, vaccinationSummary ->
        HomeUiState(
            greetingName = session?.firstName.orEmpty(),
            activeChild = activeChild,
            lastMeasurement = lastWeight,
            vaccinationSummary = vaccinationSummary,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
