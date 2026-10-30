package com.pasitos.backend.child

import com.pasitos.backend.common.toUtc
import com.pasitos.backend.guardianship.Guardianships
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

data class Child(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val sex: Sex,
    val birthDate: LocalDate,
    val gestationalWeeks: Int?,
    val gestationalWeeksUnknown: Boolean,
    val birthWeightKg: BigDecimal?,
    val birthLengthCm: BigDecimal?,
    val birthHeadCircumferenceCm: BigDecimal?,
    val allergies: String?,
    val relevantHistory: String?,
    val avatarKey: String?,
    val status: ChildStatus,
    val syncVersion: Long,
    val deletedAt: Instant?,
) {
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

data class NewChild(
    val firstName: String,
    val lastName: String,
    val sex: Sex,
    val birthDate: LocalDate,
    val gestationalWeeks: Int?,
    val birthWeightKg: BigDecimal?,
    val birthLengthCm: BigDecimal?,
    val birthHeadCircumferenceCm: BigDecimal?,
    val avatarKey: String?,
)

data class ChildEdits(
    val firstName: String,
    val lastName: String,
    val sex: Sex,
    val birthDate: LocalDate,
    val gestationalWeeks: Int?,
    val birthWeightKg: BigDecimal?,
    val birthLengthCm: BigDecimal?,
    val birthHeadCircumferenceCm: BigDecimal?,
    val allergies: String?,
    val relevantHistory: String?,
    val avatarKey: String?,
)

/** Todas las funciones asumen una transacción abierta. */
class ChildRepository {

    fun insert(data: NewChild, createdBy: UUID): Child {
        val id = UUID.randomUUID()
        Children.insert {
            it[Children.id] = id
            it[firstName] = data.firstName
            it[lastName] = data.lastName
            it[sex] = data.sex
            it[birthDate] = data.birthDate
            it[gestationalWeeks] = data.gestationalWeeks
            it[gestationalWeeksUnknown] = data.gestationalWeeks == null
            it[birthWeightKg] = data.birthWeightKg
            it[birthLengthCm] = data.birthLengthCm
            it[birthHeadCircumferenceCm] = data.birthHeadCircumferenceCm
            it[avatarKey] = data.avatarKey
            it[createdByAccountId] = createdBy
        }
        return requireNotNull(findById(id))
    }

    fun update(id: UUID, data: ChildEdits) {
        Children.update({ Children.id eq id }) {
            it[firstName] = data.firstName
            it[lastName] = data.lastName
            it[sex] = data.sex
            it[birthDate] = data.birthDate
            it[gestationalWeeks] = data.gestationalWeeks
            it[gestationalWeeksUnknown] = data.gestationalWeeks == null
            it[birthWeightKg] = data.birthWeightKg
            it[birthLengthCm] = data.birthLengthCm
            it[birthHeadCircumferenceCm] = data.birthHeadCircumferenceCm
            it[allergies] = data.allergies
            it[relevantHistory] = data.relevantHistory
            it[avatarKey] = data.avatarKey
        }
    }

    /** Lápida (RN-HIJ-08): el niño no se borra físicamente mientras haya teléfonos que deban enterarse. */
    fun softDelete(id: UUID, at: Instant) {
        Children.update({ Children.id eq id }) {
            it[deletedAt] = at.toUtc()
        }
    }

    fun findById(id: UUID): Child? =
        Children.selectAll().where { Children.id eq id }.firstOrNull()?.toChild()

    /** El niño, solo si [accountId] es su tutor (principal o secundario). Si no, null: no existe para quien pregunta. */
    fun findForGuardian(childId: UUID, accountId: UUID): Child? =
        guardedBy(accountId, Children.id eq childId).firstOrNull()?.toChild()

    /** Niños de los que [accountId] es tutor, en orden de alta (para listar, si hiciera falta). */
    fun listForGuardian(accountId: UUID): List<Child> =
        guardedBy(accountId, Op.TRUE).map { it.toChild() }

    /** Filas vigentes o borradas de [childIds] con `sync_version` mayor a [cursor] (14 · Sincronización). */
    fun changesSince(childIds: Set<UUID>, cursor: Long, limit: Int): List<Child> =
        Children.selectAll()
            .where { (Children.id inList childIds) and (Children.syncVersion greater cursor) }
            .orderBy(Children.syncVersion to SortOrder.ASC)
            .limit(limit)
            .map { it.toChild() }

    private fun guardedBy(accountId: UUID, condition: Op<Boolean>) =
        Children.join(Guardianships, JoinType.INNER, Children.id, Guardianships.childId)
            .selectAll()
            .where {
                condition and
                    (Guardianships.accountId eq accountId) and
                    Guardianships.deletedAt.isNull() and
                    Children.deletedAt.isNull()
            }

    private fun ResultRow.toChild() = Child(
        id = this[Children.id],
        firstName = this[Children.firstName],
        lastName = this[Children.lastName],
        sex = this[Children.sex],
        birthDate = this[Children.birthDate],
        gestationalWeeks = this[Children.gestationalWeeks],
        gestationalWeeksUnknown = this[Children.gestationalWeeksUnknown],
        birthWeightKg = this[Children.birthWeightKg],
        birthLengthCm = this[Children.birthLengthCm],
        birthHeadCircumferenceCm = this[Children.birthHeadCircumferenceCm],
        allergies = this[Children.allergies],
        relevantHistory = this[Children.relevantHistory],
        avatarKey = this[Children.avatarKey],
        status = this[Children.status],
        syncVersion = this[Children.syncVersion],
        deletedAt = this[Children.deletedAt]?.toInstant(),
    )
}
