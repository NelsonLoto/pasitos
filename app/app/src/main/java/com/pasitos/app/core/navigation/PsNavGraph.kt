package com.pasitos.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.pasitos.app.feature.advicedetail.AdviceDetailScreen
import com.pasitos.app.feature.advicelist.AdviceListScreen
import com.pasitos.app.feature.childonboarding.ChildOnboardingScreen
import com.pasitos.app.feature.childselector.ChildSelectorScreen
import com.pasitos.app.feature.emailverification.EmailVerificationScreen
import com.pasitos.app.feature.growthchart.GrowthChartScreen
import com.pasitos.app.feature.home.HomeScreen
import com.pasitos.app.feature.login.ForgotPasswordScreen
import com.pasitos.app.feature.login.LoginScreen
import com.pasitos.app.feature.measurement.MeasurementFormScreen
import com.pasitos.app.feature.measurementhistory.MeasurementHistoryScreen
import com.pasitos.app.feature.notificationcenter.NotificationCenterScreen
import com.pasitos.app.feature.profile.ProfileScreen
import com.pasitos.app.feature.register.RegisterScreen
import com.pasitos.app.feature.reportexport.ReportExportScreen
import com.pasitos.app.feature.vaccination.VaccinationCalendarScreen
import com.pasitos.app.feature.vaccinedetail.VaccineDetailScreen

@Composable
fun PsNavGraph(
    navController: NavHostController,
    startDestination: PsRoute,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable<PsRoute.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(PsRoute.Home) {
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
                    val destination = if (route.afterVerifiedGoHome) PsRoute.Home else PsRoute.ChildOnboarding
                    navController.navigate(destination) {
                        popUpTo(PsRoute.Login) { inclusive = true }
                    }
                },
            )
        }
        composable<PsRoute.ForgotPassword> {
            ForgotPasswordScreen(onBackClick = { navController.popBackStack() })
        }
        composable<PsRoute.ChildOnboarding> {
            ChildOnboardingScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.navigate(PsRoute.Home) {
                        popUpTo(PsRoute.Login) { inclusive = true }
                    }
                },
            )
        }
        composable<PsRoute.ChildSelector> {
            ChildSelectorScreen(
                onBackClick = { navController.popBackStack() },
                onAddChildClick = { navController.navigate(PsRoute.ChildOnboarding) },
            )
        }
        composable<PsRoute.Home> {
            HomeScreen(
                onChildChipClick = { navController.navigate(PsRoute.ChildSelector) },
                onNotificationsClick = { navController.navigate(PsRoute.NotificationCenter) },
                onMeasurementClick = { childId -> navController.navigate(PsRoute.MeasurementForm(childId)) },
                onVaccinesClick = { navController.navigate(PsRoute.VaccinationCalendar) },
                onAdviceClick = { navController.navigate(PsRoute.AdviceList) },
                onAddChildClick = { navController.navigate(PsRoute.ChildOnboarding) },
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
                onChildChipClick = { navController.navigate(PsRoute.ChildSelector) },
                onDoseClick = { vaccineId -> navController.navigate(PsRoute.VaccineDetail(vaccineId)) },
            )
        }
        composable<PsRoute.VaccineDetail> {
            VaccineDetailScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
            )
        }
        composable<PsRoute.NotificationCenter> {
            NotificationCenterScreen(onBackClick = { navController.popBackStack() })
        }
        composable<PsRoute.AdviceList> {
            AdviceListScreen(
                onChildChipClick = { navController.navigate(PsRoute.ChildSelector) },
                onAdviceClick = { adviceId -> navController.navigate(PsRoute.AdviceDetail(adviceId)) },
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
                onMyChildrenClick = { navController.navigate(PsRoute.ChildSelector) },
                onNotificationsClick = { navController.navigate(PsRoute.NotificationCenter) },
                onExportReportsClick = { navController.navigate(PsRoute.ReportExport) },
            )
        }
    }
}
