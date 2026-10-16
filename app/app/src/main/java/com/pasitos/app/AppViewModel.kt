package com.pasitos.app

import androidx.lifecycle.ViewModel
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.app.data.child.ChildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** (Prueba de CI, no mergear.) Estado compartido a nivel de la Activity: la sesión y el hijo activo, para armar la ruta del tab "Crecimiento". */
@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
    childRepository: ChildRepository,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = authRepository.sessionState
    val activeChildId: StateFlow<String?> = childRepository.activeChildId
}
