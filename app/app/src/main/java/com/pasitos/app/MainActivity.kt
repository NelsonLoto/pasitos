package com.pasitos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import com.pasitos.app.data.auth.model.SessionState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pasitos.app.core.designsystem.component.NsTabBar
import com.pasitos.app.core.navigation.NsNavGraph
import com.pasitos.app.core.navigation.currentTabItemOrNull
import com.pasitos.app.core.navigation.toRoute
import com.pasitos.app.ui.theme.NsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NsTheme {
                Column {
                    if (BuildConfig.BUILD_TYPE == "staging") Text("Versión de prueba: no cargues datos de niños reales")
                    Box(Modifier.weight(1f)) { NsApp() }
                }
            }
        }
    }
}

@Composable
private fun NsApp(appViewModel: AppViewModel = hiltViewModel()) {
    val session by appViewModel.sessionState.collectAsStateWithLifecycle()
    if (session is SessionState.Restoring) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    key((session as? SessionState.SignedIn)?.account?.id) { SessionContent(session, appViewModel) }
}

@Composable
private fun SessionContent(session: SessionState, appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentTab = currentTabItemOrNull(currentDestination)
    val activeChildId by appViewModel.activeChildId.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                NsTabBar(
                    selected = currentTab,
                    onSelect = { tab ->
                        navController.navigate(tab.toRoute(activeChildId)) { launchSingleTop = true }
                    },
                )
            }
        },
    ) { innerPadding ->
        NsNavGraph(
            navController = navController,
            signedIn = session is SessionState.SignedIn,
            needsOnboarding = (session as? SessionState.SignedIn)?.needsOnboarding == true,
            expired = (session as? SessionState.SignedOut)?.expired == true,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
