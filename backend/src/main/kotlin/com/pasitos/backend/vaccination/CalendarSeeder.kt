package com.pasitos.backend.vaccination

import com.pasitos.backend.common.ArgentinaClock
import com.pasitos.backend.common.tx
import com.pasitos.shared.vaccination.calendarProblems
import java.io.File
import java.time.LocalDate

private val CALENDAR_FILE = Regex("""calendario-nacional-(\d{4}-\d{2}-\d{2})\.csv""")

/**
 * Carga las versiones del Calendario Nacional desde `backend/seed/calendar/` (13 · Base de datos,
 * Datos iniciales). La usan la tarea `seedReferenceData` en `local` y las pruebas; en el piloto
 * el calendario lo gestiona el panel (RF-ADM-06).
 */
class CalendarSeeder(
    private val calendar: CalendarRepository,
    private val clock: ArgentinaClock,
) {

    /**
     * Carga cada archivo `calendario-nacional-AAAA-MM-DD.csv` de [directory] cuya fecha de
     * vigencia todavía no esté en la base, y devuelve las fechas cargadas. Una versión ya cargada
     * se saltea: un archivo cargado no se edita, porque las versiones anteriores explican las dosis
     * aplicadas.
     */
    suspend fun seed(directory: File): List<LocalDate> {
        val files = directory.listFiles().orEmpty()
            .mapNotNull { file -> CALENDAR_FILE.matchEntire(file.name)?.let { LocalDate.parse(it.groupValues[1]) to file } }
            .sortedBy { (effectiveFrom, _) -> effectiveFrom }
        return files.mapNotNull { (effectiveFrom, file) -> seedFile(effectiveFrom, file.readText(Charsets.UTF_8), file.name) }
    }

    /**
     * Una versión en una sola transacción. Si una fila no se puede leer o `calendarProblems()`
     * encuentra un problema, lanza [IllegalArgumentException] y no carga nada de ese archivo: nunca
     * queda publicado un calendario a medias.
     */
    suspend fun seedFile(effectiveFrom: LocalDate, csv: String, fileName: String): LocalDate? {
        val rows = try {
            parseCalendarCsv(csv)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("$fileName: ${e.message}", e)
        }
        val problems = calendarProblems(rows.map { it.toCalendarDose() })
        require(problems.isEmpty()) { "$fileName: ${problems.joinToString("; ")}" }

        return tx {
            if (calendar.versionExists(effectiveFrom)) {
                null
            } else {
                calendar.insertPublishedVersion(effectiveFrom, rows, clock.now())
                effectiveFrom
            }
        }
    }
}
