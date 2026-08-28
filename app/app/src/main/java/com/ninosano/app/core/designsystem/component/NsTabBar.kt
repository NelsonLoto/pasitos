package com.ninosano.app.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.ninosano.app.ui.theme.NsColor
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType

enum class NsTabItem(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Filled.Home),
    Growth("Crecimiento", Icons.AutoMirrored.Filled.ShowChart),
    Vaccines("Vacunas", Icons.Filled.ChildCare),
    Advice("Consejos", Icons.Filled.Lightbulb),
    Profile("Perfil", Icons.Filled.Person),
}

/** Tab bar inferior de 5 secciones (patrón de pantalla del Design System). */
@Composable
fun NsTabBar(selected: NsTabItem, onSelect: (NsTabItem) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NsTabItem.entries.forEach { item ->
            NavigationBarItem(
                selected = item == selected,
                onClick = { onSelect(item) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, style = NsType.Caption) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = NsColor.TextSecondary,
                    unselectedTextColor = NsColor.TextSecondary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NsTabBarPreview() {
    NsTheme {
        NsTabBar(selected = NsTabItem.Home, onSelect = {})
    }
}
