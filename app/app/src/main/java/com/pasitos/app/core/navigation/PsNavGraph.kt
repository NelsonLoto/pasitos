package com.pasitos.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.pasitos.app.feature.advicedetail.AdviceDetailScreen
import com.pasitos.app.feature.advicelist.AdviceListScreen
import com.pasitos.app.feature.childonboarding.ChildFormScreen
import com.pasitos.app.feature.childprofile.ChildProfileScreen
import com.pasitos.app.feature.childselector.ChildSelectorScreen
import com.pasitos.app.feature.emailverification.EmailVerificationScreen
import com.pasitos.app.feature.growthchart.GrowthChartScreen
import com.pasitos.app.feature.home.HomeScreen
import com.pasitos.app.feature.login.ForgotPasswordScreen
import com.pasitos.app.feature.login.LoginScreen
import com.pasitos.app.feature.measurement.MeasurementFormScreen
import com.pasitos.app.feature.measurementhistory.MeasurementHistoryScreen
import com.pasitos.app.feature.othervaccine.OtherVaccineScreen
import com.pasitos.app.feature.previousdoses.PreviousDosesScreen
import com.pasitos.app.feature.profile.ProfileScreen
import com.pasitos.app.feature.profile.account.ChangeEmailScreen
import com.pasitos.app.feature.profile.account.ChangePasswordScreen
import com.pasitos.app.feature.profile.account.EditNameScreen
import com.pasitos.app.feature.register.RegisterScreen
import com.pasitos.app.feature.reportexport.ReportExportScreen
import com.pasitos.app.feature.vaccination.VaccinationCalendarScreen
import com.pasitos.app.feature.vaccinedetail.VaccineDetailScreen
import com.pasitos.app.feature.professional.ProfessionalHomeScreen
import com.pasitos.app.feature.professional.ProfessionalPatientsScreen
import com.pasitos.app.feature.professional.ProfessionalGrowthScreen
import com.pasitos.app.feature.professional.ProfessionalVaccinesScreen
import com.pasitos.app.feature.notificationcenter.NotificationCenterScreen

