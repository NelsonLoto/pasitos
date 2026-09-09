package com.ninosano.app

import androidx.lifecycle.ViewModel
import com.ninosano.app.data.child.ChildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Estado compartido a nivel de la Activity — hoy solo el hijo activo, para armar la ruta del tab "Crecimiento". */
@HiltViewModel
class AppViewModel @Inject constructor(
    childRepository: ChildRepository,
) : ViewModel() {
    val activeChildId: StateFlow<String?> = childRepository.activeChildId
}
