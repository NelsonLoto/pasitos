package com.pasitos.backend.measurement

import com.pasitos.backend.common.ChildNotFoundException
import com.pasitos.backend.child.ChildRepository
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.AuthorRole
import com.pasitos.backend.common.BusinessRuleException
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.toLocalDateOrNull
import com.pasitos.backend.common.tx
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class MeasurementService(
    private val measurements: MeasurementRepository,
    private val children: ChildRepository,
    private val clock: ArgentinaClock,
) {

    suspend fun create(caller: Caller, childId: UUID, request: CreateMeasurementRequest): MeasurementResponse {
        val measuredOn = request.measuredOn.toLocalDateOrNull()
            ?: throw InvalidInputException("Fecha de medición inválida")
        if (measuredOn.isAfter(clock.today())) throw InvalidInputException("La fecha no puede ser futura")
        if (request.weightKg == null && request.lengthCm == null && request.headCircumferenceCm == null) {
            throw InvalidInputException("Cargá al menos un valor")
        }
        // Se redondea como en la base (kg con 3 decimales, cm con 1) para responder lo guardado.
        val weightKg = request.weightKg?.toStoredValue(scale = 3, max = MAX_WEIGHT_KG)
        val lengthCm = request.lengthCm?.toStoredValue(scale = 1, max = MAX_CM)
        val headCircumferenceCm = request.headCircumferenceCm?.toStoredValue(scale = 1, max = MAX_CM)

        return tx {
            val child = children.findForGuardian(childId, caller.accountId) ?: throw ChildNotFoundException()
            if (measurements.existsForAuthorOn(childId, caller.accountId, measuredOn)) {
                throw BusinessRuleException("RN-CRE-12", "Ya cargaste una medición en esa fecha")
            }
            val measurement = Measurement(
                id = UUID.randomUUID(),
                childId = childId,
                measuredOn = measuredOn,
                weightKg = weightKg,
                lengthCm = lengthCm,
                lengthPosition = lengthCm?.let { defaultLengthPosition(child.birthDate, measuredOn) },
                headCircumferenceCm = headCircumferenceCm,
                authorAccountId = caller.accountId,
                authorRole = AuthorRole.GUARDIAN,
            )
            measurements.insert(measurement)
            measurement.toResponse()
        }
    }

    suspend fun list(caller: Caller, childId: UUID): List<MeasurementResponse> = tx {
        children.findForGuardian(childId, caller.accountId) ?: throw ChildNotFoundException()
        measurements.listByChild(childId).map { it.toResponse() }
    }

    private fun Double.toStoredValue(scale: Int, max: BigDecimal): BigDecimal {
        val value = if (isFinite()) BigDecimal.valueOf(this).setScale(scale, RoundingMode.HALF_UP) else null
        if (value == null || value.signum() <= 0 || value > max) throw InvalidInputException("Valor fuera de rango")
        return value
    }

    private companion object {
        // Máximos que admiten las columnas numeric(6,3) y numeric(5,1). Los rangos de
        // plausibilidad reales (RN-CRE-11) llegan con el motor de crecimiento.
        val MAX_WEIGHT_KG = BigDecimal("999.999")
        val MAX_CM = BigDecimal("9999.9")
    }
}

/**
 * La app actual no pregunta la forma de medición: se asume la que corresponde a la edad, acostado
 * antes de los 24 meses y de pie después (RN-CRE-03).
 */
internal fun defaultLengthPosition(birthDate: LocalDate, measuredOn: LocalDate): LengthPosition =
    if (ChronoUnit.MONTHS.between(birthDate, measuredOn) < 24) LengthPosition.LYING else LengthPosition.STANDING
