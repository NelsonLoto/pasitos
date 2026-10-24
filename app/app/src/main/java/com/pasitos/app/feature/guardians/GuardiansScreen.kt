package com.pasitos.app.feature.guardians

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.AlertCard
import com.pasitos.app.core.designsystem.component.PsAlertTone
import com.pasitos.app.core.designsystem.component.PsBadge
import com.pasitos.app.core.designsystem.component.PsBadgeTone
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.data.child.model.GuardianRole
import com.pasitos.app.data.guardian.Guardian
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

@Composable
fun GuardiansScreen(
    onBackClick: () -> Unit,
    onLeft: () -> Unit,
    viewModel: GuardiansViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.left.collect { onLeft() }
    }

    GuardiansContent(
        state = state,
        actions = GuardiansActions(
            onBackClick = onBackClick,
            onRetryClick = viewModel::onRetryClick,
            onInviteClick = viewModel::onInviteClick,
            onInviteEmailChange = viewModel::onInviteEmailChange,
            onSendInvite = viewModel::onSendInvite,
            onRemoveClick = viewModel::onRemoveClick,
            onConfirmRemove = viewModel::onConfirmRemove,
            onTransferClick = viewModel::onTransferClick,
            onConfirmTransfer = viewModel::onConfirmTransfer,
            onLeaveClick = viewModel::onLeaveClick,
            onConfirmLeave = viewModel::onConfirmLeave,
            onDismissDialog = viewModel::onDismissDialog,
        ),
    )
}

internal class GuardiansActions(
    val onBackClick: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
    val onInviteClick: () -> Unit = {},
    val onInviteEmailChange: (String) -> Unit = {},
    val onSendInvite: () -> Unit = {},
    val onRemoveClick: (Guardian) -> Unit = {},
    val onConfirmRemove: () -> Unit = {},
    val onTransferClick: (Guardian) -> Unit = {},
    val onConfirmTransfer: () -> Unit = {},
    val onLeaveClick: () -> Unit = {},
    val onConfirmLeave: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
)

@Composable
private fun GuardiansContent(state: GuardiansUiState, actions: GuardiansActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(
            title = if (state.childFirstName.isEmpty()) "Tutores" else "Tutores de ${state.childFirstName}",
            showBack = true,
            onBackClick = actions.onBackClick,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.isOnline) {
                AlertCard(
                    text = "Necesitás conexión a internet para ver y gestionar los tutores.",
                    tone = PsAlertTone.Warning,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            when {
                state.isLoading -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    CircularProgressIndicator()
                }

                state.loadError != null -> {
                    Text(text = state.loadError, style = PsType.BodyRegular, color = PsColor.TextSecondary)
                    PsButton(
                        text = "Reintentar",
                        onClick = actions.onRetryClick,
                        variant = PsButtonVariant.Secondary,
                        enabled = state.isOnline,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> GuardianList(state, actions)
            }
        }
    }

    when (val dialog = state.dialog) {
        GuardiansDialog.None -> Unit
        GuardiansDialog.Invite -> InviteDialog(state, actions)
        is GuardiansDialog.Remove -> ConfirmDialog(
            state = state,
            title = "¿Quitar a ${dialog.guardian.firstName} como tutor?",
            text = "Va a dejar de ver el perfil de ${state.childFirstName} y le vamos a avisar. " +
                "Lo que cargó queda en el historial.",
            confirmLabel = "Quitar",
            destructive = true,
            onConfirm = actions.onConfirmRemove,
            onDismiss = actions.onDismissDialog,
        )
        is GuardiansDialog.Transfer -> ConfirmDialog(
            state = state,
            title = "¿Hacer a ${dialog.guardian.firstName} tutor principal?",
            text = "Vas a pasar a ser tutor secundario y ya no vas a poder invitar ni quitar tutores. " +
                "Les vamos a avisar a los dos.",
            confirmLabel = "Ceder el rol",
            destructive = false,
            onConfirm = actions.onConfirmTransfer,
            onDismiss = actions.onDismissDialog,
        )
        GuardiansDialog.Leave -> ConfirmDialog(
            state = state,
            title = "¿Dejar de ser tutor de ${state.childFirstName}?",
            text = "El perfil va a seguir existiendo para sus otros tutores y vos vas a dejar de verlo. " +
                "Le vamos a avisar al tutor principal.",
            confirmLabel = "Dejar de ser tutor",
            destructive = true,
            onConfirm = actions.onConfirmLeave,
            onDismiss = actions.onDismissDialog,
        )
    }
}

@Composable
private fun ColumnScope.GuardianList(state: GuardiansUiState, actions: GuardiansActions) {
    PsCard(modifier = Modifier.fillMaxWidth()) {
        state.guardians.forEachIndexed { index, guardian ->
            GuardianRow(
                guardian = guardian,
                isMe = state.isMe(guardian),
                // Solo el principal gestiona, y solo a los secundarios (RF-HIJ-07, RF-HIJ-08)
                showMenu = state.isPrincipal && guardian.role == GuardianRole.SECONDARY,
                menuEnabled = state.canAct,
                onTransferClick = { actions.onTransferClick(guardian) },
                onRemoveClick = { actions.onRemoveClick(guardian) },
            )
            if (index < state.guardians.lastIndex) HorizontalDivider(color = PsColor.Border)
        }
    }

    Text(
        text = if (state.isPrincipal) {
            "Todos los tutores ven y cargan los datos de ${state.childFirstName}. Solo el tutor principal invita y quita tutores."
        } else {
            "Todos los tutores ven y cargan los datos de ${state.childFirstName}. El tutor principal es quien invita y quita tutores."
        },
        style = PsType.BodySmall,
        color = PsColor.TextSecondary,
    )

    state.message?.let { message ->
        AlertCard(text = message, tone = PsAlertTone.Info, modifier = Modifier.fillMaxWidth())
    }

    Spacer(modifier = Modifier.height(4.dp))
    if (state.isPrincipal) {
        PsButton(
            text = "Invitar tutor",
            onClick = actions.onInviteClick,
            variant = PsButtonVariant.Primary,
            enabled = state.canAct,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        TextButton(onClick = actions.onLeaveClick, enabled = state.canAct, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(text = "Dejar de ser tutor", color = if (state.canAct) MaterialTheme.colorScheme.error else PsColor.TextSecondary)
        }
    }
}

@Composable
private fun GuardianRow(
    guardian: Guardian,
    isMe: Boolean,
    showMenu: Boolean,
    menuEnabled: Boolean,
    onTransferClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = if (isMe) "${guardian.fullName} (vos)" else guardian.fullName, style = PsType.BodyMedium)
            Text(text = guardian.email, style = PsType.BodySmall, color = PsColor.TextSecondary)
        }
        PsBadge(
            text = if (guardian.role == GuardianRole.PRINCIPAL) "Principal" else "Secundario",
            tone = if (guardian.role == GuardianRole.PRINCIPAL) PsBadgeTone.Info else PsBadgeTone.Neutral,
        )
        if (showMenu) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }, enabled = menuEnabled) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Opciones de ${guardian.firstName}")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Hacer tutor principal") },
                        onClick = {
                            expanded = false
                            onTransferClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Quitar", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expanded = false
                            onRemoveClick()
                        },
                    )
                }
            }
        }
    }
}

