package com.pasitos.shared.api.child

import kotlinx.serialization.Serializable

// Contratos de /api/v1/children (RF-HIJ-01 a 03, 09; 12 · API).

/** RN-HIJ-02: femenino o masculino, porque las tablas de crecimiento se separan por sexo. */
@Serializable
enum class Sex { FEMALE, MALE }

/**
 * Claves de los avatares predefinidos, en el orden en que la app los ofrece (RN-HIJ-11: no se suben
 * fotos). La app dibuja cada clave y el backend rechaza cualquier otra. Una clave no cambia aunque
 * cambie su dibujo.
 */
val AVATAR_KEYS: List<String> = listOf("rabbit", "bird", "bee", "paw", "flower", "tree", "sun", "star")

/** RN-HIJ-09: un perfil finalizado no admite cargas ni modificaciones (desde I6). */
@Serializable
enum class ChildStatus { ACTIVE, FINALIZED }

/** `POST /children` (RF-HIJ-01). Sin [gestationalWeeks] el niño queda con "No lo sé" (RN-HIJ-03). */
@Serializable
data class CreateChildRequest(
    val firstName: String,
    val lastName: String,
    val sex: Sex,
    /** `"2026-06-15"` (`LocalDate`). */
    val birthDate: String,
    val gestationalWeeks: Int?,
    val birthWeightKg: Double? = null,
    val birthLengthCm: Double? = null,
    val birthHeadCircumferenceCm: Double? = null,
    /** Una de [AVATAR_KEYS], o nulo sin avatar. */
    val avatarKey: String? = null,
    /** "Soy madre, padre o representante legal de este niño o niña" (RF-HIJ-01). */
    val representationDeclared: Boolean,
)

/** `PATCH /children/{id}` (RF-HIJ-02, RF-HIJ-03). Reemplaza todos los campos editables. */
@Serializable
data class PatchChildRequest(
    val firstName: String,
    val lastName: String,
    val sex: Sex,
    val birthDate: String,
    val gestationalWeeks: Int?,
    val birthWeightKg: Double? = null,
    val birthLengthCm: Double? = null,
    val birthHeadCircumferenceCm: Double? = null,
    /** Máximo 280 caracteres (RN-HIJ-10). */
    val allergies: String? = null,
    val relevantHistory: String? = null,
    /** Una de [AVATAR_KEYS], o nulo sin avatar. */
    val avatarKey: String? = null,
)

@Serializable
data class ChildResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val sex: Sex,
    val birthDate: String,
    val gestationalWeeks: Int?,
    val gestationalWeeksUnknown: Boolean,
    val birthWeightKg: Double?,
    val birthLengthCm: Double?,
    val birthHeadCircumferenceCm: Double?,
    val allergies: String?,
    val relevantHistory: String?,
    val avatarKey: String?,
    val status: ChildStatus,
)

/** `GET /children/{id}/deletion-preview` (RF-HIJ-09). */
@Serializable
data class DeletionPreviewResponse(
    /** `true`: es el único tutor, se elimina todo el historial (RN-HIJ-08). `false`: solo deja de ser tutor. */
    val onlyGuardian: Boolean,
)
