package com.pasitos.app.feature.measurementhistory

import androidx.lifecycle.SavedStateHandle
import com.pasitos.app.core.sync.SyncState
import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.child.FakeChildRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.measurement.FakeMeasurementRepository
import com.pasitos.app.data.measurement.model.LengthPosition
import com.pasitos.app.data.measurement.model.Measurement
import com.pasitos.shared.api.AuthorRole
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Historial de mediciones (RF-CRE-03). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MeasurementHistoryViewModelTest {

    /** Prematura de 32 semanas: su fecha de término es el 10/06/2026. */
    private val preterm = Child(
        id = ChildTestData.SOFIA,
        firstName = "Sofía",
        lastName = "Pérez",
        sex = ChildSex.Female,
        birthDate = LocalDate.of(2026, 4, 15),
        gestationalWeeks = 32,
        birthWeightKg = 1.8,
        birthLengthCm = 42.0,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RF-CRE-03 CA1 y CA2 de la más reciente a la más antigua, con edad, edad corregida, valores, autor y marcas`() = runTest {
        val measurements = FakeMeasurementRepository(
            listOf(
                measurement("m-1", LocalDate.of(2026, 8, 15), weightKg = 3.9, syncState = SyncState.SYNCED),
                measurement("m-2", LocalDate.of(2026, 10, 15), weightKg = 5.25, lengthCm = 55.5, headCircumferenceCm = 38.0, syncState = SyncState.PENDING),
                measurement("m-3", LocalDate.of(2026, 9, 15), lengthCm = 52.0, syncState = SyncState.ERROR, authorRole = AuthorRole.PEDIATRICIAN),
            ),
        )
        val viewModel = MeasurementHistoryViewModel(SavedStateHandle(mapOf("childId" to preterm.id)), FakeChildRepository(listOf(preterm)), measurements)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val rows = viewModel.uiState.value.rows
        assertEquals(listOf("m-2", "m-3", "m-1", null), rows.map { it.measurementId })
        assertEquals("15/10/2026", rows[0].dateText)
        assertEquals("6 meses · 4 meses y 5 días corregidos", rows[0].ageText)
        assertEquals("5,25 kg", rows[0].primaryValue)
        assertEquals("55,5 cm acostado · PC 38 cm", rows[0].secondaryValues)
        assertEquals("Familia", rows[0].authorText)
        assertEquals(SyncState.PENDING, rows[0].syncState)
        assertEquals("52 cm acostado", rows[1].primaryValue)
        assertEquals("Pediatra", rows[1].authorText)
        assertEquals(SyncState.ERROR, rows[1].syncState)
    }

    @Test
    fun `RN-CRE-14 las medidas al nacer son el primer punto, y un perfil finalizado no ofrece cargar`() = runTest {
        val children = FakeChildRepository(listOf(preterm.copy(finalized = true)))
        val viewModel = MeasurementHistoryViewModel(SavedStateHandle(mapOf("childId" to preterm.id)), children, FakeMeasurementRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val birth = viewModel.uiState.value.rows.single()
        assertEquals("Al nacer", birth.ageText)
        assertEquals("15/04/2026", birth.dateText)
        assertEquals("1,8 kg", birth.primaryValue)
        assertEquals("42 cm", birth.secondaryValues)
        assertFalse(viewModel.uiState.value.canAdd)
    }

    private fun measurement(
        id: String,
        date: LocalDate,
        weightKg: Double? = null,
        lengthCm: Double? = null,
        headCircumferenceCm: Double? = null,
        syncState: SyncState,
        authorRole: AuthorRole = AuthorRole.GUARDIAN,
    ) = Measurement(
        id = id,
        childId = preterm.id,
        date = date,
        weightKg = weightKg,
        lengthCm = lengthCm,
        lengthPosition = lengthCm?.let { LengthPosition.LYING },
        headCircumferenceCm = headCircumferenceCm,
        authorAccountId = ChildTestData.ACCOUNT_ID,
        authorRole = authorRole,
        syncState = syncState,
    )
}
