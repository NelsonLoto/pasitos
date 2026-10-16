package com.pasitos.backend.child

import com.pasitos.backend.guardianship.Guardianships
import java.time.LocalDate
import java.util.UUID
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

data class Child(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val sex: Sex,
    val birthDate: LocalDate,
) {
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

/** Todas las funciones asumen una transacción abierta. */
class ChildRepository {

    /** Sin [gestationalWeeks] el niño queda con "No lo sé" (RN-HIJ-03). */
    fun insert(
        firstName: String,
        lastName: String,
        sex: Sex,
        birthDate: LocalDate,
        gestationalWeeks: Int?,
        createdBy: UUID,
    ): Child {
        val id = UUID.randomUUID()
        Children.insert {
            it[Children.id] = id
            it[Children.firstName] = firstName
            it[Children.lastName] = lastName
            it[Children.sex] = sex
            it[Children.birthDate] = birthDate
            it[Children.gestationalWeeks] = gestationalWeeks
            it[gestationalWeeksUnknown] = gestationalWeeks == null
            it[createdByAccountId] = createdBy
        }
        return Child(id, firstName, lastName, sex, birthDate)
    }

    /** Niños de los que [accountId] es tutor (principal o secundario), en orden de alta. */
    fun listForGuardian(accountId: UUID): List<Child> =
        guardedBy(accountId, Op.TRUE)
            .orderBy(Children.createdAt to SortOrder.ASC)
            .map { it.toChild() }

    /** El niño, solo si [accountId] es su tutor. Si no, null: para quien pregunta, no existe. */
    fun findForGuardian(childId: UUID, accountId: UUID): Child? =
        guardedBy(accountId, Children.id eq childId).firstOrNull()?.toChild()

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
    )
}
