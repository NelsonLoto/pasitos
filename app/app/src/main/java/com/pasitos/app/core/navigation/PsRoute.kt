package com.pasitos.app.core.navigation

import kotlinx.serialization.Serializable

/** Las 15 pantallas del documento funcional, como destinos de navegación type-safe. */
sealed interface PsRoute {
    @Serializable data object Login : PsRoute // RF01
    @Serializable data object Register : PsRoute // RF01
    /** Después del alta (RF-CTA-01, RF-CTA-02): [afterVerifiedGoHome] decide a dónde seguir una vez verificado. */
    @Serializable data class EmailVerification(val email: String, val afterVerifiedGoHome: Boolean) : PsRoute // RF01
    @Serializable data object ForgotPassword : PsRoute // RF01, RF-CTA-05
    @Serializable data class ChildForm(val childId: String? = null, val professionalPatient: Boolean = false) : PsRoute // alta/edición de hijo o paciente
    @Serializable data class ChildProfile(val childId: String) : PsRoute // RF-HIJ-02, 03, 09
    @Serializable data object ChildSelector : PsRoute // RF02, RF03
    @Serializable data object Home : PsRoute // RF03
    @Serializable data class MeasurementForm(val childId: String) : PsRoute // RF04
    @Serializable data class GrowthChart(val childId: String) : PsRoute // RF06-09
    @Serializable data class MeasurementHistory(val childId: String) : PsRoute // RF04, RF06
    @Serializable data object VaccinationCalendar : PsRoute // RF10
    @Serializable data class VaccineDetail(val childId: String, val doseCode: String) : PsRoute // RF-VAC-02, 03, 07
    /** Carga retroactiva (RF-VAC-04): después del alta ([afterChildCreated], RF-HIJ-01 CA2) o desde Vacunas. */
    @Serializable data class PreviousDoses(val childId: String, val afterChildCreated: Boolean = false) : PsRoute
    @Serializable data class OtherVaccine(val childId: String, val vaccineId: String? = null) : PsRoute // RF-VAC-05: alta, o edición con vaccineId
    @Serializable data object NotificationCenter : PsRoute // RF12, RF13
    @Serializable data object AdviceList : PsRoute // RF14
    @Serializable data class AdviceDetail(val adviceId: String) : PsRoute // RF14
    @Serializable data object ReportExport : PsRoute // RF15
    @Serializable data object Profile : PsRoute // 5to tab
    @Serializable data object ProfessionalHome : PsRoute
    @Serializable data object ProfessionalPatients : PsRoute
    @Serializable data object ProfessionalGrowth : PsRoute
    @Serializable data object ProfessionalVaccines : PsRoute
}
