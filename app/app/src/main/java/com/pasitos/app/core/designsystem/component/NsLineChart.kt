package com.pasitos.app.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.pasitos.app.ui.theme.NsColor
import com.pasitos.app.ui.theme.NsType

/**
 * Gráfico de línea simple: conecta [values] (ya ordenados cronológicamente) espaciados
 * parejo en X y escalados a la altura disponible en Y. No dibuja ejes numéricos ni bandas
 * de referencia — sirve para mostrar la evolución propia de una medición en el tiempo.
 */
@Composable
fun NsLineChart(values: List<Double>, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 170.dp) {
    when {
        values.isEmpty() -> Box(
            modifier = modifier.fillMaxWidth().height(height),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Todavía no hay mediciones cargadas para mostrar la curva.",
                style = NsType.BodySmall,
                color = NsColor.TextSecondary,
            )
        }

        values.size == 1 -> Box(
            modifier = modifier.fillMaxWidth().height(height),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Con una sola medición todavía no se puede trazar una curva.",
                style = NsType.BodySmall,
                color = NsColor.TextSecondary,
            )
        }

        else -> {
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = modifier
                    .fillMaxWidth()
                    .height(height)
                    .padding(12.dp),
            ) {
                val minValue = values.min()
                val maxValue = values.max()
                val valueRange = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
                val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f

                val points = values.mapIndexed { index, value ->
                    val x = stepX * index
                    val normalized = (value - minValue) / valueRange
                    val y = size.height - (normalized * size.height).toFloat()
                    Offset(x, y)
                }

                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = primaryColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 6f,
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(color = primaryColor, radius = 8f, center = points.last())
            }
        }
    }
}
