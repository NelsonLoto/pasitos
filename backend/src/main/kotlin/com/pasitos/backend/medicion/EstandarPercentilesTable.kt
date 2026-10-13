package com.pasitos.backend.medicion

import org.jetbrains.exposed.sql.Table

/**
 * Mapea la tabla "estandar_percentiles" (V1, RN02) — tablas de referencia SAP usadas
 * como lookup para calcular el percentil de cada medición. Es contenido de referencia,
 * no de un hijo puntual: hoy está vacía (nadie cargó los datos reales todavía), pero el
 * cálculo en [calcularPercentil] ya queda funcionando para el día que se carguen.
 */
object EstandarPercentiles : Table("estandar_percentiles") {
    val id = integer("id").autoIncrement()
    val sexo = text("sexo")
    val edadMeses = integer("edad_meses")
    val tipoMedida = text("tipo_medida")
    val percentil = decimal("percentil", 5, 2)
    val valor = decimal("valor", 6, 3)

    override val primaryKey = PrimaryKey(id)
}
