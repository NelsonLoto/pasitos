package com.pasitos.backend.sync

import com.pasitos.backend.child.ChildRepository
import com.pasitos.backend.child.PatientOwnershipRepository
import com.pasitos.backend.child.ChildStatus
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.AuthorRole
import com.pasitos.backend.common.BusinessRuleException
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.ChildNotFoundException
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.NotFoundException
import com.pasitos.backend.common.toLocalDateOrNull
import com.pasitos.backend.common.toUuidOrNull
import com.pasitos.backend.measurement.LengthPosition
import com.pasitos.backend.measurement.Measurement
import com.pasitos.backend.measurement.MeasurementEdits
import com.pasitos.backend.measurement.MeasurementRepository
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.sync.SyncOp
import com.pasitos.shared.growth.MeasurementValidation
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class MeasurementNotFoundException : NotFoundException(ErrorCode.MEASUREMENT_NOT_FOUND, "No se encontró esa medición")

/**
 * `measurements` en la sincronización (I1-B6: RF-CRE-01, RF-CRE-02; ADR-28). Es la única entidad
 * de este incremento que se escribe por `/sync/push` - `apply` cubre tanto el alta como la edición
 * (mismo `id`, RF-CRE-02) y el borrado lógico, siempre con las mismas reglas que tendría un
 * endpoint en línea (14 · Sincronización §3).
 */
