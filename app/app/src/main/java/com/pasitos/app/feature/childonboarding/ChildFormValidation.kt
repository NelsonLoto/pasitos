package com.pasitos.app.feature.childonboarding

import com.pasitos.app.data.child.model.BirthMeasurementRanges
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildInput
import com.pasitos.app.data.child.model.toDecimalText
import java.time.LocalDate

/** Máximo de caracteres de alergias y de antecedentes (RN-HIJ-10). */
const val MAX_TEXT_LENGTH = 280

/** Edad desde la que ya no se puede dar de alta a un niño (RN-HIJ-01). */
private const val MAX_AGE_YEARS = 15L

/** Primer día de nacimiento admitido hoy: el niño tiene que tener menos de 15 años (RN-HIJ-01). */
internal fun minBirthDate(today: LocalDate): LocalDate = today.minusYears(MAX_AGE_YEARS).plusDays(1)

/** Errores de cada campo, con las mismas reglas que valida el backend (RF-HIJ-01 a 03). */
internal fun validate(state: ChildFormUiState): ChildFormErrors = ChildFormErrors(
    firstName = "Ingresá el nombre".takeIf { state.firstName.isBlank() },
    lastName = "Ingresá el apellido".takeIf { state.lastName.isBlank() },
    sex = "Elegí el sexo".takeIf { state.sex == null },
    birthDate = birthDateError(state.birthDate, state.today),
    gestationalWeeks = gestationalWeeksError(state),
    birthWeightKg = measurementError(state.birthWeightKg),
    birthLengthCm = measurementError(state.birthLengthCm),
    birthHeadCircumferenceCm = measurementError(state.birthHeadCircumferenceCm),
    allergies = textError(state.allergies),
    relevantHistory = textError(state.relevantHistory),
    representation = "Confirmá que sos madre, padre o representante legal"
        .takeIf { !state.isEdit && !state.representationDeclared },
)

/** RN-HIJ-01, con un mensaje que explica el motivo (RF-HIJ-01, CA3). */
private fun birthDateError(date: LocalDate?, today: LocalDate): String? = when {
    date == null -> "Elegí la fecha de nacimiento"
    date.isAfter(today) -> "La fecha de nacimiento no puede ser futura"
    date.isBefore(minBirthDate(today)) -> "Pasitos acompaña a niños y niñas menores de 15 años"
    else -> null
}

/** RN-HIJ-03: entre 22 y 42, o "No lo sé". */
private fun gestationalWeeksError(state: ChildFormUiState): String? {
    if (state.gestationalWeeksUnknown) return null
    if (state.gestationalWeeks.isBlank()) return "Ingresá las semanas o elegí \"No lo sé\""
    val weeks = state.gestationalWeeks.trim().toIntOrNull()
    return if (weeks == null || weeks !in 22..42) "Tienen que ser entre 22 y 42 semanas" else null
}

private fun measurementError(text: String): String? {
    if (text.isBlank()) return null
    val value = parseDecimal(text)
    return if (value == null || !value.isFinite() || value <= 0) "Ingresá un número válido" else null
}

/** RF-HIJ-03, CA2: se indica la cantidad máxima de caracteres. */
private fun textError(text: String): String? =
    "Máximo $MAX_TEXT_LENGTH caracteres".takeIf { text.trim().length > MAX_TEXT_LENGTH }

/** "3,2" o "3.2"; vacío o ilegible es nulo. */
internal fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/** Medidas al nacer fuera de [BirthMeasurementRanges], listas para mostrar (RF-HIJ-01, CA4). */
internal fun implausibleMeasurements(state: ChildFormUiState): List<String> = listOfNotNull(
    outOfRange("Peso", state.birthWeightKg, BirthMeasurementRanges.weightKg, "kg"),
    outOfRange("Longitud", state.birthLengthCm, BirthMeasurementRanges.lengthCm, "cm"),
    outOfRange("Perímetro cefálico", state.birthHeadCircumferenceCm, BirthMeasurementRanges.headCircumferenceCm, "cm"),
)

private fun outOfRange(label: String, text: String, range: ClosedFloatingPointRange<Double>, unit: String): String? {
    val value = parseDecimal(text) ?: return null
    return if (value in range) null else "$label: ${text.trim()} $unit"
}

/** Si cambió algo que recalcula percentiles, estados y vacunas (RF-HIJ-02, CA1). */
internal fun needsRecalculation(state: ChildFormUiState, original: Child): Boolean =
    state.birthDate != original.birthDate ||
        state.sex != original.sex ||
        state.gestationalWeeksValue() != original.gestationalWeeks

private fun ChildFormUiState.gestationalWeeksValue(): Int? =
    if (gestationalWeeksUnknown) null else gestationalWeeks.trim().toIntOrNull()

/** El formulario ya validado, como datos para el repositorio. */
internal fun ChildFormUiState.toInput() = ChildInput(
    firstName = firstName.trim(),
    lastName = lastName.trim(),
    sex = checkNotNull(sex),
    birthDate = checkNotNull(birthDate),
    gestationalWeeks = gestationalWeeksValue(),
    birthWeightKg = parseDecimal(birthWeightKg),
    birthLengthCm = parseDecimal(birthLengthCm),
    birthHeadCircumferenceCm = parseDecimal(birthHeadCircumferenceCm),
    avatarKey = avatarKey,
    allergies = allergies.takeIf { isEdit },
    relevantHistory = relevantHistory.takeIf { isEdit },
)

/** Carga en el formulario los datos actuales del niño, para editarlos. */
internal fun ChildFormUiState.withChild(child: Child) = copy(
    firstName = child.firstName,
    lastName = child.lastName,
    sex = child.sex,
    birthDate = child.birthDate,
    gestationalWeeks = child.gestationalWeeks?.toString().orEmpty(),
    gestationalWeeksUnknown = child.gestationalWeeks == null,
    birthWeightKg = child.birthWeightKg?.toDecimalText().orEmpty(),
    birthLengthCm = child.birthLengthCm?.toDecimalText().orEmpty(),
    birthHeadCircumferenceCm = child.birthHeadCircumferenceCm?.toDecimalText().orEmpty(),
    avatarKey = child.avatarKey,
    allergies = child.allergies.orEmpty(),
    relevantHistory = child.relevantHistory.orEmpty(),
    isLoading = false,
)
