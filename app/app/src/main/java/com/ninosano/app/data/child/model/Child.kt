package com.ninosano.app.data.child.model

import java.time.LocalDate

enum class ChildSex { Female, Male }

data class Child(
    val id: String,
    val name: String,
    val birthDate: LocalDate,
    val sex: ChildSex,
)