class MeasurementSyncEntity(
    private val measurements: MeasurementRepository,
    private val children: ChildRepository,
    private val clock: ArgentinaClock,
    private val patientOwnership: PatientOwnershipRepository = PatientOwnershipRepository(),
) : SyncEntity {

    override val name: String = "MEASUREMENT"

    override fun changesSince(childIds: Set<UUID>, cursor: Long, limit: Int): List<EntityChange> =
        measurements.changesSince(childIds, cursor, limit).map { it.toChange() }

    override fun snapshot(childId: UUID): List<EntityChange> =
        measurements.listByChild(childId).map { it.toChange() }

    override fun apply(caller: Caller, op: SyncOp, data: JsonElement): EntityChange {
        val obj = data.jsonObject
        val id = obj.requireUuid("id")
        return when (op) {
            SyncOp.UPSERT -> upsert(caller, id, obj)
            SyncOp.DELETE -> delete(caller, id)
        }
    }

    /**
     * RF-CRE-01 (alta) y RF-CRE-02 (edición, mismo `id`): solo el autor edita (RF-CRE-02). Una
     * medición borrada no vuelve: reenviar su alta o editarla desde otro teléfono se rechaza como
     * si no existiera, en lugar de insertar otra vez el mismo `id`.
     */
    private fun upsert(caller: Caller, id: UUID, obj: Map<String, JsonElement>): EntityChange {
        val existing = measurements.findById(id)
        if (existing != null && (existing.deletedAt != null || existing.authorAccountId != caller.accountId)) {
            throw MeasurementNotFoundException()
        }

        val childId = existing?.childId ?: obj.requireUuid("childId")
        val child = if (patientOwnership.owns(childId, caller.accountId)) {
            children.findById(childId)?.takeIf { it.deletedAt == null }
        } else {
            children.findForGuardian(childId, caller.accountId)
        } ?: throw ChildNotFoundException()
        // RF-CRE-01, CA6: un perfil finalizado no admite mediciones nuevas (no afecta a editar las que ya tiene).
        if (existing == null && child.status != ChildStatus.ACTIVE) {
            throw BusinessRuleException("RF-CRE-01-CA6", "Este perfil está finalizado, ya no admite mediciones nuevas")
        }

        val measuredOn = obj.requireString("measuredOn").toLocalDateOrNull()
            ?: throw InvalidInputException("Fecha de medición inválida")
        // RF-CRE-01, CA2.
        if (measuredOn.isAfter(clock.today())) throw InvalidInputException("La fecha no puede ser futura")
        if (measuredOn.isBefore(child.birthDate)) throw InvalidInputException("La fecha no puede ser anterior al nacimiento")

        val weightKg = obj.doubleOrNull("weightKg")
        val lengthCm = obj.doubleOrNull("lengthCm")
        val headCircumferenceCm = obj.doubleOrNull("headCircumferenceCm")
        if (weightKg == null && lengthCm == null && headCircumferenceCm == null) {
            throw InvalidInputException("Cargá al menos un valor")
        }
        // RN-CRE-11: rango absoluto de entrada, repetido acá porque llega por sincronización
        // (la app ya lo valida antes de enviar, con la misma función de `shared`).
        weightKg?.let { if (!MeasurementValidation.isWeightInRange(it)) throw InvalidInputException("Peso fuera de rango") }
        lengthCm?.let { if (!MeasurementValidation.isLengthInRange(it)) throw InvalidInputException("Longitud o talla fuera de rango") }
        headCircumferenceCm?.let {
            if (!MeasurementValidation.isHeadCircumferenceInRange(it)) throw InvalidInputException("Perímetro cefálico fuera de rango")
        }

        val lengthPosition = lengthCm?.let {
            val position = obj.requireString("lengthPosition").toLengthPositionOrNull()
                ?: throw InvalidInputException("Forma de medición inválida")
            // RN-CRE-03: desde los 60 meses solo se admite "de pie".
            if (!MeasurementValidation.isLengthPositionValid(child.birthDate, measuredOn, position == LengthPosition.STANDING)) {
                throw InvalidInputException("Desde los 60 meses la talla solo se mide de pie")
            }
            position
        }

        // RN-CRE-12.
        if (measurements.existsForAuthorOn(childId, caller.accountId, measuredOn, excludeId = id)) {
            throw BusinessRuleException("RN-CRE-12", "Ya cargaste una medición en esa fecha")
        }

        val edits = MeasurementEdits(
            measuredOn = measuredOn,
            weightKg = weightKg?.toScale(3),
            lengthCm = lengthCm?.toScale(1),
            lengthPosition = lengthPosition,
            headCircumferenceCm = headCircumferenceCm?.toScale(1),
            outlierConfirmed = obj.booleanOrDefault("outlierConfirmed", default = false),
        )

        if (existing == null) {
            val role = if (patientOwnership.owns(childId, caller.accountId)) AuthorRole.PEDIATRICIAN else AuthorRole.GUARDIAN
            measurements.insert(id, childId, caller.accountId, role, edits)
        } else {
            measurements.update(id, edits)
        }
        return requireNotNull(measurements.findById(id)).toChange()
    }

    /** RF-CRE-02: borrado lógico, solo por el autor; idempotente (ADR-15). */
    private fun delete(caller: Caller, id: UUID): EntityChange {
        val existing = measurements.findById(id) ?: throw MeasurementNotFoundException()
        if (!patientOwnership.owns(existing.childId, caller.accountId) && children.findForGuardian(existing.childId, caller.accountId) == null) throw ChildNotFoundException()
        if (existing.authorAccountId != caller.accountId) throw MeasurementNotFoundException()
        measurements.softDelete(id, clock.now())
        return requireNotNull(measurements.findById(id)).toChange()
    }

    private fun Measurement.toChange() = EntityChange(syncVersion, toJson())

    private fun Measurement.toJson() = buildJsonObject {
        put("id", id.toString())
        put("childId", childId.toString())
        put("measuredOn", measuredOn.toString())
        weightKg?.let { put("weightKg", it.toDouble()) }
        lengthCm?.let { put("lengthCm", it.toDouble()) }
        lengthPosition?.let { put("lengthPosition", it.name) }
        headCircumferenceCm?.let { put("headCircumferenceCm", it.toDouble()) }
        put("outlierConfirmed", outlierConfirmed)
        put("authorAccountId", authorAccountId.toString())
        put("authorRole", authorRole.name)
        put("deleted", deletedAt != null)
    }

    private fun Double.toScale(scale: Int): BigDecimal = BigDecimal.valueOf(this).setScale(scale, RoundingMode.HALF_UP)

    private fun String.toLengthPositionOrNull(): LengthPosition? =
        try {
            LengthPosition.valueOf(this)
        } catch (e: IllegalArgumentException) {
            null
        }

    private fun Map<String, JsonElement>.requireUuid(key: String): UUID =
        requireString(key).toUuidOrNull() ?: throw InvalidInputException("\"$key\" inválido")

    private fun Map<String, JsonElement>.requireString(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: throw InvalidInputException("Falta \"$key\"")

    private fun Map<String, JsonElement>.doubleOrNull(key: String): Double? =
        this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.doubleOrNull

    private fun Map<String, JsonElement>.booleanOrDefault(key: String, default: Boolean): Boolean =
        this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.booleanOrNull ?: default
}
