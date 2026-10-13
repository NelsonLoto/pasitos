package com.pasitos.backend.medicion

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll

/**
 * RF06/RN02 - calcula en qué percentil cae un valor medido, interpolando entre los
 * puntos de referencia SAP cargados en "estandar_percentiles" para ese sexo/edad/tipo
 * de medida. Debe llamarse desde dentro de una transacción Exposed ya abierta.
 *
 * Si todavía no hay datos cargados para esa combinación (la tabla está vacía al momento
 * de este commit), devuelve null -no rompe nada, simplemente no calcula- y queda listo
 * para funcionar solo el día que se carguen las tablas reales, sin tocar código.
 */
fun calcularPercentil(sexo: String?, edadMeses: Int?, tipoMedida: String, valor: Double?): Double? {
    if (sexo == null || edadMeses == null || valor == null) return null

    val puntos = EstandarPercentiles.selectAll()
        .where {
            (EstandarPercentiles.sexo eq sexo) and
                (EstandarPercentiles.edadMeses eq edadMeses) and
                (EstandarPercentiles.tipoMedida eq tipoMedida)
        }
        .orderBy(EstandarPercentiles.percentil to SortOrder.ASC)
        .map { it[EstandarPercentiles.percentil].toDouble() to it[EstandarPercentiles.valor].toDouble() }

    if (puntos.isEmpty()) return null

    val primero = puntos.first()
    val ultimo = puntos.last()

    return when {
        valor <= primero.second -> primero.first
        valor >= ultimo.second -> ultimo.first
        else -> {
            val (inferior, superior) = puntos.zipWithNext().first { (a, b) -> valor in a.second..b.second }
            val (p1, v1) = inferior
            val (p2, v2) = superior
            if (v2 == v1) p1 else p1 + (valor - v1) / (v2 - v1) * (p2 - p1)
        }
    }
}