/** RF-HIJ-05: el email de la persona invitada. */
@Composable
private fun InviteDialog(state: GuardiansUiState, actions: GuardiansActions) {
    AlertDialog(
        onDismissRequest = actions.onDismissDialog,
        title = { Text(text = "Invitar a otro tutor") },
        text = {
            Column {
                Text(
                    text = "Le vamos a mandar un email. Si no tiene cuenta, la puede crear con ese mismo email. " +
                        "La invitación vence en 15 días.",
                )
                Spacer(modifier = Modifier.height(12.dp))
                PsInputField(
                    label = "Email",
                    value = state.inviteEmail,
                    onValueChange = actions.onInviteEmailChange,
                    placeholder = "nombre@ejemplo.com",
                    errorText = state.dialogError,
                    keyboardType = KeyboardType.Email,
                    enabled = !state.isWorking,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onSendInvite, enabled = !state.isWorking && state.isOnline) {
                Text(text = if (state.isWorking) "Enviando…" else "Enviar invitación")
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismissDialog, enabled = !state.isWorking) { Text(text = "Cancelar") }
        },
    )
}

@Composable
private fun ConfirmDialog(
    state: GuardiansUiState,
    title: String,
    text: String,
    confirmLabel: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                Text(text = text)
                state.dialogError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            val enabled = !state.isWorking && state.isOnline
            TextButton(onClick = onConfirm, enabled = enabled) {
                Text(
                    text = confirmLabel,
                    color = when {
                        !enabled -> PsColor.TextSecondary
                        destructive -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isWorking) { Text(text = "Cancelar") }
        },
    )
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun GuardiansScreenPreview() {
    PsTheme {
        GuardiansContent(
            state = GuardiansUiState(
                childFirstName = "Tomi",
                guardians = listOf(
                    Guardian("me", "Ana", "Pérez", "ana@example.com", GuardianRole.PRINCIPAL),
                    Guardian("other", "Beto", "Gómez", "beto@example.com", GuardianRole.SECONDARY),
                ),
                myAccountId = "me",
                isLoading = false,
            ),
            actions = GuardiansActions(),
        )
    }
}
