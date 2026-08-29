package com.ninosano.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ninosano.app.core.common.PlaceholderScreen
import com.ninosano.app.feature.login.LoginScreen

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
        composable<NsRoute.Register> { PlaceholderScreen(title = "Registro de cuenta") }
        composable<NsRoute.ChildOnboarding> { PlaceholderScreen(title = "Alta de perfil de hijo/a") }
        composable<NsRoute.ChildSelector> { PlaceholderScreen(title = "Selector de hijo") }
        composable<NsRoute.Home> { PlaceholderScreen(title = "Home / Dashboard") }
        composable<NsRoute.MeasurementForm> { PlaceholderScreen(title = "Registro de medición") }
        composable<NsRoute.GrowthChart> { PlaceholderScreen(title = "Curva de crecimiento") }
        composable<NsRoute.MeasurementHistory> { PlaceholderScreen(title = "Historial de mediciones") }
        composable<NsRoute.VaccinationCalendar> { PlaceholderScreen(title = "Calendario de vacunación") }
        composable<NsRoute.VaccineDetail> { PlaceholderScreen(title = "Detalle de vacuna") }
        composable<NsRoute.NotificationCenter> { PlaceholderScreen(title = "Centro de notificaciones") }
        composable<NsRoute.AdviceList> { PlaceholderScreen(title = "Consejos por edad") }
        composable<NsRoute.AdviceDetail> { PlaceholderScreen(title = "Detalle de consejo") }
        composable<NsRoute.ReportExport> { PlaceholderScreen(title = "Exportar reporte PDF") }
        composable<NsRoute.Profile> { PlaceholderScreen(title = "Perfil / Configuración") }
    }
}
