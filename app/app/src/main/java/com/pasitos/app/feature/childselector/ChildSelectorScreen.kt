package com.pasitos.app.feature.childselector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.R
import com.pasitos.app.core.designsystem.component.ChildAvatar
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.core.designsystem.component.childAvatarColor
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.child.model.ageAndSexLabel
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import java.time.LocalDate

@Composable
fun ChildSelectorScreen(
    onBackClick: () -> Unit,
    onAddChildClick: () -> Unit,
    onChildProfileClick: (childId: String) -> Unit,
    viewModel: ChildSelectorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect { onBackClick() }
    }
    LaunchedEffect(Unit) {
        viewModel.navigateToAddChild.collect { onAddChildClick() }
    }

    ChildSelectorContent(
        state = state,
        onBackClick = onBackClick,
        onSelectChild = viewModel::onSelectChild,
        onChildProfileClick = onChildProfileClick,
        onAddChildClick = viewModel::onAddChildClick,
    )
}

@Composable
private fun ChildSelectorContent(
    state: ChildSelectorUiState,
    onBackClick: () -> Unit,
    onSelectChild: (String) -> Unit,
    onChildProfileClick: (String) -> Unit,
    onAddChildClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = "Mis hijos", showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 20.dp),
        ) {
            state.children.forEach { child ->
                ChildRow(
                    child = child,
                    today = state.today,
                    selected = child.id == state.activeChildId,
                    onClick = { onSelectChild(child.id) },
                    onProfileClick = { onChildProfileClick(child.id) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            PsButton(
                text = "+ Agregar otro hijo/a",
                onClick = onAddChildClick,
                variant = PsButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChildRow(child: Child, today: LocalDate, selected: Boolean, onClick: () -> Unit, onProfileClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
            .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp))
            .let {
                if (selected) {
                    it.border(width = 1.5.dp, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(14.dp))
                } else {
                    it
                }
            }
            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
    ) {
        ChildAvatar(avatarKey = child.avatarKey, initial = child.firstName.take(1).uppercase(), color = childAvatarColor(child.id))
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(text = child.firstName, style = PsType.BodyRegular)
            Text(text = child.ageAndSexLabel(today), style = PsType.BodySmall, color = PsColor.TextSecondary)
        }
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = "Seleccionado",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onProfileClick) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Ver el perfil de ${child.firstName}",
                tint = PsColor.TextSecondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChildSelectorScreenPreview() {
    val today = LocalDate.of(2026, 10, 19)
    PsTheme {
        ChildSelectorContent(
            state = ChildSelectorUiState(
                children = listOf(
                    Child(id = "child-2", firstName = "Benjamín", lastName = "Gómez", birthDate = today.minusYears(3), sex = ChildSex.Male),
                    Child(
                        id = "child-1",
                        firstName = "Lucía",
                        lastName = "Gómez",
                        birthDate = today.minusMonths(8),
                        sex = ChildSex.Female,
                        avatarKey = "rabbit",
                    ),
                ),
                activeChildId = "child-1",
                today = today,
            ),
            onBackClick = {},
            onSelectChild = {},
            onChildProfileClick = {},
            onAddChildClick = {},
        )
    }
}
