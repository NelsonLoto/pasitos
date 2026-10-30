package com.pasitos.app

import androidx.lifecycle.ViewModel
import com.pasitos.app.data.account.AccountRepository
import com.pasitos.app.data.auth.AuthRepository
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.app.data.child.ChildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Estado compartido a nivel de la Activity: la sesión, el hijo activo (para armar la ruta del tab
 * "Crecimiento") y los términos pendientes.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
    accountRepository: AccountRepository,
    childRepository: ChildRepository,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = authRepository.sessionState
    val activeChildId: StateFlow<String?> = childRepository.activeChildId

    /** RN-CTA-02: mientras no sea nulo, se muestran los términos nuevos sobre toda la app. */
    val pendingTermsVersion: StateFlow<String?> = accountRepository.pendingTermsVersion
}
