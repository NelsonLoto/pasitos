package com.pasitos.app.data.child.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ChildTest {
    private val child = Child(id = "child-1", name = "Lucía", birthDate = LocalDate.of(2026, 1, 10), sex = ChildSex.Female)

    @Test
    fun `I0-02 la edad del hijo usa formatAge de shared`() {
        assertEquals("4 meses y 1 día · Niña", child.ageAndSexLabel(LocalDate.of(2026, 5, 11)))
        assertEquals("2 años y 2 meses", child.ageLabel(LocalDate.of(2028, 3, 10)))
    }

    @Test
    fun `I0-02 un nacimiento posterior a hoy muestra 0 dias en lugar de fallar`() {
        assertEquals("0 días", child.ageLabel(LocalDate.of(2026, 1, 9)))
    }
}
