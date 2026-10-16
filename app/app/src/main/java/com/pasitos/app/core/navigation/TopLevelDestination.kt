package com.pasitos.app.core.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.pasitos.app.core.designsystem.component.PsTabItem

/** Fallback si por algún motivo se toca el tab "Crecimiento" sin ningún hijo activo todavía. */
private const val NO_CHILD_SELECTED_ID = "none"

/** Mapeo entre los 5 tabs de la bottom bar y sus rutas de destino. */
fun PsTabItem.toRoute(activeChildId: String?): PsRoute = when (this) {
    PsTabItem.Home -> PsRoute.Home
    PsTabItem.Growth -> PsRoute.GrowthChart(childId = activeChildId ?: NO_CHILD_SELECTED_ID)
    PsTabItem.Vaccines -> PsRoute.VaccinationCalendar
    PsTabItem.Advice -> PsRoute.AdviceList
    PsTabItem.Profile -> PsRoute.Profile
}

/** null si el destino actual no corresponde a ninguno de los 5 tabs (ej. Login, Register). */
fun currentTabItemOrNull(destination: NavDestination?): PsTabItem? = when {
    destination == null -> null
    destination.hasRoute<PsRoute.Home>() -> PsTabItem.Home
    destination.hasRoute<PsRoute.GrowthChart>() -> PsTabItem.Growth
    destination.hasRoute<PsRoute.VaccinationCalendar>() -> PsTabItem.Vaccines
    destination.hasRoute<PsRoute.AdviceList>() -> PsTabItem.Advice
    destination.hasRoute<PsRoute.Profile>() -> PsTabItem.Profile
    else -> null
}
