package com.ninosano.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ninosano.app.ui.theme.NsColor
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType

/** Selector de hijo: chip/pill prominente en el header (avatar + nombre + edad + chevron). */
@Composable
fun ChildSelectorChip(name: String, age: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onClick)
            .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(color = NsColor.Primary.copy(alpha = 0.15f))
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(text = name, style = NsType.H3)
            Text(text = age, style = NsType.Caption, color = NsColor.TextSecondary)
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = NsColor.TextSecondary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun Box(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Person, contentDescription = null, tint = NsColor.Primary)
    }
}

@Preview(showBackground = true)
@Composable
private fun ChildSelectorChipPreview() {
    NsTheme {
        ChildSelectorChip(name = "Sofía", age = "2 años y 3 meses", onClick = {}, modifier = Modifier.padding(16.dp))
    }
}
