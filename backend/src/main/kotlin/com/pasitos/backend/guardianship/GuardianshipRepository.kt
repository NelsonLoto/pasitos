package com.pasitos.backend.guardianship

import java.util.UUID
import org.jetbrains.exposed.sql.insert

/** Todas las funciones asumen una transacción abierta. */
class GuardianshipRepository {

    fun insert(childId: UUID, accountId: UUID, role: GuardianshipRole) {
        Guardianships.insert {
            it[Guardianships.childId] = childId
            it[Guardianships.accountId] = accountId
            it[Guardianships.role] = role
        }
    }
}
