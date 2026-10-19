package com.pasitos.shared.api

import kotlinx.serialization.Serializable

/**
 * Perfil desde el que se cargó un registro (40 · Modelo conceptual, Autoría). Define cómo se
 * muestra el autor: "Familia" o el nombre del profesional.
 */
@Serializable
enum class AuthorRole { GUARDIAN, PEDIATRICIAN }