@Composable
fun PsNavGraph(
    navController: NavHostController,
    startDestination: PsRoute,
    isProfessional: Boolean,
    firstName: String,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable<PsRoute.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(if (isProfessional) PsRoute.ProfessionalHome else PsRoute.Home) {
                        popUpTo(PsRoute.Login) { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate(PsRoute.Register) },
                onForgotPasswordClick = { navController.navigate(PsRoute.ForgotPassword) },
            )
        }
        composable<PsRoute.Register> {
            RegisterScreen(
                onBackClick = { navController.popBackStack() },
                onRegisterSuccess = { email ->
                    navController.navigate(PsRoute.EmailVerification(email, afterVerifiedGoHome = false))
                },
                onLoginClick = { navController.popBackStack() },
            )
        }
        composable<PsRoute.EmailVerification> { backStackEntry ->
            val route = backStackEntry.toRoute<PsRoute.EmailVerification>()
            EmailVerificationScreen(
                onVerified = {
                    val destination = when {
                        isProfessional -> PsRoute.ProfessionalHome
                        route.afterVerifiedGoHome -> PsRoute.Home
                        else -> PsRoute.ChildForm()
                    }
                    navController.navigate(destination) {
                        popUpTo(PsRoute.Login) { inclusive = true }
                    }
                },
            )
        }
        composable<PsRoute.ForgotPassword> {
            ForgotPasswordScreen(onBackClick = { navController.popBackStack() })
        }
        composable<PsRoute.ChildForm> { entry ->
            val formRoute = entry.toRoute<PsRoute.ChildForm>()
            val isEdit = formRoute.childId != null
            ChildFormScreen(
                onBackClick = { navController.popBackStack() },
                onSaved = { saved ->
                    when {
                        isEdit -> navController.popBackStack()
                        // Si por su edad ya debería tener dosis, se ofrece la carga retroactiva (RF-HIJ-01, CA2)
                        saved.offerPreviousDoses -> navController.navigate(PsRoute.PreviousDoses(saved.childId, afterChildCreated = true)) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                        formRoute.professionalPatient -> navController.navigate(PsRoute.ProfessionalHome) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                        // El hijo nuevo queda activo (RF-HIJ-01, CA1): se vuelve a Inicio sin nada atrás
                        else -> navController.navigate(PsRoute.Home) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable<PsRoute.ChildSelector> {
            ChildSelectorScreen(
                onBackClick = { navController.popBackStack() },
                onAddChildClick = { navController.navigate(PsRoute.ChildForm()) },
                onChildProfileClick = { childId -> navController.navigate(PsRoute.ChildProfile(childId)) },
            )
        }
        composable<PsRoute.ChildProfile> {
            ChildProfileScreen(
                onBackClick = { navController.popBackStack() },
                onEditClick = { childId -> navController.navigate(PsRoute.ChildForm(childId)) },
                onDeleted = { navController.popBackStack() },
            )
        }
        composable<PsRoute.Home> {
            HomeScreen(
                onChildChipClick = { navController.navigate(PsRoute.ChildSelector) },
                onNotificationsClick = { navController.navigate(PsRoute.NotificationCenter) },
                onMeasurementClick = { childId -> navController.navigate(PsRoute.MeasurementForm(childId)) },
                onVaccinesClick = { navController.navigate(PsRoute.VaccinationCalendar) },
                onAdviceClick = { navController.navigate(PsRoute.AdviceList) },
                onAddChildClick = { navController.navigate(PsRoute.ChildForm()) },
            )
        }
        composable<PsRoute.MeasurementForm> {
            MeasurementFormScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
            )
        }
        composable<PsRoute.GrowthChart> {
            GrowthChartScreen(
                onChildChipClick = { navController.navigate(PsRoute.ChildSelector) },
                onViewHistoryClick = { childId -> navController.navigate(PsRoute.MeasurementHistory(childId)) },
                onRegisterMeasurementClick = { childId -> navController.navigate(PsRoute.MeasurementForm(childId)) },
            )
        }
        composable<PsRoute.MeasurementHistory> {
            MeasurementHistoryScreen(onBackClick = { navController.popBackStack() })
        }
        composable<PsRoute.VaccinationCalendar> {
            VaccinationCalendarScreen(
                onChildChipClick = { navController.navigate(if (isProfessional) PsRoute.ProfessionalPatients else PsRoute.ChildSelector) },
                onDoseClick = { childId, doseCode -> navController.navigate(PsRoute.VaccineDetail(childId, doseCode)) },
                onMarkPreviousDosesClick = { childId -> navController.navigate(PsRoute.PreviousDoses(childId)) },
                onAddOtherVaccineClick = { childId -> navController.navigate(PsRoute.OtherVaccine(childId)) },
                onOtherVaccineClick = { childId, vaccineId -> navController.navigate(PsRoute.OtherVaccine(childId, vaccineId)) },
            )
        }
        composable<PsRoute.VaccineDetail> {
            VaccineDetailScreen(
                onBackClick = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
        composable<PsRoute.PreviousDoses> { entry ->
            val afterChildCreated = entry.toRoute<PsRoute.PreviousDoses>().afterChildCreated
            PreviousDosesScreen(
                onFinished = {
                    if (afterChildCreated) {
                        navController.navigate(PsRoute.Home) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable<PsRoute.OtherVaccine> {
            OtherVaccineScreen(
                onBackClick = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
        composable<PsRoute.NotificationCenter> {
            NotificationCenterScreen(onBackClick = { navController.popBackStack() })
        }
        composable<PsRoute.AdviceList> {
            AdviceListScreen(
                onChildChipClick = { navController.navigate(PsRoute.ChildSelector) },
                onAdviceClick = { adviceId -> navController.navigate(PsRoute.AdviceDetail(adviceId)) },
                isProfessional = isProfessional,
            )
        }
        composable<PsRoute.AdviceDetail> {
            AdviceDetailScreen(onBackClick = { navController.popBackStack() })
        }
        composable<PsRoute.ReportExport> {
            ReportExportScreen(onBackClick = { navController.popBackStack() })
        }
        composable<PsRoute.Profile> {
            ProfileScreen(
                onMyChildrenClick = {
                    navController.navigate(if (isProfessional) PsRoute.ProfessionalPatients else PsRoute.ChildSelector)
                },
                onNotificationsClick = { navController.navigate(PsRoute.NotificationCenter) },
                onExportReportsClick = { navController.navigate(PsRoute.ReportExport) },
                isProfessional = isProfessional,
                onEditNameClick = { navController.navigate(PsRoute.EditName) },
                onChangeEmailClick = { navController.navigate(PsRoute.ChangeEmail) },
                onChangePasswordClick = { navController.navigate(PsRoute.ChangePassword) },
            )
        }
        composable<PsRoute.ProfessionalHome> {
            ProfessionalHomeScreen(
                firstName = firstName,
                onPatientsClick = { navController.navigate(PsRoute.ProfessionalPatients) },
                onNotificationsClick = { navController.navigate(PsRoute.NotificationCenter) },
                onAddPatientClick = { navController.navigate(PsRoute.ChildForm(professionalPatient = true)) },
            )
        }
        composable<PsRoute.ProfessionalPatients> {
            ProfessionalPatientsScreen(
                onBackClick = { navController.popBackStack() },
                onAddPatientClick = { navController.navigate(PsRoute.ChildForm(professionalPatient = true)) },
                onPatientClick = { patientId ->
                    navController.navigate(PsRoute.GrowthChart(patientId))
                },
            )
        }
        composable<PsRoute.ProfessionalGrowth> {
            ProfessionalGrowthScreen(onPatientsClick = { navController.navigate(PsRoute.ProfessionalPatients) })
        }
        composable<PsRoute.ProfessionalVaccines> {
            ProfessionalVaccinesScreen(onPatientsClick = { navController.navigate(PsRoute.ProfessionalPatients) })
        }
        composable<PsRoute.EditName> {
            EditNameScreen(onBackClick = { navController.popBackStack() })
        }
        composable<PsRoute.ChangeEmail> {
            ChangeEmailScreen(
                onBackClick = { navController.popBackStack() },
                // Sin contraseña, primero hay que crearla (RF-CTA-11, CA4)
                onCreatePasswordClick = {
                    navController.navigate(PsRoute.ChangePassword) { popUpTo(PsRoute.ChangeEmail) { inclusive = true } }
                },
            )
        }
        composable<PsRoute.ChangePassword> {
            ChangePasswordScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
