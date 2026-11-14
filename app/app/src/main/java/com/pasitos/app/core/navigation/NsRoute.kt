package com.pasitos.app.core.navigation

import kotlinx.serialization.Serializable

/** Las 15 pantallas del documento funcional, como destinos de navegación type-safe. */
sealed interface NsRoute {
    @Serializable data object Login : NsRoute // RF01
    @Serializable data object Register : NsRoute // RF01
    @Serializable data object ChildOnboarding : NsRoute // RF02
    @Serializable data object ChildSelector : NsRoute // RF02, RF03
    @Serializable data object Home : NsRoute // RF03
    @Serializable data class MeasurementForm(val childId: String) : NsRoute // RF04
    @Serializable data class GrowthChart(val childId: String) : NsRoute // RF06-09
    @Serializable data class MeasurementHistory(val childId: String) : NsRoute // RF04, RF06
    @Serializable data object VaccinationCalendar : NsRoute // RF10
    @Serializable data class VaccineDetail(val vaccineId: String) : NsRoute // RF11
    @Serializable data object NotificationCenter : NsRoute // RF12, RF13
    @Serializable data object AdviceList : NsRoute // RF14
    @Serializable data class AdviceDetail(val adviceId: String) : NsRoute // RF14
    @Serializable data object ReportExport : NsRoute // RF15
    @Serializable data object Profile : NsRoute // 5to tab
}
