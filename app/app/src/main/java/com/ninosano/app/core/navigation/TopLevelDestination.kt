package com.ninosano.app.core.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.ninosano.app.core.designsystem.component.NsTabItem

/** Fallback si por algún motivo se toca el tab "Crecimiento" sin ningún hijo activo todavía. */
private const val NO_CHILD_SELECTED_ID = "none"

/** Mapeo entre los 5 tabs de la bottom bar y sus rutas de destino. */
fun NsTabItem.toRoute(activeChildId: String?): NsRoute = when (this) {
    NsTabItem.Home -> NsRoute.Home
    NsTabItem.Growth -> NsRoute.GrowthChart(childId = activeChildId ?: NO_CHILD_SELECTED_ID)
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
