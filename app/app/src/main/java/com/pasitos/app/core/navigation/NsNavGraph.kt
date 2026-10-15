package com.pasitos.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pasitos.app.feature.advicedetail.AdviceDetailScreen
import com.pasitos.app.feature.advicelist.AdviceListScreen
import com.pasitos.app.feature.childonboarding.ChildOnboardingScreen
import com.pasitos.app.feature.childselector.ChildSelectorScreen
import com.pasitos.app.feature.growthchart.GrowthChartScreen
import com.pasitos.app.feature.home.HomeScreen
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
        composable<NsRoute.Profile> {
            ProfileScreen(
                onMyChildrenClick = { navController.navigate(NsRoute.ChildSelector) },
                onNotificationsClick = { navController.navigate(NsRoute.NotificationCenter) },
                onExportReportsClick = { navController.navigate(NsRoute.ReportExport) },
                onLogoutClick = {
                    navController.navigate(NsRoute.Login) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
    }
}
