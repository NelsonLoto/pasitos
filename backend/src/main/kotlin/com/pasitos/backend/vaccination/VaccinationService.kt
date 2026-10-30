package com.pasitos.backend.vaccination

import com.pasitos.backend.child.ChildRepository
import com.pasitos.backend.common.AccessPolicy
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.AuthorRole
import com.pasitos.backend.common.BusinessRuleException
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.ChildAccess
import com.pasitos.backend.common.ChildNotFoundException
import com.pasitos.backend.common.ForbiddenException
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.NotFoundException
import com.pasitos.backend.common.toLocalDateOrNull
import com.pasitos.backend.common.tx
import com.pasitos.shared.api.AuthorRole as AuthorRoleDto
import com.pasitos.shared.api.ErrorCode
import com.pasitos.shared.api.vaccination.AppliedDoseResponse
import com.pasitos.shared.api.vaccination.CalendarDoseDto
import com.pasitos.shared.api.vaccination.CreateAppliedDoseRequest
import com.pasitos.shared.api.vaccination.FormerDoseDto
import com.pasitos.shared.api.vaccination.MAX_LOT_LENGTH
import com.pasitos.shared.api.vaccination.MAX_OTHER_VACCINE_NAME_LENGTH
import com.pasitos.shared.api.vaccination.MAX_OTHER_VACCINE_NOTE_LENGTH
import com.pasitos.shared.api.vaccination.MAX_PLACE_LENGTH
import com.pasitos.shared.api.vaccination.MAX_RETROACTIVE_DOSES
import com.pasitos.shared.api.vaccination.OtherVaccineRequest
import com.pasitos.shared.api.vaccination.OtherVaccineResponse
import com.pasitos.shared.api.vaccination.PatchAppliedDoseRequest
import com.pasitos.shared.api.vaccination.RetroactiveDosesRequest
import com.pasitos.shared.api.vaccination.RetroactiveDosesResponse
import com.pasitos.shared.api.vaccination.VaccinationCalendarResponse
import com.pasitos.shared.vaccination.CalendarDose
import java.time.LocalDate
import java.util.UUID
import org.jetbrains.exposed.exceptions.ExposedSQLException

class AppliedDoseNotFoundException : NotFoundException(ErrorCode.APPLIED_DOSE_NOT_FOUND, "No se encontró esa dosis")

class OtherVaccineNotFoundException : NotFoundException(ErrorCode.OTHER_VACCINE_NOT_FOUND, "No se encontró esa vacuna")

/** Violación de un índice único en PostgreSQL. */
private const val UNIQUE_VIOLATION = "23505"

