package com.pasitos.shared.api.vaccination

import com.pasitos.shared.api.AuthorRole
import kotlinx.serialization.Serializable

// Contratos de /vaccination-calendar, /applied-doses y /other-vaccines (RF-VAC-01 a 05, 07; 12 · API).

/** Lote de una dosis o de otra vacuna (SUP-24). */
const val MAX_LOT_LENGTH = 40

/** Vacunatorio o lugar de una dosis (SUP-24). */
const val MAX_PLACE_LENGTH = 120

/** Nombre de otra vacuna (SUP-24). */
const val MAX_OTHER_VACCINE_NAME_LENGTH = 120

/** Nota de otra vacuna (SUP-24). */
const val MAX_OTHER_VACCINE_NOTE_LENGTH = 280

/** Códigos por pedido de carga retroactiva: el calendario tiene 27 dosis. */
const val MAX_RETROACTIVE_DOSES = 50

/**
 * `GET /vaccination-calendar` (RF-VAC-01, RF-VAC-07). Sin calendario publicado, [effectiveFrom]
 * es nulo y las listas vienen vacías.
 */
@Serializable
data class VaccinationCalendarResponse(
    /** Vigencia de la versión, `"2026-01-01"`. */
    val effectiveFrom: String?,
    /** Todas las filas de la versión vigente, también las de otras cohortes (RN-VAC-10). */
    val doses: List<CalendarDoseDto>,
    /** Dosis de versiones anteriores que ya no están en la vigente, para mostrar las aplicadas (RN-VAC-03). */
    val formerDoses: List<FormerDoseDto>,
)

/** Una fila de la versión vigente. Se convierte a `CalendarDose` de `shared/vaccination`. */
@Serializable
data class CalendarDoseDto(
    val doseCode: String,
    val vaccineName: String,
    /** Qué previene (RF-VAC-07). */
    val prevents: String?,
    /** Fuente de la información (RF-VAC-07). */
    val source: String?,
    val doseLabel: String,
    val ageMonths: Int,
    val leadDays: Int,
    val toleranceDays: Int,
    /** `"2024-07-01"`, o nulo sin límite (RN-VAC-10). */
    val bornFrom: String?,
    val bornUntil: String?,
    val sortOrder: Int,
)

/** Nombre de una dosis que ya no está en la versión vigente. */
@Serializable
data class FormerDoseDto(
    val doseCode: String,
    val vaccineName: String,
    val doseLabel: String,
)

/** `POST /children/{id}/applied-doses` (RF-VAC-02). */
@Serializable
data class CreateAppliedDoseRequest(
    val doseCode: String,
    /** `"2026-08-15"`; no anterior al nacimiento ni posterior a hoy (RN-VAC-02). */
    val appliedOn: String,
    val lot: String? = null,
    val place: String? = null,
)

/**
 * `PATCH /applied-doses/{id}` (RF-VAC-03, RF-VAC-04 CA3). Reemplaza fecha, lote y lugar; la dosis
 * no cambia. [appliedOn] solo puede quedar nulo en una dosis de carga retroactiva que todavía no
 * tiene fecha.
 */
@Serializable
data class PatchAppliedDoseRequest(
    val appliedOn: String?,
    val lot: String? = null,
    val place: String? = null,
)

/** `POST /children/{id}/applied-doses/retroactive` (RF-VAC-04): dosis aplicadas con fecha no registrada. */
@Serializable
data class RetroactiveDosesRequest(
    val doseCodes: List<String>,
)

/** Las dosis que creó la carga retroactiva; las que ya estaban aplicadas no se repiten. */
@Serializable
data class RetroactiveDosesResponse(
    val created: List<AppliedDoseResponse>,
)

@Serializable
data class AppliedDoseResponse(
    val id: String,
    val childId: String,
    val doseCode: String,
    /** Nulo: aplicada con fecha no registrada (RN-VAC-04). */
    val appliedOn: String?,
    val lot: String?,
    val place: String?,
    val authorAccountId: String,
    val authorRole: AuthorRole,
)

/** `POST /children/{id}/other-vaccines` y `PATCH /other-vaccines/{id}` (RF-VAC-05). */
@Serializable
data class OtherVaccineRequest(
    val name: String,
    /** No anterior al nacimiento ni posterior a hoy (RN-VAC-02). */
    val appliedOn: String,
    val lot: String? = null,
    val note: String? = null,
)

@Serializable
data class OtherVaccineResponse(
    val id: String,
    val childId: String,
    val name: String,
    val appliedOn: String,
    val lot: String?,
    val note: String?,
    val authorAccountId: String,
    val authorRole: AuthorRole,
)
