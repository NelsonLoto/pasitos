package com.pasitos.backend.sync

/**
 * Entidades enchufadas al motor de sincronización (11 · Backend §2). En I0 no hay ninguna real
 * registrada: `Dependencies.kt` crea un `SyncRegistry` vacío. I1 registra `children`,
 * `measurements`, etc. a medida que cada dominio se adapta a sincronización (ADR-28). Las
 * pruebas de este dominio registran su propia entidad de prueba, sin tocar este registro de
 * producción.
 */
class SyncRegistry {
    private val entities = mutableMapOf<String, SyncEntity>()

    fun register(entity: SyncEntity) {
        entities[entity.name] = entity
    }

    fun get(name: String): SyncEntity? = entities[name]

    val all: Collection<SyncEntity> get() = entities.values
}