/** RF-VAC-01 a 05, RF-VAC-07 (12 · API, Vacunación). */
class VaccinationService(
    private val calendar: CalendarRepository,
    private val appliedDoses: AppliedDoseRepository,
    private val otherVaccines: OtherVaccineRepository,
    private val children: ChildRepository,
    private val accessPolicy: AccessPolicy,
    private val clock: ArgentinaClock,
) {

    /**
     * `GET /vaccination-calendar`: la versión vigente con todas sus filas y los nombres de las
     * dosis que ya no están en ella (RN-VAC-03). Sin calendario publicado, las listas vacías.
     */
    suspend fun calendar(): VaccinationCalendarResponse = tx {
        val current = calendar.currentVersion(clock.today())
            ?: return@tx VaccinationCalendarResponse(effectiveFrom = null, doses = emptyList(), formerDoses = emptyList())
        VaccinationCalendarResponse(
            effectiveFrom = current.effectiveFrom.toString(),
            doses = calendar.doses(current.id).map { it.toDto() },
            formerDoses = calendar.formerDoses(current).map { FormerDoseDto(it.doseCode, it.vaccineName, it.doseLabel) },
        )
    }

    /** RF-VAC-02: solo dosis del calendario vigente que corresponden al niño, una vez cada una (RN-VAC-09). */
    suspend fun createAppliedDose(caller: Caller, childId: UUID, request: CreateAppliedDoseRequest): AppliedDoseResponse {
        val appliedOn = parseAppliedOn(request.appliedOn)
        val lot = cleanText(request.lot, MAX_LOT_LENGTH, "El lote")
        val place = cleanText(request.place, MAX_PLACE_LENGTH, "El lugar")
        val doseCode = request.doseCode.trim()

        return translatingDuplicate {
            tx {
                val access = accessPolicy.require(caller, childId)
                val birthDate = birthDateOf(childId)
                requireAppliedOnInRange(appliedOn, birthDate)
                if (currentDosesFor(birthDate).none { it.doseCode == doseCode }) {
                    throw InvalidInputException("Esa dosis no corresponde al calendario de este niño o niña")
                }
                if (doseCode in appliedDoses.activeCodes(childId)) throw alreadyApplied()
                appliedDoses.insert(childId, doseCode, AppliedDoseValues(appliedOn, lot, place), caller.accountId, access.authorRole()).toDto()
            }
        }
    }

    /**
     * RF-VAC-04: las dosis elegidas quedan aplicadas con fecha no registrada (RN-VAC-04). Tienen
     * que ser del calendario vigente del niño, con fecha indicada anterior a hoy. Las que ya estaban
     * aplicadas se saltean, así reenviar el mismo pedido da el mismo resultado; un código que no
     * corresponde rechaza todo el pedido.
     */
    suspend fun registerRetroactive(caller: Caller, childId: UUID, request: RetroactiveDosesRequest): RetroactiveDosesResponse {
        val codes = request.doseCodes.map { it.trim() }.distinct()
        if (codes.isEmpty()) throw InvalidInputException("Elegí al menos una dosis")
        if (codes.size > MAX_RETROACTIVE_DOSES) throw InvalidInputException("Elegí hasta $MAX_RETROACTIVE_DOSES dosis por vez")

        return translatingDuplicate {
            tx {
                val access = accessPolicy.require(caller, childId)
                val birthDate = birthDateOf(childId)
                val today = clock.today()
                val eligible = currentDosesFor(birthDate)
                    .filter { birthDate.plusMonths(it.ageMonths.toLong()).isBefore(today) }
                    .mapTo(mutableSetOf()) { it.doseCode }
                if (!eligible.containsAll(codes)) {
                    throw InvalidInputException("Alguna de las dosis elegidas no es anterior a hoy o no corresponde a este niño o niña")
                }
                val alreadyApplied = appliedDoses.activeCodes(childId)
                val created = codes.filterNot { it in alreadyApplied }.map { code ->
                    appliedDoses.insert(childId, code, AppliedDoseValues(null, null, null), caller.accountId, access.authorRole()).toDto()
                }
                RetroactiveDosesResponse(created)
            }
        }
    }

    /**
     * RF-VAC-03 y RF-VAC-04 CA3: solo el autor edita fecha, lote y lugar. La fecha solo puede
     * quedar vacía en una dosis de carga retroactiva que todavía no la tiene.
     */
    suspend fun updateAppliedDose(caller: Caller, doseId: UUID, request: PatchAppliedDoseRequest): AppliedDoseResponse {
        val appliedOn = request.appliedOn?.let { parseAppliedOn(it) }
        val lot = cleanText(request.lot, MAX_LOT_LENGTH, "El lote")
        val place = cleanText(request.place, MAX_PLACE_LENGTH, "El lugar")

        return tx {
            val dose = appliedDoses.findActive(doseId)?.takeIf { hasAccess(caller, it.childId) } ?: throw AppliedDoseNotFoundException()
            requireAuthor(caller, dose.authorAccountId)
            if (appliedOn == null && dose.appliedOn != null) throw InvalidInputException("Completá la fecha de aplicación")
            appliedOn?.let { requireAppliedOnInRange(it, birthDateOf(dose.childId)) }
            appliedDoses.update(doseId, AppliedDoseValues(appliedOn, lot, place))
            requireNotNull(appliedDoses.findActive(doseId)).toDto()
        }
    }

    /** RF-VAC-03: solo el autor elimina; el estado de la dosis se recalcula en la app. */
    suspend fun deleteAppliedDose(caller: Caller, doseId: UUID) = tx {
        val dose = appliedDoses.findActive(doseId)?.takeIf { hasAccess(caller, it.childId) } ?: throw AppliedDoseNotFoundException()
        requireAuthor(caller, dose.authorAccountId)
        appliedDoses.softDelete(doseId, clock.now())
    }

    /** RF-VAC-05: sin estado calculado ni avisos (RN-VAC-06). */
    suspend fun createOtherVaccine(caller: Caller, childId: UUID, request: OtherVaccineRequest): OtherVaccineResponse {
        val values = otherVaccineValues(request)
        return tx {
            val access = accessPolicy.require(caller, childId)
            requireAppliedOnInRange(values.appliedOn, birthDateOf(childId))
            otherVaccines.insert(childId, values, caller.accountId, access.authorRole()).toDto()
        }
    }

    suspend fun updateOtherVaccine(caller: Caller, vaccineId: UUID, request: OtherVaccineRequest): OtherVaccineResponse {
        val values = otherVaccineValues(request)
        return tx {
            val vaccine = otherVaccines.findActive(vaccineId)?.takeIf { hasAccess(caller, it.childId) } ?: throw OtherVaccineNotFoundException()
            requireAuthor(caller, vaccine.authorAccountId)
            requireAppliedOnInRange(values.appliedOn, birthDateOf(vaccine.childId))
            otherVaccines.update(vaccineId, values)
            requireNotNull(otherVaccines.findActive(vaccineId)).toDto()
        }
    }

    suspend fun deleteOtherVaccine(caller: Caller, vaccineId: UUID) = tx {
        val vaccine = otherVaccines.findActive(vaccineId)?.takeIf { hasAccess(caller, it.childId) } ?: throw OtherVaccineNotFoundException()
        requireAuthor(caller, vaccine.authorAccountId)
        otherVaccines.softDelete(vaccineId, clock.now())
    }

    /** Las dosis de la versión vigente que corresponden a un nacido el [birthDate] (RN-VAC-10). */
    private fun currentDosesFor(birthDate: LocalDate): List<CalendarDose> {
        val current = calendar.currentVersion(clock.today()) ?: return emptyList()
        return calendar.doses(current.id).map { it.toCalendarDose() }.filter { it.appliesTo(birthDate) }
    }

    private fun birthDateOf(childId: UUID): LocalDate =
        children.findById(childId)?.takeIf { it.deletedAt == null }?.birthDate ?: throw ChildNotFoundException()

    /** Sin acceso al niño, el registro no existe para quien pregunta: 404, no 403 (11 · Backend §4). */
    private fun hasAccess(caller: Caller, childId: UUID) = accessPolicy.accessTo(caller, childId) != ChildAccess.NONE

    /** Autoría: solo quien cargó un registro lo modifica (RNF-OFF-05). */
    private fun requireAuthor(caller: Caller, authorAccountId: UUID) {
        if (authorAccountId != caller.accountId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Solo quien la registró puede modificarla o eliminarla")
        }
    }

    private fun parseAppliedOn(raw: String): LocalDate =
        raw.toLocalDateOrNull() ?: throw InvalidInputException("Fecha de aplicación inválida")

    /** RN-VAC-02. "Hoy" en hora argentina (ADR-26). */
    private fun requireAppliedOnInRange(appliedOn: LocalDate, birthDate: LocalDate) {
        if (appliedOn.isBefore(birthDate)) throw InvalidInputException("La fecha de aplicación no puede ser anterior al nacimiento")
        if (appliedOn.isAfter(clock.today())) throw InvalidInputException("La fecha de aplicación no puede ser posterior a hoy")
    }

    private fun otherVaccineValues(request: OtherVaccineRequest): OtherVaccineValues {
        val name = cleanText(request.name, MAX_OTHER_VACCINE_NAME_LENGTH, "El nombre")
            ?: throw InvalidInputException("Completá el nombre de la vacuna")
        return OtherVaccineValues(
            name = name,
            appliedOn = parseAppliedOn(request.appliedOn),
            lot = cleanText(request.lot, MAX_LOT_LENGTH, "El lote"),
            note = cleanText(request.note, MAX_OTHER_VACCINE_NOTE_LENGTH, "La nota"),
        )
    }

    /** Sin espacios en los extremos; un texto vacío se guarda como nulo (SUP-24). */
    private fun cleanText(value: String?, maxLength: Int, label: String): String? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (text.length > maxLength) throw InvalidInputException("$label tiene un máximo de $maxLength caracteres")
        return text
    }

    private fun alreadyApplied() =
        BusinessRuleException("RN-VAC-09", "Esa dosis ya está registrada. Podés verla o editarla desde el calendario")

    /**
     * Si dos tutores registran la misma dosis a la vez, el índice único rechaza la segunda. La
     * transacción se reintenta y la verificación de [AppliedDoseRepository.activeCodes] la
     * encuentra; si aun así llega la violación, responde lo mismo que esa verificación.
     */
    private suspend fun <T> translatingDuplicate(block: suspend () -> T): T = try {
        block()
    } catch (e: ExposedSQLException) {
        if (e.sqlState == UNIQUE_VIOLATION) throw alreadyApplied() else throw e
    }

    private fun ChildAccess.authorRole() =
        if (this == ChildAccess.CURRENT_PEDIATRICIAN) AuthorRole.PEDIATRICIAN else AuthorRole.GUARDIAN

    private fun AuthorRole.toDto() = when (this) {
        AuthorRole.GUARDIAN -> AuthorRoleDto.GUARDIAN
        AuthorRole.PEDIATRICIAN -> AuthorRoleDto.PEDIATRICIAN
    }

    private fun CalendarDoseRow.toDto() = CalendarDoseDto(
        doseCode = doseCode,
        vaccineName = vaccineName,
        prevents = prevents,
        source = source,
        doseLabel = doseLabel,
        ageMonths = ageMonths,
        leadDays = leadDays,
        toleranceDays = toleranceDays,
        bornFrom = bornFrom?.toString(),
        bornUntil = bornUntil?.toString(),
        sortOrder = sortOrder,
    )

    private fun CalendarDoseRow.toCalendarDose() = CalendarDose(
        doseCode = doseCode,
        vaccineName = vaccineName,
        doseLabel = doseLabel,
        ageMonths = ageMonths,
        leadDays = leadDays,
        toleranceDays = toleranceDays,
        sortOrder = sortOrder,
        bornFrom = bornFrom,
        bornUntil = bornUntil,
    )

    private fun AppliedDose.toDto() = AppliedDoseResponse(
        id = id.toString(),
        childId = childId.toString(),
        doseCode = doseCode,
        appliedOn = appliedOn?.toString(),
        lot = lot,
        place = place,
        authorAccountId = authorAccountId.toString(),
        authorRole = authorRole.toDto(),
    )

    private fun OtherVaccine.toDto() = OtherVaccineResponse(
        id = id.toString(),
        childId = childId.toString(),
        name = name,
        appliedOn = appliedOn.toString(),
        lot = lot,
        note = note,
        authorAccountId = authorAccountId.toString(),
        authorRole = authorRole.toDto(),
    )
}
