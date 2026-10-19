package com.pasitos.app.core.navigation

import kotlinx.serialization.Serializable

/** Las 15 pantallas del documento funcional, como destinos de navegación type-safe. */
sealed interface PsRoute {
    @Serializable data object Login : PsRoute // RF01
    @Serializable data object Register : PsRoute // RF01
    /** Después del alta (RF-CTA-01, RF-CTA-02): [afterVerifiedGoHome] decide a dónde seguir una vez verificado. */
    @Serializable data class EmailVerification(val email: String, val afterVerifiedGoHome: Boolean) : PsRoute // RF01
    @Serializable data object ForgotPassword : PsRoute // RF01, RF-CTA-05
    @Serializable data class ChildForm(val childId: String? = null) : PsRoute // RF-HIJ-01 a 03: alta, o edición con childId
    @Serializable data class ChildProfile(val childId: String) : PsRoute // RF-HIJ-02, 03, 09
    @Serializable data object ChildSelector : PsRoute // RF02, RF03
    @Serializable data object Home : PsRoute // RF03
    @Serializable data class MeasurementForm(val childId: String) : PsRoute // RF04
    @Serializable data class GrowthChart(val childId: String) : PsRoute // RF06-09
    @Serializable data class MeasurementHistory(val childId: String) : PsRoute // RF04, RF06
    @Serializable data object VaccinationCalendar : PsRoute // RF10
    @Serializable data class VaccineDetail(val vaccineId: String) : PsRoute // RF11
    @Serializable data object NotificationCenter : PsRoute // RF12, RF13
    @Serializable data object AdviceList : PsRoute // RF14
    @Serializable data class AdviceDetail(val adviceId: String) : PsRoute // RF14
    @Serializable data object ReportExport : PsRoute // RF15
    @Serializable data object Profile : PsRoute // 5to tab
}
