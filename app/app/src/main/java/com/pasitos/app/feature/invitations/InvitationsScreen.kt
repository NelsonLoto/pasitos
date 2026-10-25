package com.pasitos.app.feature.invitations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.AlertCard
import com.pasitos.app.core.designsystem.component.PsAlertTone
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.data.invitation.ReceivedInvitation
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import java.time.Instant

/**
 * [onContinue] solo se usa después del registro: `goHome` es verdadero si aceptó alguna, y falso
 * para seguir al alta de hijo como sin invitaciones.
 */
@Composable
fun InvitationsScreen(
    onBackClick: () -> Unit,
    onContinue: (goHome: Boolean) -> Unit,
    viewModel: InvitationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    InvitationsContent(
        state = state,
        actions = InvitationsActions(
            onBackClick = onBackClick,
            onContinueClick = { onContinue(state.acceptedAny) },
            onRetryClick = viewModel::onRetryClick,
            onAcceptClick = viewModel::onAcceptClick,
            onRepresentationDeclaredChange = viewModel::onRepresentationDeclaredChange,
            onConfirmAccept = viewModel::onConfirmAccept,
            onRejectClick = viewModel::onRejectClick,
            onConfirmReject = viewModel::onConfirmReject,
            onDismissDialog = viewModel::onDismissDialog,
        ),
    )
}

internal class InvitationsActions(
    val onBackClick: () -> Unit = {},
    val onContinueClick: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
    val onAcceptClick: (ReceivedInvitation) -> Unit = {},
    val onRepresentationDeclaredChange: (Boolean) -> Unit = {},
    val onConfirmAccept: () -> Unit = {},
    val onRejectClick: (ReceivedInvitation) -> Unit = {},
    val onConfirmReject: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
)

@Composable
private fun InvitationsContent(state: InvitationsUiState, actions: InvitationsActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = "Invitaciones", showBack = !state.afterRegistration, onBackClick = actions.onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.afterRegistration) {
                Text(
                    text = "Te invitaron a ser tutor. Si aceptás, vas a ver y cargar los datos del niño o niña junto con quien te invitó.",
                    style = PsType.BodyRegular,
                    color = PsColor.TextSecondary,
                )
            }

            if (!state.isOnline) {
                AlertCard(
                    text = "Necesitás conexión a internet para ver y responder las invitaciones.",
                    tone = PsAlertTone.Warning,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.message?.let { message ->
                AlertCard(text = message, tone = PsAlertTone.Info, modifier = Modifier.fillMaxWidth())
            }

            when {
                state.isLoading && state.invitations.isEmpty() ->
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        CircularProgressIndicator()
                    }

                state.loadError != null && state.invitations.isEmpty() -> {
                    Text(text = state.loadError, style = PsType.BodyRegular, color = PsColor.TextSecondary)
                    PsButton(
                        text = "Reintentar",
                        onClick = actions.onRetryClick,
                        variant = PsButtonVariant.Secondary,
                        enabled = state.isOnline,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                state.invitations.isEmpty() ->
                    Text(text = "No tenés invitaciones pendientes.", style = PsType.BodyRegular, color = PsColor.TextSecondary)

                else -> state.invitations.forEach { invitation ->
                    InvitationCard(invitation = invitation, enabled = state.canAct, actions = actions)
                }
            }

            if (state.afterRegistration) {
                Spacer(modifier = Modifier.height(4.dp))
                PsButton(
                    text = if (state.acceptedAny || state.invitations.isEmpty()) "Continuar" else "Ahora no",
                    onClick = actions.onContinueClick,
                    variant = PsButtonVariant.Secondary,
                    enabled = !state.isWorking,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    state.accepting?.let { invitation -> AcceptDialog(state, invitation, actions) }
    state.rejecting?.let { invitation -> RejectDialog(state, invitation, actions) }
}

@Composable
private fun InvitationCard(invitation: ReceivedInvitation, enabled: Boolean, actions: InvitationsActions) {
    PsCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = invitation.description, style = PsType.BodyMedium)
        Text(text = invitation.expiryText, style = PsType.BodySmall, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            PsButton(
                text = "Aceptar",
                onClick = { actions.onAcceptClick(invitation) },
                variant = PsButtonVariant.Primary,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            PsButton(
                text = "Rechazar",
                onClick = { actions.onRejectClick(invitation) },
                variant = PsButtonVariant.Secondary,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** RF-HIJ-06, CA1: la casilla nunca aparece marcada y sin ella no se puede aceptar. */
@Composable
private fun AcceptDialog(state: InvitationsUiState, invitation: ReceivedInvitation, actions: InvitationsActions) {
    AlertDialog(
        onDismissRequest = actions.onDismissDialog,
        title = { Text(text = "¿Ser tutor de ${invitation.childFirstName}?") },
        text = {
            Column {
                Text(
                    text = "Vas a ser tutor secundario junto con ${invitation.inviterFirstName}: vas a ver y cargar los datos de " +
                        "${invitation.childFirstName}, y le vamos a avisar que aceptaste.",
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = state.representationDeclared,
                            enabled = !state.isWorking,
                            role = Role.Checkbox,
                            onValueChange = actions.onRepresentationDeclaredChange,
                        ),
                ) {
                    Checkbox(checked = state.representationDeclared, onCheckedChange = null, enabled = !state.isWorking)
                    Text(
                        text = "Soy madre, padre o representante legal de este niño o niña",
                        style = PsType.BodyRegular,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                state.dialogError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onConfirmAccept, enabled = state.canConfirmAccept) {
                Text(text = if (state.isWorking) "Aceptando…" else "Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismissDialog, enabled = !state.isWorking) { Text(text = "Cancelar") }
        },
    )
}

/** RF-HIJ-06, CA2. */
@Composable
private fun RejectDialog(state: InvitationsUiState, invitation: ReceivedInvitation, actions: InvitationsActions) {
    AlertDialog(
        onDismissRequest = actions.onDismissDialog,
        title = { Text(text = "¿Rechazar la invitación?") },
        text = {
            Column {
                Text(
                    text = "Le vamos a avisar a ${invitation.inviterFirstName}. Si cambiás de idea, te tiene que invitar de nuevo.",
                )
                state.dialogError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onConfirmReject, enabled = state.canAct) {
                Text(text = "Rechazar", color = if (state.canAct) MaterialTheme.colorScheme.error else PsColor.TextSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismissDialog, enabled = !state.isWorking) { Text(text = "Cancelar") }
        },
    )
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun InvitationsScreenPreview() {
    PsTheme {
        InvitationsContent(
            state = InvitationsUiState(
                invitations = listOf(
                    ReceivedInvitation(
                        id = "inv-1",
                        childId = "child-1",
                        childFirstName = "Tomi",
                        childLastName = "García",
                        inviterFirstName = "Ana",
                        inviterLastName = "Pérez",
                        sentAt = Instant.parse("2026-10-24T15:00:00Z"),
                        expiresAt = Instant.parse("2026-11-08T15:00:00Z"),
                    ),
                ),
                isLoading = false,
                afterRegistration = true,
            ),
            actions = InvitationsActions(),
        )
    }
}
