package com.ninosano.app.feature.childselector

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ninosano.app.R
import com.ninosano.app.core.designsystem.component.ChildAvatar
import com.ninosano.app.core.designsystem.component.NsButton
import com.ninosano.app.core.designsystem.component.NsButtonVariant
import com.ninosano.app.core.designsystem.component.NsTopAppBar
import com.ninosano.app.core.designsystem.component.childAvatarColor
import com.ninosano.app.data.child.model.Child
import com.ninosano.app.data.child.model.ChildSex
import com.ninosano.app.data.child.model.ageAndSexLabel
import com.ninosano.app.ui.theme.NsColor
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType
import java.time.LocalDate

@Composable
fun ChildSelectorScreen(
    onBackClick: () -> Unit,
    onAddChildClick: () -> Unit,
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
        onAddChildClick = viewModel::onAddChildClick,
    )
}

@Composable
private fun ChildSelectorContent(
    state: ChildSelectorUiState,
    onBackClick: () -> Unit,
    onSelectChild: (String) -> Unit,
    onAddChildClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NsTopAppBar(title = "Mis hijos", showBack = true, onBackClick = onBackClick)

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
                    selected = child.id == state.activeChildId,
                    onClick = { onSelectChild(child.id) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            NsButton(
                text = "+ Agregar otro hijo/a",
                onClick = onAddChildClick,
                variant = NsButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChildRow(child: Child, selected: Boolean, onClick: () -> Unit) {
    Row(
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        ChildAvatar(initial = child.name.take(1).uppercase(), color = childAvatarColor(child.id))
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(text = child.name, style = NsType.BodyRegular)
            Text(text = child.ageAndSexLabel(), style = NsType.BodySmall, color = NsColor.TextSecondary)
        }
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = "Seleccionado",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChildSelectorScreenPreview() {
    NsTheme {
        ChildSelectorContent(
            state = ChildSelectorUiState(
                children = listOf(
                    Child(id = "child-1", name = "Lucía", birthDate = LocalDate.now().minusMonths(8), sex = ChildSex.Female),
                    Child(id = "child-2", name = "Benjamín", birthDate = LocalDate.now().minusYears(3), sex = ChildSex.Male),
                ),
                activeChildId = "child-1",
            ),
            onBackClick = {},
            onSelectChild = {},
            onAddChildClick = {},
        )
    }
}
