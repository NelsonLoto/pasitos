package com.pasitos.backend.seed

import com.pasitos.backend.child.Sex
import com.pasitos.backend.measurement.LengthPosition
import com.pasitos.backend.measurement.MeasurementEdits
import com.pasitos.shared.growth.MeasurementValidation
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DemoData(val version: Int, val guardians: List<DemoGuardian>, val children: List<DemoChild>) {
    fun id(kind: String, key: String): UUID =
        UUID.nameUUIDFromBytes("pasitos:demo:t18:v$version:$kind:$key".toByteArray(Charsets.UTF_8))

    fun validate(today: LocalDate) {
        require(version == 1) { "Versión de datos demo no soportada" }
        require(guardians.isNotEmpty() && children.isNotEmpty()) { "La demo debe incluir tutores y niños" }
        val guardianKeys = guardians.map { it.key }
        val childKeys = children.map { it.key }
        require(guardianKeys.distinct().size == guardianKeys.size && childKeys.distinct().size == childKeys.size) {
            "Las claves de demo no pueden repetirse"
        }
        (guardianKeys + childKeys).forEach { require(it.matches(Regex("[a-z][a-z0-9-]{0,39}"))) { "Clave de demo inválida" } }
        guardians.forEach { require(it.firstName.isNotBlank()) { "Falta el nombre ficticio del tutor" } }
        children.forEach { child ->
            require(child.firstName.isNotBlank() && child.growthObjective.isNotBlank()) { "Falta la descripción del escenario" }
            require(child.ageMonths in 1..179 && child.gestationalWeeks in 22..42) { "Edad de demo inválida" }
            Sex.valueOf(child.sex)
            require(child.guardians.isNotEmpty() && child.guardians.distinct().size == child.guardians.size) { "Tutores repetidos o ausentes" }
            require(child.guardians.all { it in guardianKeys }) { "Tutor de demo desconocido" }
            val birth = child.birthDate(today)
            child.birth.edits(birth, birth)
            val dates = child.measurements.map { point ->
                require(point.ageMonths >= 0 && point.ageDays >= 0) { "Edad de medición negativa" }
                require(point.author in child.guardians) { "El autor debe ser tutor de ese niño" }
                val date = point.date(birth)
                require(!date.isBefore(birth) && !date.isAfter(today)) { "Medición fuera de la vida del niño" }
                point.values.edits(birth, date)
                point.author to date
            }
            require(dates.distinct().size == dates.size) { "Más de una medición por autor y fecha" }
        }
    }
}

@Serializable
data class DemoGuardian(val key: String, val firstName: String) {
    val email: String get() = "$key@demo.pasitos.invalid"
}

@Serializable
enum class DemoVaccination { UP_TO_DATE, OVERDUE, RETROACTIVE }

@Serializable
data class DemoChild(
    val key: String,
    val firstName: String,
    val sex: String,
    val ageMonths: Int,
    val gestationalWeeks: Int = 40,
    val guardians: List<String>,
    val growthObjective: String,
    val vaccination: DemoVaccination,
    val birth: DemoValues,
    val measurements: List<DemoMeasurement>,
) {
    fun birthDate(today: LocalDate): LocalDate = today.minusMonths(ageMonths.toLong())
}

@Serializable
data class DemoMeasurement(
    val ageMonths: Int = 0,
    val ageDays: Int = 0,
    val author: String,
    val values: DemoValues,
) {
    fun date(birth: LocalDate): LocalDate = birth.plusMonths(ageMonths.toLong()).plusDays(ageDays.toLong())
}

@Serializable
data class DemoValues(
    val weightKg: String? = null,
    val lengthCm: String? = null,
    val headCircumferenceCm: String? = null,
    val lengthPosition: String? = null,
) {
    fun edits(birth: LocalDate, date: LocalDate): MeasurementEdits {
        fun decimal(text: String?, scale: Int, valid: (Double) -> Boolean): BigDecimal? = text?.let {
            val value = requireNotNull(it.toBigDecimalOrNull()) { "Valor demo no numérico" }
            require(value.scale() <= scale && valid(value.toDouble())) { "Valor demo fuera de rango o precisión" }
            value
        }
        val weight = decimal(weightKg, MeasurementValidation.WEIGHT_DECIMALS, MeasurementValidation::isWeightInRange)
        val length = decimal(lengthCm, MeasurementValidation.LENGTH_DECIMALS, MeasurementValidation::isLengthInRange)
        val head = decimal(headCircumferenceCm, MeasurementValidation.HEAD_CIRCUMFERENCE_DECIMALS, MeasurementValidation::isHeadCircumferenceInRange)
        require(weight != null || length != null || head != null) { "Medición demo vacía" }
        require(head == null || MeasurementValidation.offersHeadCircumference(birth, date)) { "Perímetro cefálico desde los 5 años" }
        require(length != null || lengthPosition == null) { "Forma de medición sin talla" }
        val position = if (length == null) null else lengthPosition?.let(LengthPosition::valueOf)
            ?: if (MeasurementValidation.defaultStanding(birth, date)) LengthPosition.STANDING else LengthPosition.LYING
        require(position == null || MeasurementValidation.isLengthPositionValid(birth, date, position == LengthPosition.STANDING)) {
            "Desde los 5 años solo se mide de pie"
        }
        return MeasurementEdits(date, weight, length, position, head, outlierConfirmed = false)
    }
}

fun readDemoData(file: File = File("seed/demo/scenarios.json")): DemoData = Json.decodeFromString(file.readText())
