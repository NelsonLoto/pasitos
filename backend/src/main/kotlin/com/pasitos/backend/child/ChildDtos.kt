package com.pasitos.backend.child

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Contrato heredado que usa la app actual: claves JSON en español y sexo como "M" o "F". Lo
// reemplazan los endpoints de /api/v1 en I1.

@Serializable
data class CreateChildRequest(
    @SerialName("nombre") val name: String,
    @SerialName("fechaNacimiento") val birthDate: String,
    @SerialName("sexo") val sex: String,
)

@Serializable
data class ChildResponse(
    val id: String,
    @SerialName("nombre") val name: String,
    @SerialName("fechaNacimiento") val birthDate: String,
    @SerialName("sexo") val sex: String,
)

internal fun Child.toResponse() = ChildResponse(
    id = id.toString(),
    name = fullName,
    birthDate = birthDate.toString(),
    sex = when (sex) {
        Sex.FEMALE -> "F"
        Sex.MALE -> "M"
    },
)

internal fun sexFromLegacyCode(code: String): Sex? = when (code) {
    "F" -> Sex.FEMALE
    "M" -> Sex.MALE
    else -> null
}
