package com.pasitos.app.feature.profile.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pasitos.app.core.designsystem.component.AlertCard
import com.pasitos.app.core.designsystem.component.PsAlertTone
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.ui.theme.PsType

/**
 * Estructura común de "Nombre y apellido", "Email" y "Contraseña" (RF-CTA-11): barra con volver y
 * contenido con scroll. Ninguna funciona sin conexión, así que lo avisan antes de que falle.
 */
@Composable
internal fun AccountFormScaffold(
    title: String,
    isOnline: Boolean,
    error: String?,
    onBackClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = title, showBack = true, onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            if (!isOnline) {
                AlertCard(
                    text = "Necesitás conexión a internet para hacer este cambio.",
                    tone = PsAlertTone.Warning,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            content()
            error?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = it, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
