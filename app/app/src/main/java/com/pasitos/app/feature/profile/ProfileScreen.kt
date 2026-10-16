package com.pasitos.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.ChildAvatar
import com.pasitos.app.core.designsystem.component.PsMenuRow
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

@Composable
fun ProfileScreen(
    onMyChildrenClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onExportReportsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        state = state,
        onMyChildrenClick = onMyChildrenClick,
        onNotificationsClick = onNotificationsClick,
        onExportReportsClick = onExportReportsClick,
        onLogoutClick = {
            viewModel.onLogoutClick()
            onLogoutClick()
        },
    )
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    onMyChildrenClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onExportReportsClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Text(
            text = "Perfil",
            style = PsType.H3,
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 30.dp, bottom = 14.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChildAvatar(initial = state.avatarInitial, color = PsColor.Primary, size = 56.dp)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(text = state.displayName, style = PsType.BodyMedium)
                    Text(text = state.email, style = PsType.BodySmall, color = PsColor.TextSecondary)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(text = "CUENTA")
                MenuGroupCard {
                    PsMenuRow(icon = Icons.Filled.Person, label = "Mis hijos/as", onClick = onMyChildrenClick)
                    HorizontalDivider(color = PsColor.Border)
                    PsMenuRow(icon = Icons.Filled.Notifications, label = "Notificaciones", onClick = onNotificationsClick)
                    HorizontalDivider(color = PsColor.Border)
                    PsMenuRow(icon = Icons.Filled.Description, label = "Exportar reportes", onClick = onExportReportsClick)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(text = "SOPORTE")
                MenuGroupCard {
                    PsMenuRow(icon = Icons.AutoMirrored.Filled.HelpOutline, label = "Ayuda y soporte", onClick = {})
                    HorizontalDivider(color = PsColor.Border)
                    PsMenuRow(icon = Icons.Filled.Lock, label = "Términos y Privacidad", onClick = {})
                }
            }

            MenuGroupCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onLogoutClick)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        text = "Cerrar sesión",
                        style = PsType.BodyRegular,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = PsType.Caption.copy(letterSpacing = 0.5.sp),
        color = PsColor.TextSecondary,
    )
}

@Composable
private fun MenuGroupCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
            .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)),
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    PsTheme {
        ProfileContent(
            state = ProfileUiState(displayName = "María Gómez", email = "maria@email.com", avatarInitial = "M"),
            onMyChildrenClick = {},
            onNotificationsClick = {},
            onExportReportsClick = {},
            onLogoutClick = {},
        )
    }
}
