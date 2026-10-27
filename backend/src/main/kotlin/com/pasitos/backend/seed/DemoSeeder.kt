package com.pasitos.backend.seed

import com.pasitos.backend.auth.Accounts
import com.pasitos.backend.auth.ConsentRepository
import com.pasitos.backend.auth.PasswordHasher
import com.pasitos.backend.auth.PasswordPolicy
import com.pasitos.backend.child.Children
import com.pasitos.backend.child.Sex
import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.AuthorRole
import com.pasitos.backend.common.toUtc
import com.pasitos.backend.guardianship.GuardianshipRepository
import com.pasitos.backend.guardianship.GuardianshipRole
import com.pasitos.backend.measurement.MeasurementRepository
import com.pasitos.backend.vaccination.AppliedDoseRepository
import com.pasitos.backend.vaccination.AppliedDoseValues
import com.pasitos.backend.vaccination.CalendarRepository
import com.pasitos.shared.api.auth.AccountMode
import com.pasitos.shared.vaccination.CalendarDose
import com.pasitos.shared.vaccination.doseStatuses
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Carga explícita de T-18; nunca forma parte del arranque del servidor ni de las migraciones. */
class DemoSeeder(
    private val clock: ArgentinaClock,
    private val termsVersion: String,
    private val declarationVersion: String,
) {
    /** true si cargó; false si el conjunto ya existe. No repara ni sobrescribe una demo editada. */
    fun seed(data: DemoData, password: String): Boolean {
        val today = clock.today()
        data.validate(today)
        require(PasswordPolicy.isValid(password) && password.length >= 12 && password.toByteArray(Charsets.UTF_8).size <= 72) {
            "DEMO_PASSWORD debe tener al menos 12 caracteres, una letra y un número, y hasta 72 bytes UTF-8"
        }
        require(termsVersion.isNotBlank() && declarationVersion.isNotBlank()) { "Faltan versiones de los consentimientos" }
        return transaction {
            // Serializa dos cargas concurrentes; el lock se libera también si hay rollback.
            exec("SELECT pg_advisory_xact_lock(70180001)") { }
            val accountIds = data.guardians.associate { it.key to data.id("account", it.key) }
            val childIds = data.children.associate { it.key to data.id("child", it.key) }
            val existingAccounts = Accounts.selectAll().where { Accounts.id inList accountIds.values.toList() }.count()
            val existingChildren = Children.selectAll().where { Children.id inList childIds.values.toList() }.count()
            if (existingAccounts > 0 || existingChildren > 0) {
                require(existingAccounts == accountIds.size.toLong() && existingChildren == childIds.size.toLong()) {
                    "Conjunto demo incompleto: no se modificó nada. Usá otra base de demostración"
                }
                return@transaction false
            }
            require(Accounts.selectAll().where { Accounts.email inList data.guardians.map { it.email } }.empty()) {
                "Un email reservado de demo ya pertenece a otra cuenta; no se modificó nada"
            }
            val calendars = CalendarRepository()
            val version = requireNotNull(calendars.currentVersion(today)) { "Falta un calendario vigente: ejecutá seedReferenceData primero" }
            val calendar = calendars.doses(version.id).map {
                CalendarDose(it.doseCode, it.vaccineName, it.doseLabel, it.ageMonths, it.leadDays, it.toleranceDays, it.sortOrder, it.bornFrom, it.bornUntil)
            }
            require(calendar.isNotEmpty()) { "El calendario vigente no tiene dosis" }
            val now = clock.now()
            val hash = PasswordHasher.hash(password)
            val consents = ConsentRepository()
            data.guardians.forEach { guardian ->
                // UUID estable solo para reconocer el conjunto: no se cambia el contrato de los repositorios.
                Accounts.insert {
                    it[id] = accountIds.getValue(guardian.key)
                    it[email] = guardian.email
                    it[firstName] = guardian.firstName
                    it[lastName] = "Demo T18"
                    it[passwordHash] = hash
                    it[emailVerifiedAt] = now.toUtc()
                    it[isGuardian] = true
                    it[lastMode] = AccountMode.GUARDIAN.name
                }
                consents.insertTerms(accountIds.getValue(guardian.key), termsVersion, now)
            }
            val guardianships = GuardianshipRepository()
            val measurements = MeasurementRepository()
            val appliedDoses = AppliedDoseRepository()
            data.children.forEach { child ->
                val childId = childIds.getValue(child.key)
                val principal = accountIds.getValue(child.guardians.first())
                val birth = child.birthDate(today)
                val birthValues = child.birth.edits(birth, birth)
                Children.insert {
                    it[id] = childId
                    it[firstName] = child.firstName
                    it[lastName] = "Demo T18"
                    it[sex] = Sex.valueOf(child.sex)
                    it[birthDate] = birth
                    it[gestationalWeeks] = child.gestationalWeeks
                    it[gestationalWeeksUnknown] = false
                    it[birthWeightKg] = birthValues.weightKg
                    it[birthLengthCm] = birthValues.lengthCm
                    it[birthHeadCircumferenceCm] = birthValues.headCircumferenceCm
                    it[createdByAccountId] = principal
                }
                child.guardians.forEachIndexed { index, key ->
                    val accountId = accountIds.getValue(key)
                    guardianships.insert(childId, accountId, if (index == 0) GuardianshipRole.PRINCIPAL else GuardianshipRole.SECONDARY)
                    consents.insertGuardianDeclaration(accountId, childId, declarationVersion, now)
                }
                child.measurements.forEachIndexed { index, point ->
                    measurements.insert(
                        data.id("measurement", "${child.key}:$index"), childId, accountIds.getValue(point.author),
                        AuthorRole.GUARDIAN, point.values.edits(birth, point.date(birth)),
                    )
                }
                // Usa cohortes y edades del calendario vigente; no reproduce un esquema médico propio.
                val due = doseStatuses(birth, calendar, emptyList(), today).doses.filter { !it.indicatedDate.isAfter(today) }
                val selected = if (child.vaccination == DemoVaccination.OVERDUE) due.take(2) else due
                selected.forEachIndexed { index, dose ->
                    val date = if (child.vaccination == DemoVaccination.RETROACTIVE && index == 0) null else dose.indicatedDate
                    appliedDoses.insert(
                        childId, dose.dose.doseCode, AppliedDoseValues(date, "DEMO", "Centro ficticio DEMO"), principal, AuthorRole.GUARDIAN,
                    )
                }
            }
            true
        }
    }
}
