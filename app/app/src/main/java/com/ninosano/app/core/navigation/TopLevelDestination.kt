package com.ninosano.app.core.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.ninosano.app.core.designsystem.component.NsTabItem

/**
 * Placeholder de "hijo activo" hasta que exista selección real de hijo (RF03),
 * que depende del backend. Se resuelve con un ViewModel de estado compartido
 * cuando se implemente el selector de hijo (pantalla 04).
 */
const val PLACEHOLDER_CHILD_ID = "current"

/** Mapeo entre los 5 tabs de la bottom bar y sus rutas de destino. */
fun NsTabItem.toRoute(): NsRoute = when (this) {
    NsTabItem.Home -> NsRoute.Home
    NsTabItem.Growth -> NsRoute.GrowthChart(childId = PLACEHOLDER_CHILD_ID)
    NsTabItem.Vaccines -> NsRoute.VaccinationCalendar
    NsTabItem.Advice -> NsRoute.AdviceList
    NsTabItem.Profile -> NsRoute.Profile
}

/** null si el destino actual no corresponde a ninguno de los 5 tabs (ej. Login, Register). */
fun currentTabItemOrNull(destination: NavDestination?): NsTabItem? = when {
    destination == null -> null
    destination.hasRoute<NsRoute.Home>() -> NsTabItem.Home
    destination.hasRoute<NsRoute.GrowthChart>() -> NsTabItem.Growth
    destination.hasRoute<NsRoute.VaccinationCalendar>() -> NsTabItem.Vaccines
    destination.hasRoute<NsRoute.AdviceList>() -> NsTabItem.Advice
    destination.hasRoute<NsRoute.Profile>() -> NsTabItem.Profile
    else -> null
}
