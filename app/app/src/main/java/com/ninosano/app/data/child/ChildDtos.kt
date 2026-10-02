package com.ninosano.app.data.child

import kotlinx.serialization.Serializable

@Serializable
internal data class CreateChildRequestDto(
    val nombre: String,
    val fechaNacimiento: String,
    val sexo: String,
)

@Serializable
internal data class ChildResponseDto(
    val id: String,
    val nombre: String,
    val fechaNacimiento: String,
    val sexo: String,
)
