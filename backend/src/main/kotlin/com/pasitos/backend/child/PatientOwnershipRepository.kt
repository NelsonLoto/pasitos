package com.pasitos.backend.child

import java.util.UUID
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

/** Pacientes dados de alta por un profesional, sin relación de representación legal. */
object PediatricianPatients : Table("pediatrician_patients") {
    val childId = uuid("child_id")
    val pediatricianAccountId = uuid("pediatrician_account_id")
    override val primaryKey = PrimaryKey(childId)
}

/** Las operaciones asumen una transacción Exposed abierta. */
class PatientOwnershipRepository {
    fun insert(childId: UUID, pediatricianAccountId: UUID) {
        PediatricianPatients.insert {
            it[PediatricianPatients.childId] = childId
            it[PediatricianPatients.pediatricianAccountId] = pediatricianAccountId
        }
    }

    fun owns(childId: UUID, pediatricianAccountId: UUID): Boolean =
        PediatricianPatients.selectAll()
            .where { (PediatricianPatients.childId eq childId) and (PediatricianPatients.pediatricianAccountId eq pediatricianAccountId) }
            .any()
}
