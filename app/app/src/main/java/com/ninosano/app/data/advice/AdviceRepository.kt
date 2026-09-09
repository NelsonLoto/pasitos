package com.ninosano.app.data.advice

import com.ninosano.app.data.advice.model.AdviceTip
import kotlinx.coroutines.flow.StateFlow

/**
 * Consejos por edad (RF14). Mismo patrón mock/local que el resto de los repositorios —
 * el día que exista el backend, el listado pasa a venir de ahí en vez de estar fijo.
 */
interface AdviceRepository {
    val tips: StateFlow<List<AdviceTip>>
}
