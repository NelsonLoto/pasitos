package com.pasitos.backend.medicion

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Mapea la tabla "mediciones" (V1, RF04). Guarda los valores crudos cargados por el
 * tutor y los percentiles ya calculados (RF06/RN02) al momento de guardar -no se
 * recalculan en cada consulta, por el requisito de performance de la curva (RNF06)-.
 */
object Mediciones : Table("mediciones") {
    val id = integer("id").autoIncrement()
    val personaId = uuid("persona_id")
    val fechaMedicion = date("fecha_medicion")
    val pesoKg = decimal("peso_kg", 6, 3).nullable()
    val tallaCm = decimal("talla_cm", 6, 3).nullable()
    val perimetroCefalicoCm = decimal("perimetro_cefalico_cm", 6, 3).nullable()
    val percentilPeso = decimal("percentil_peso", 5, 2).nullable()
    val percentilTalla = decimal("percentil_talla", 5, 2).nullable()
    val percentilPerimetroCefalico = decimal("percentil_perimetro_cefalico", 5, 2).nullable()
    val fueraDeRango = bool("fuera_de_rango")
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
