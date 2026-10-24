package com.pasitos.app.feature.measurement

import com.pasitos.app.data.child.model.toDecimalText
import com.pasitos.app.data.measurement.model.LengthPosition
import com.pasitos.app.data.measurement.model.Measurement
import com.pasitos.shared.api.AuthorRole
import java.time.format.DateTimeFormatter

val MeasurementDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** RF-CRE-10, CA1: ayuda breve para medir acostado. */
const val LYING_HELP =
    "Acostado: boca arriba sobre una superficie firme, con la cabeza apoyada y las piernas estiradas. " +
        "Se mide desde la coronilla hasta los talones."

/** RF-CRE-10, CA2: ayuda breve para medir de pie. */
const val STANDING_HELP =
    "De pie: descalzo, de espaldas a la pared, con los talones juntos y la cabeza, la espalda y los talones " +
        "apoyados. Se mide desde el piso hasta la coronilla, mirando al frente."

fun LengthPosition.label(): String = when (this) {
    LengthPosition.LYING -> "acostado"
    LengthPosition.STANDING -> "de pie"
}

fun Measurement.weightText(): String? = weightKg?.let { "${it.toDecimalText()} kg" }

/** "68,5 cm acostado". */
fun Measurement.lengthText(): String? = lengthCm?.let { cm ->
    listOfNotNull("${cm.toDecimalText()} cm", lengthPosition?.label()).joinToString(" ")
}

fun Measurement.headCircumferenceText(): String? = headCircumferenceCm?.let { "${it.toDecimalText()} cm" }

/** Cómo se muestra el autor en el historial (RF-CRE-03, CA1; 40 · Modelo conceptual, Autoría). */
fun AuthorRole.historyLabel(): String = when (this) {
    AuthorRole.GUARDIAN -> "Familia"
    // El nombre del profesional llega con los vínculos (I3)
    AuthorRole.PEDIATRICIAN -> "Pediatra"
}
