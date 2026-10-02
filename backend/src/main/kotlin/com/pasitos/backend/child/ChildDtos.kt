package com.pasitos.backend.child

import kotlinx.serialization.Serializable

@Serializable
data class CreateChildRequest(
    val nombre: String,
    val fechaNacimiento: String,
    val sexo: String,
)

@Serializable
data class ChildResponse(
    val id: String,
    val nombre: String,
    val fechaNacimiento: String,
    val sexo: String,
)
