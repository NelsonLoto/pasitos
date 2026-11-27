package com.pasitos.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pasitos.app.core.common.PlaceholderScreen
import com.pasitos.app.feature.childonboarding.ChildOnboardingScreen
import com.pasitos.app.feature.childselector.ChildSelectorScreen
import com.pasitos.app.feature.growthchart.GrowthChartScreen
import com.pasitos.app.feature.home.HomeScreen
import com.pasitos.app.feature.login.LoginScreen
import com.pasitos.app.feature.measurement.MeasurementFormScreen
import com.pasitos.app.feature.register.RegisterScreen

@Composable
fun NsNavGraph(navController: NavHostController, modifier: Modifier = Modifier, signedIn: Boolean = false, expired: Boolean = false, needsOnboarding: Boolean = false) {
    val start = if (!signedIn) NsRoute.Login else if (needsOnboarding) NsRoute.ChildOnboarding else NsRoute.Home
    NavHost(navController = navController, startDestination = start, modifier = modifier) {
        composable<NsRoute.Login> {
            LoginScreen(
                onLoginSuccess = {
                    if (!signedIn) navController.navigate(NsRoute.Home) {
                        popUpTo(NsRoute.Login) { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate(NsRoute.Register) },
                expired = expired,
            )
        }
        composable<NsRoute.Register> {
            RegisterScreen(
                onBackClick = { navController.popBackStack() },
                onRegisterSuccess = { navController.navigate(NsRoute.ChildOnboarding) },
                onLoginClick = { navController.popBackStack() },
            )
        }
        composable<NsRoute.ChildOnboarding> {
            ChildOnboardingScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.navigate(NsRoute.Home) {
                        popUpTo(NsRoute.Login) { inclusive = true }
                    }
                },
            )
        }
        composable<NsRoute.ChildSelector> {
            ChildSelectorScreen(
                onBackClick = { navController.popBackStack() },
                onAddChildClick = { navController.navigate(NsRoute.ChildOnboarding) },
            )
        }
        composable<NsRoute.Home> {
            HomeScreen(
                onChildChipClick = { navController.navigate(NsRoute.ChildSelector) },
                onNotificationsClick = { navController.navigate(NsRoute.NotificationCenter) },
                onMeasurementClick = { childId -> navController.navigate(NsRoute.MeasurementForm(childId)) },
                onVaccinesClick = { navController.navigate(NsRoute.VaccinationCalendar) },
                onAdviceClick = { navController.navigate(NsRoute.AdviceList) },
                onAddChildClick = { navController.navigate(NsRoute.ChildOnboarding) },
            )
        }
        composable<NsRoute.MeasurementForm> {
            MeasurementFormScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
            )
        }
        composable<NsRoute.GrowthChart> {
            GrowthChartScreen(
                onChildChipClick = { navController.navigate(NsRoute.ChildSelector) },
                onViewHistoryClick = { childId -> navController.navigate(NsRoute.MeasurementHistory(childId)) },
                onRegisterMeasurementClick = { childId -> navController.navigate(NsRoute.MeasurementForm(childId)) },
            )
        }
        composable<NsRoute.MeasurementHistory> { PlaceholderScreen(title = "Historial de mediciones") }
        composable<NsRoute.VaccinationCalendar> { PlaceholderScreen(title = "Calendario de vacunación") }
        composable<NsRoute.VaccineDetail> { PlaceholderScreen(title = "Detalle de vacuna") }
        composable<NsRoute.NotificationCenter> { PlaceholderScreen(title = "Centro de notificaciones") }
        composable<NsRoute.AdviceList> { PlaceholderScreen(title = "Consejos por edad") }
        composable<NsRoute.AdviceDetail> { PlaceholderScreen(title = "Detalle de consejo") }
        composable<NsRoute.ReportExport> { PlaceholderScreen(title = "Exportar reporte PDF") }
        composable<NsRoute.Profile> { com.pasitos.app.feature.profile.SessionProfileScreen() }
    }
}
