package com.ninosano.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ninosano.app.core.common.PlaceholderScreen
import com.ninosano.app.feature.advicedetail.AdviceDetailScreen
import com.ninosano.app.feature.advicelist.AdviceListScreen
import com.ninosano.app.feature.childonboarding.ChildOnboardingScreen
import com.ninosano.app.feature.childselector.ChildSelectorScreen
import com.ninosano.app.feature.growthchart.GrowthChartScreen
import com.ninosano.app.feature.home.HomeScreen
import com.ninosano.app.feature.login.LoginScreen
import com.ninosano.app.feature.measurement.MeasurementFormScreen
import com.ninosano.app.feature.measurementhistory.MeasurementHistoryScreen
import com.ninosano.app.feature.notificationcenter.NotificationCenterScreen
import com.ninosano.app.feature.register.RegisterScreen
import com.ninosano.app.feature.reportexport.ReportExportScreen
import com.ninosano.app.feature.vaccination.VaccinationCalendarScreen
import com.ninosano.app.feature.vaccinedetail.VaccineDetailScreen

@Composable
fun NsNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = NsRoute.Login, modifier = modifier) {
        composable<NsRoute.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NsRoute.Home) {
                        popUpTo(NsRoute.Login) { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate(NsRoute.Register) },
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
        composable<NsRoute.MeasurementHistory> {
            MeasurementHistoryScreen(onBackClick = { navController.popBackStack() })
        }
        composable<NsRoute.VaccinationCalendar> {
            VaccinationCalendarScreen(
                onChildChipClick = { navController.navigate(NsRoute.ChildSelector) },
                onDoseClick = { vaccineId -> navController.navigate(NsRoute.VaccineDetail(vaccineId)) },
            )
        }
        composable<NsRoute.VaccineDetail> {
            VaccineDetailScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
            )
        }
        composable<NsRoute.NotificationCenter> {
            NotificationCenterScreen(onBackClick = { navController.popBackStack() })
        }
        composable<NsRoute.AdviceList> {
            AdviceListScreen(
                onChildChipClick = { navController.navigate(NsRoute.ChildSelector) },
                onAdviceClick = { adviceId -> navController.navigate(NsRoute.AdviceDetail(adviceId)) },
            )
        }
        composable<NsRoute.AdviceDetail> {
            AdviceDetailScreen(onBackClick = { navController.popBackStack() })
        }
        composable<NsRoute.ReportExport> {
            ReportExportScreen(onBackClick = { navController.popBackStack() })
        }
        composable<NsRoute.Profile> { PlaceholderScreen(title = "Perfil / Configuración") }
    }
}
