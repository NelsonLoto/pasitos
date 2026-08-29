package com.ninosano.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ninosano.app.core.designsystem.component.NsTabBar
import com.ninosano.app.core.navigation.NsNavGraph
import com.ninosano.app.core.navigation.currentTabItemOrNull
import com.ninosano.app.core.navigation.toRoute
import com.ninosano.app.ui.theme.NsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NsTheme {
                NsApp()
            }
        }
    }
}

@Composable
private fun NsApp() {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentTab = currentTabItemOrNull(currentDestination)

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                NsTabBar(
                    selected = currentTab,
                    onSelect = { tab ->
                        navController.navigate(tab.toRoute()) { launchSingleTop = true }
                    },
                )
            }
        },
    ) { innerPadding ->
        NsNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
