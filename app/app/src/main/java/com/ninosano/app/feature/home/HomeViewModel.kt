package com.ninosano.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ninosano.app.data.auth.AuthRepository
import com.ninosano.app.data.child.ChildRepository
import com.ninosano.app.data.measurement.MeasurementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    childRepository: ChildRepository,
    measurementRepository: MeasurementRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.currentSession,
        childRepository.children,
        childRepository.activeChildId,
        measurementRepository.measurements,
    ) { session, children, activeChildId, measurements ->
        HomeUiState(
            greetingName = session?.displayName?.substringBefore(" ").orEmpty(),
            activeChild = children.firstOrNull { it.id == activeChildId },
            // Último peso cargado del hijo activo (RF04) - puede haber mediciones sin peso
            // (solo talla, por ejemplo), por eso se filtra antes de buscar la más reciente.
            lastMeasurement = measurements
                .filter { it.childId == activeChildId && it.weightKg != null }
                .maxByOrNull { it.date },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
