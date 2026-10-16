package com.pasitos.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.pasitos.app.R
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

/** Selector de hijo: chip/pill prominente en el header de Home (avatar + nombre + edad + chevron). */
@Composable
fun ChildSelectorChip(
    name: String,
    ageAndSex: String,
    avatarColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onClick)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp))
            .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp))
            .padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
    ) {
        ChildAvatar(initial = name.take(1).uppercase(), color = avatarColor, size = 44.dp)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f, fill = false)) {
            Text(text = name, style = PsType.H2)
            Text(text = ageAndSex, style = PsType.Caption, color = PsColor.TextSecondary)
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = null,
            tint = PsColor.TextSecondary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Color determinístico por hijo (misma paleta que usa Figma: primary/accent alternados). */
fun childAvatarColor(childId: String): Color =
    if (Math.floorMod(childId.hashCode(), 2) == 0) PsColor.Primary else PsColor.Accent

@Composable
fun ChildAvatar(initial: String, color: Color, size: androidx.compose.ui.unit.Dp = 46.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, style = PsType.H3, color = PsColor.TextOnPrimary)
    }
}

@Preview(showBackground = true)
@Composable
private fun ChildSelectorChipPreview() {
    PsTheme {
        ChildSelectorChip(
            name = "Lucía",
            ageAndSex = "8 meses · Niña",
            avatarColor = PsColor.Primary,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
