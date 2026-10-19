package com.pasitos.backend.child

import com.pasitos.backend.auth.ConsentRepository
import com.pasitos.backend.common.AccessPolicy
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.Caller
import com.pasitos.backend.common.ChildAccess
import com.pasitos.backend.common.ChildNotFoundException
import com.pasitos.backend.common.InvalidInputException
import com.pasitos.backend.common.toLocalDateOrNull
import com.pasitos.backend.common.tx
import com.pasitos.backend.guardianship.GuardianshipRepository
import com.pasitos.backend.guardianship.GuardianshipRole
import com.pasitos.shared.api.child.AVATAR_KEYS
import com.pasitos.shared.api.child.ChildResponse
import com.pasitos.shared.api.child.ChildStatus as ChildStatusDto
import com.pasitos.shared.api.child.CreateChildRequest
import com.pasitos.shared.api.child.DeletionPreviewResponse
import com.pasitos.shared.api.child.PatchChildRequest
import com.pasitos.shared.api.child.Sex as SexDto
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

/** RF-HIJ-01 a 03, 09 (11 · Backend §4). */
class ChildService(
    private val children: ChildRepository,
    private val guardianships: GuardianshipRepository,
    private val consents: ConsentRepository,
    private val accessPolicy: AccessPolicy,
    private val clock: ArgentinaClock,
    private val declarationVersion: String,
    /** Lo que cada dominio borra al eliminarse el perfil del niño (RN-HIJ-08). */
    private val purgers: List<ChildDataPurger>,
) {

    suspend fun create(caller: Caller, request: CreateChildRequest): ChildResponse {
        val firstName = request.firstName.trim()
        val lastName = request.lastName.trim()
        if (firstName.isBlank() || lastName.isBlank()) throw InvalidInputException("Completá nombre y apellido")
        val birthDate = parseBirthDate(request.birthDate)
        val gestationalWeeks = validateGestationalWeeks(request.gestationalWeeks)
        if (!request.representationDeclared) {
            throw InvalidInputException("Tenés que declarar que sos madre, padre o representante legal de este niño o niña")
        }
        val birthWeightKg = request.birthWeightKg?.toStoredValue(3)
        val birthLengthCm = request.birthLengthCm?.toStoredValue(1)
        val birthHeadCircumferenceCm = request.birthHeadCircumferenceCm?.toStoredValue(1)
        val avatarKey = validateAvatarKey(request.avatarKey)

        return tx {
            val child = children.insert(
                NewChild(
                    firstName = firstName,
                    lastName = lastName,
                    sex = request.sex.toDomain(),
                    birthDate = birthDate,
                    gestationalWeeks = gestationalWeeks,
                    birthWeightKg = birthWeightKg,
                    birthLengthCm = birthLengthCm,
                    birthHeadCircumferenceCm = birthHeadCircumferenceCm,
                    avatarKey = avatarKey,
                ),
                createdBy = caller.accountId,
            )
            // Quien da de alta al niño queda como su tutor principal (RF-HIJ-01).
            guardianships.insert(child.id, caller.accountId, GuardianshipRole.PRINCIPAL)
            consents.insertGuardianDeclaration(caller.accountId, child.id, declarationVersion, clock.now())
            child.toDto()
        }
    }

    suspend fun get(caller: Caller, childId: UUID): ChildResponse = tx {
        accessPolicy.require(caller, childId)
        (children.findById(childId)?.takeIf { it.deletedAt == null } ?: throw ChildNotFoundException()).toDto()
    }

    suspend fun update(caller: Caller, childId: UUID, request: PatchChildRequest): ChildResponse {
        val firstName = request.firstName.trim()
        val lastName = request.lastName.trim()
        if (firstName.isBlank() || lastName.isBlank()) throw InvalidInputException("Completá nombre y apellido")
        val birthDate = parseBirthDate(request.birthDate)
        val gestationalWeeks = validateGestationalWeeks(request.gestationalWeeks)
        val allergies = request.allergies?.trim()?.takeIf { it.isNotBlank() }
        val relevantHistory = request.relevantHistory?.trim()?.takeIf { it.isNotBlank() }
        if ((allergies?.length ?: 0) > MAX_TEXT_LENGTH || (relevantHistory?.length ?: 0) > MAX_TEXT_LENGTH) {
            throw InvalidInputException("Alergias y antecedentes tienen un máximo de $MAX_TEXT_LENGTH caracteres")
        }
        val birthWeightKg = request.birthWeightKg?.toStoredValue(3)
        val birthLengthCm = request.birthLengthCm?.toStoredValue(1)
        val birthHeadCircumferenceCm = request.birthHeadCircumferenceCm?.toStoredValue(1)
        val avatarKey = validateAvatarKey(request.avatarKey)

        return tx {
            accessPolicy.require(caller, childId)
            children.update(
                childId,
                ChildEdits(
                    firstName = firstName,
                    lastName = lastName,
                    sex = request.sex.toDomain(),
                    birthDate = birthDate,
                    gestationalWeeks = gestationalWeeks,
                    birthWeightKg = birthWeightKg,
                    birthLengthCm = birthLengthCm,
                    birthHeadCircumferenceCm = birthHeadCircumferenceCm,
                    allergies = allergies,
                    relevantHistory = relevantHistory,
                    avatarKey = avatarKey,
                ),
            )
            requireNotNull(children.findById(childId)).toDto()
        }
    }

    suspend fun deletionPreview(caller: Caller, childId: UUID): DeletionPreviewResponse = tx {
        accessPolicy.require(caller, childId)
        DeletionPreviewResponse(onlyGuardian = guardianships.listForChild(childId).size == 1)
    }

    /**
     * RF-HIJ-09: si [caller] es el único tutor, se elimina el perfil entero (RN-HIJ-08); si hay
     * otros tutores, [caller] deja de serlo y, si era principal, el rol pasa al tutor secundario
     * más antiguo (CA2). Los datos de cada dominio (vacunas desde I1-B7) los borra su
     * [ChildDataPurger], en la misma transacción.
     */
    suspend fun delete(caller: Caller, childId: UUID) = tx {
        val access = accessPolicy.require(caller, childId)
        val guardians = guardianships.listForChild(childId)
        val now = clock.now()
        if (guardians.size == 1) {
            children.softDelete(childId, now)
            guardianships.softDelete(childId, caller.accountId, now)
            purgers.forEach { it.purge(childId) }
        } else {
            guardianships.softDelete(childId, caller.accountId, now)
            if (access == ChildAccess.PRINCIPAL_GUARDIAN) {
                guardians.firstOrNull { it.accountId != caller.accountId }
                    ?.let { oldestSecondary -> guardianships.promoteToPrincipal(childId, oldestSecondary.accountId) }
            }
        }
    }

    private fun parseBirthDate(raw: String): LocalDate {
        val birthDate = raw.toLocalDateOrNull() ?: throw InvalidInputException("Fecha de nacimiento inválida")
        val today = clock.today()
        // RN-HIJ-01: no futura y menos de 15 años.
        if (birthDate.isAfter(today)) throw InvalidInputException("La fecha de nacimiento no puede ser futura")
        if (!birthDate.isAfter(today.minusYears(15))) {
            throw InvalidInputException("El niño o niña tiene que tener menos de 15 años")
        }
        return birthDate
    }

    /** RN-HIJ-03: "No lo sé" (null) o un entero entre 22 y 42. */
    private fun validateGestationalWeeks(weeks: Int?): Int? {
        if (weeks != null && weeks !in 22..42) {
            throw InvalidInputException("Las semanas de gestación tienen que estar entre 22 y 42")
        }
        return weeks
    }

    /** RN-HIJ-11: sin avatar (null) o uno de los predefinidos. */
    private fun validateAvatarKey(key: String?): String? {
        if (key != null && key !in AVATAR_KEYS) throw InvalidInputException("Ese avatar no existe")
        return key
    }

    private fun Double.toStoredValue(scale: Int): BigDecimal {
        if (!isFinite() || this <= 0) throw InvalidInputException("Ese valor no es válido")
        return BigDecimal.valueOf(this).setScale(scale, RoundingMode.HALF_UP)
    }

    private fun SexDto.toDomain() = when (this) {
        SexDto.FEMALE -> Sex.FEMALE
        SexDto.MALE -> Sex.MALE
    }

    private fun Sex.toDto() = when (this) {
        Sex.FEMALE -> SexDto.FEMALE
        Sex.MALE -> SexDto.MALE
    }

    private fun ChildStatus.toDto() = when (this) {
        ChildStatus.ACTIVE -> ChildStatusDto.ACTIVE
        ChildStatus.FINALIZED -> ChildStatusDto.FINALIZED
    }

    private fun Child.toDto() = ChildResponse(
        id = id.toString(),
        firstName = firstName,
        lastName = lastName,
        sex = sex.toDto(),
        birthDate = birthDate.toString(),
        gestationalWeeks = gestationalWeeks,
        gestationalWeeksUnknown = gestationalWeeksUnknown,
        birthWeightKg = birthWeightKg?.toDouble(),
        birthLengthCm = birthLengthCm?.toDouble(),
        birthHeadCircumferenceCm = birthHeadCircumferenceCm?.toDouble(),
        allergies = allergies,
        relevantHistory = relevantHistory,
        avatarKey = avatarKey,
        status = status.toDto(),
    )

    companion object {
        /** RN-HIJ-10. */
        const val MAX_TEXT_LENGTH = 280
    }
}
