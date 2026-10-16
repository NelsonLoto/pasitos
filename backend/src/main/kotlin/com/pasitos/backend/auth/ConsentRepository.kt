package com.pasitos.backend.auth

import com.pasitos.backend.common.toUtc
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

/** Todas las funciones asumen una transacción abierta. */
class ConsentRepository {

    fun insertTerms(accountId: UUID, version: String, acceptedAt: Instant) {
        Consents.insert {
            it[id] = UUID.randomUUID()
            it[Consents.accountId] = accountId
            it[type] = ConsentType.TERMS
            it[Consents.version] = version
            it[Consents.acceptedAt] = acceptedAt.toUtc()
        }
    }

    /** Última versión de los términos que aceptó la cuenta, o null si nunca aceptó ninguna. */
    fun latestTermsVersion(accountId: UUID): String? =
        Consents.selectAll()
            .where { (Consents.accountId eq accountId) and (Consents.type eq ConsentType.TERMS) }
            .orderBy(Consents.acceptedAt to SortOrder.DESC)
            .firstOrNull()
            ?.get(Consents.version)
}
