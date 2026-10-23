package com.pasitos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pasitos.app.core.designsystem.component.PsTabBar
import com.pasitos.app.core.navigation.PsNavGraph
import com.pasitos.app.core.navigation.PsRoute
import com.pasitos.app.core.navigation.currentTabItemOrNull
import com.pasitos.app.core.navigation.toRoute
import com.pasitos.app.data.auth.model.SessionState
import com.pasitos.app.feature.profile.terms.TermsUpdateScreen
import com.pasitos.shared.api.auth.AccountMode
import com.pasitos.app.ui.theme.PsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PsTheme {
                PsApp()
            }
        }
    }
}

@Composable
private fun PsApp(appViewModel: AppViewModel = hiltViewModel()) {
    val sessionState by appViewModel.sessionState.collectAsStateWithLifecycle()

    // Mientras se lee la sesión guardada no se sabe si mostrar el inicio o el login
    if (sessionState == SessionState.Restoring) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    // RN-CTA-02: con términos nuevos sin aceptar no se muestra nada más. Al aceptarlos la app
    // vuelve a armarse desde el inicio
    val pendingTermsVersion by appViewModel.pendingTermsVersion.collectAsStateWithLifecycle()
    if (sessionState is SessionState.SignedIn && pendingTermsVersion != null) {
        TermsUpdateScreen()
        return
    }

    val navController = rememberNavController()
    val isProfessional = (sessionState as? SessionState.SignedIn)?.session?.let {
        it.lastMode == AccountMode.PEDIATRICIAN || !it.isGuardian
    } == true
    val startDestination: PsRoute = remember {
        if (sessionState is SessionState.SignedIn) {
            if (isProfessional) PsRoute.ProfessionalHome else PsRoute.Home
        } else PsRoute.Login
    }
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentTab = currentTabItemOrNull(currentDestination)
    val activeChildId by appViewModel.activeChildId.collectAsStateWithLifecycle()

    // Si la sesión vence mientras se usa la app, se vuelve al login (RF-CTA-06, CA2)
    LaunchedEffect(sessionState) {
        val destination = navController.currentDestination
        val onPublicScreen = destination == null ||
            destination.hasRoute<PsRoute.Login>() ||
            destination.hasRoute<PsRoute.Register>()
        if (sessionState is SessionState.SignedOut && !onPublicScreen) {
            navController.navigate(PsRoute.Login) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                PsTabBar(
                    selected = currentTab,
                    onSelect = { tab ->
                        navController.navigate(tab.toRoute(activeChildId, isProfessional)) { launchSingleTop = true }
                    },
                    isProfessional = isProfessional,
                )
            }
        },
    ) { innerPadding ->
        PsNavGraph(
            navController = navController,
            startDestination = startDestination,
            isProfessional = isProfessional,
            firstName = (sessionState as? SessionState.SignedIn)?.session?.firstName.orEmpty(),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
