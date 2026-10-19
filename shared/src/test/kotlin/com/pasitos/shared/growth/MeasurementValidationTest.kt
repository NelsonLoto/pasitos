package com.pasitos.shared.growth

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MeasurementValidationTest {

    @Test
    fun `rango absoluto de entrada RN-CRE-11`() {
        assertTrue(MeasurementValidation.isWeightInRange(6.4))
        assertFalse(MeasurementValidation.isWeightInRange(0.0))
        assertFalse(MeasurementValidation.isWeightInRange(-1.0))
        assertFalse(MeasurementValidation.isWeightInRange(150.1))
        assertFalse(MeasurementValidation.isWeightInRange(Double.NaN))

        assertTrue(MeasurementValidation.isLengthInRange(62.5))
        assertFalse(MeasurementValidation.isLengthInRange(19.9))
        assertFalse(MeasurementValidation.isLengthInRange(200.1))

        assertTrue(MeasurementValidation.isHeadCircumferenceInRange(40.8))
        assertFalse(MeasurementValidation.isHeadCircumferenceInRange(19.9))
        assertFalse(MeasurementValidation.isHeadCircumferenceInRange(70.1))
    }

    @Test
    fun `RN-CRE-03 desde los 60 meses solo se admite de pie`() {
        val birth = LocalDate.parse("2021-10-16")

        // 59 meses: todavía admite acostado.
        assertTrue(MeasurementValidation.isLengthPositionValid(birth, LocalDate.parse("2026-09-16"), standing = false))
        // 60 meses: ya no.
        assertFalse(MeasurementValidation.isLengthPositionValid(birth, LocalDate.parse("2026-10-16"), standing = false))
        assertTrue(MeasurementValidation.isLengthPositionValid(birth, LocalDate.parse("2026-10-16"), standing = true))
    }

    @Test
    fun `RF-CRE-10 la forma preseleccionada cambia a los 24 meses`() {
        val birth = LocalDate.parse("2024-10-16")

        assertEquals(false, MeasurementValidation.defaultStanding(birth, LocalDate.parse("2026-10-15")))
        assertEquals(true, MeasurementValidation.defaultStanding(birth, LocalDate.parse("2026-10-16")))
    }
}
