package com.pasitos.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasitos.app.R
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

enum class PsTabItem(val label: String, val iconRes: Int) {
    Home("Inicio", R.drawable.ic_tab_home),
    Growth("Crecimiento", R.drawable.ic_tab_growth),
    Vaccines("Vacunas", R.drawable.ic_tab_vaccines),
    Advice("Consejos", R.drawable.ic_tab_advice),
    Profile("Perfil", R.drawable.ic_tab_profile),
}

/** Tab bar inferior de 5 secciones (patrón de pantalla del Design System). */
@Composable
fun PsTabBar(selected: PsTabItem, onSelect: (PsTabItem) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.border(BorderStroke(1.dp, PsColor.Border)),
    ) {
        PsTabItem.entries.forEach { item ->
            NavigationBarItem(
                selected = item == selected,
                onClick = { onSelect(item) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label, style = PsType.Caption) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = PsColor.TextSecondary,
                    unselectedTextColor = PsColor.TextSecondary,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PsTabBarPreview() {
    PsTheme {
        PsTabBar(selected = PsTabItem.Home, onSelect = {})
    }
}
