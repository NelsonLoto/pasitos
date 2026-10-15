package com.pasitos.app.data.report

import com.pasitos.app.data.report.model.ReportSection

/**
 * Exportación de reporte PDF (RF15). Mismo patrón mock/local que el resto — todavía no hay
 * generación real de PDF ni de compartir (eso es integración externa, Etapa 4 del plan de
 * trabajo). El día que exista, esto pasa a devolver la ruta/URI del archivo generado.
 */
interface ReportRepository {
    suspend fun generateReport(childId: String, sections: Set<ReportSection>): Result<Unit>
}
