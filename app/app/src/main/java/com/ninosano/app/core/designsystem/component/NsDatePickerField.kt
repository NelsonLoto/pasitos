package com.ninosano.app.core.designsystem.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DateDisplayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Campo de fecha: se ve como un [NsInputField] normal (mismo aspecto, solo lectura) pero al
 * tocarlo abre un `DatePickerDialog` nativo en vez de teclado libre — evita fechas mal formadas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NsDatePickerField(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "DD/MM/AAAA",
    errorText: String? = null,
    enabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }

    NsInputField(
        label = label,
        value = selectedDate?.format(DateDisplayFormatter).orEmpty(),
        onValueChange = {},
        placeholder = placeholder,
        errorText = errorText,
        enabled = enabled,
        onFieldClick = { showDialog = true },
        modifier = modifier,
    )

    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDialog = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
