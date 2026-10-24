package com.pasitos.app.feature.measurement

import androidx.lifecycle.SavedStateHandle
import com.pasitos.app.core.sync.SyncState
import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.child.FakeChildRepository
import com.pasitos.app.data.child.SignedInAuthRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.measurement.FakeMeasurementRepository
import com.pasitos.app.data.measurement.model.LengthPosition
import com.pasitos.app.data.measurement.model.Measurement
import com.pasitos.app.data.measurement.model.MeasurementInput
import com.pasitos.shared.api.AuthorRole
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Carga, edición y lectura de una medición (RF-CRE-01, 02 y 10). Hoy es el 19/10/2026. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MeasurementFormViewModelTest {

    private val today = LocalDate.of(2026, 10, 19)

    /** 1 año: se mide acostado. */
    private val baby = Child(ChildTestData.SOFIA, "Sofía", "Pérez", ChildSex.Female, LocalDate.of(2025, 10, 1), 39)

    /** 3 años: se mide de pie, con perímetro cefálico. */
    private val toddler = Child(ChildTestData.TOMAS, "Tomás", "Pérez", ChildSex.Male, LocalDate.of(2023, 6, 1), 40)

    /** 6 años: solo de pie y sin perímetro cefálico. */
    private val schoolChild = Child("child-6", "Lola", "Pérez", ChildSex.Female, LocalDate.of(2020, 5, 1), 40)

    private val children = FakeChildRepository(listOf(baby, toddler, schoolChild))
    private val measurements = FakeMeasurementRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RF-CRE-10 CA1 antes de los 2 años viene seleccionado acostado con su ayuda`() = runTest {
        val state = collectState(form(baby.id))

        assertEquals(today, state().date)
        assertFalse(state().standing)
        assertEquals("Longitud (cm)", state().lengthLabel)
        assertEquals(LYING_HELP, state().lengthHelp)
        assertTrue(state().offersLying)
        assertTrue(state().offersHeadCircumference)
    }

    @Test
    fun `RF-CRE-10 CA2 desde los 2 años viene seleccionado de pie, y la forma elegida se guarda`() = runTest {
        val viewModel = form(toddler.id)
        val state = collectState(viewModel)
        assertTrue(state().standing)
        assertEquals(STANDING_HELP, state().lengthHelp)

        viewModel.onStandingSelected(false)
        viewModel.onLengthChange("96,5")
        viewModel.onSave()

        assertEquals(LengthPosition.LYING, measurements.added.single().second.lengthPosition)
        assertEquals(96.5, measurements.added.single().second.lengthCm)
    }

    @Test
    fun `RF-CRE-10 CA3 y RN-CRE-01 desde los 5 años solo de pie y sin perímetro cefálico`() = runTest {
        val viewModel = form(schoolChild.id)
        val state = collectState(viewModel)

        assertFalse(state().offersLying)
        assertTrue(state().standing)
        assertFalse(state().offersHeadCircumference)

        // Una fecha anterior a los 5 años vuelve a ofrecer las dos formas
        viewModel.onDateSelected(LocalDate.of(2024, 5, 1))
        assertTrue(state().offersLying)
        assertTrue(state().offersHeadCircumference)
    }

    @Test
    fun `RF-CRE-01 guarda sin conexión con los valores cargados y vuelve`() = runTest {
        val viewModel = form(baby.id)
        val done = doneEvents(viewModel)
        collectState(viewModel)

        viewModel.onWeightChange("7,25")
        viewModel.onHeadCircumferenceChange("44")
        viewModel.onSave()

        assertEquals(
            ChildTestData.SOFIA to MeasurementInput(today, weightKg = 7.25, lengthCm = null, lengthPosition = null, headCircumferenceCm = 44.0),
            measurements.added.single(),
        )
        assertEquals(1, done())
    }

    @Test
    fun `pide al menos un valor y rechaza los que están fuera del rango o con demasiados decimales`() = runTest {
        val viewModel = form(baby.id)
        val state = collectState(viewModel)

        viewModel.onSave()
        assertEquals("Cargá al menos un valor: peso, longitud o talla, o perímetro cefálico", state().generalError)

        viewModel.onWeightChange("200")
        viewModel.onLengthChange("66,55")
        viewModel.onHeadCircumferenceChange("abc")
        viewModel.onSave()
        assertEquals("El peso tiene que estar entre 0,1 y 150 kg", state().weightError)
        assertEquals("Usá un solo decimal", state().lengthError)
        assertEquals("Ingresá un número válido", state().headCircumferenceError)
        assertTrue(measurements.added.isEmpty())
    }

    @Test
    fun `RF-CRE-01 CA2 rechaza una fecha anterior al nacimiento`() = runTest {
        val viewModel = form(baby.id)
        val state = collectState(viewModel)

        viewModel.onDateSelected(LocalDate.of(2025, 9, 30))
        viewModel.onWeightChange("7")
        viewModel.onSave()

        assertEquals("La fecha no puede ser anterior al nacimiento", state().dateError)
        assertTrue(measurements.added.isEmpty())
    }

    @Test
    fun `RF-CRE-01 CA4 si ya cargó una medición en esa fecha ofrece editarla`() = runTest {
        measurements.state.value = listOf(measurement(id = "m-1", date = today))
        val viewModel = form(baby.id)
        val state = collectState(viewModel)
        val opened = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.openMeasurement.collect { opened += it } }

        viewModel.onWeightChange("7,3")
        viewModel.onSave()
        assertEquals(DuplicateMeasurementUi("m-1", "19/10/2026"), state().duplicate)
        assertTrue(measurements.added.isEmpty())

        viewModel.onEditDuplicate()
        assertNull(state().duplicate)
        assertEquals(listOf("m-1"), opened)
    }

    @Test
    fun `RF-CRE-02 CA1 la propia se edita con sus valores y se elimina con confirmación`() = runTest {
        measurements.state.value = listOf(measurement(id = "m-1", lengthCm = 66.0, lengthPosition = LengthPosition.STANDING))
        val viewModel = form(baby.id, "m-1")
        val state = collectState(viewModel)
        val done = doneEvents(viewModel)

        assertEquals(MeasurementFormMode.EDIT, state().mode)
        assertEquals("7,2", state().weightKg)
        assertTrue("respeta la forma guardada aunque no sea la sugerida", state().standing)

        viewModel.onWeightChange("7,4")
        viewModel.onSave()
        assertEquals(7.4, measurements.updated.single().second.weightKg)

        viewModel.onDeleteClick()
        assertTrue(state().confirmDelete)
        viewModel.onConfirmDelete()
        assertEquals(listOf("m-1"), measurements.deleted)
        assertEquals(2, done())
        assertFalse("borrarla no la muestra como no encontrada", state().notFound)
    }

    @Test
    fun `RF-CRE-02 CA2 la de otra persona se ve en modo lectura con su autor`() = runTest {
        measurements.state.value = listOf(measurement(id = "m-otro", authorAccountId = "otra-cuenta", lengthCm = 66.0, lengthPosition = LengthPosition.LYING))
        val viewModel = form(baby.id, "m-otro")
        val state = collectState(viewModel)

        assertEquals(MeasurementFormMode.READ_ONLY, state().mode)
        assertEquals("La cargó la familia", state().authorText)
        assertEquals(listOf("Fecha" to "14/10/2026", "Peso" to "7,2 kg", "Longitud" to "66 cm acostado"), state().readOnlyRows)
        viewModel.onSave()
        viewModel.onDeleteClick()
        assertTrue(measurements.updated.isEmpty())
        assertFalse(state().confirmDelete)
    }

    @Test
    fun `un cambio rechazado muestra el motivo y se puede descartar`() = runTest {
        measurements.state.value = listOf(measurement(id = "m-1").copy(syncState = SyncState.ERROR, syncError = "Ya cargaste una medición en esa fecha"))
        val viewModel = form(baby.id, "m-1")
        val state = collectState(viewModel)
        val done = doneEvents(viewModel)

        assertEquals(SyncState.ERROR, state().syncState)
        assertEquals("Ya cargaste una medición en esa fecha", state().syncError)

        viewModel.onDiscardChange()
        assertEquals(listOf("m-1"), measurements.discarded)
        assertEquals(1, done())
    }

    @Test
    fun `RF-CRE-01 CA6 un perfil finalizado no ofrece cargar mediciones`() = runTest {
        children.state.value = listOf(baby.copy(finalized = true))
        val viewModel = form(baby.id)
        val state = collectState(viewModel)

        assertTrue(state().finalized)
        assertFalse(state().canEdit)
        viewModel.onWeightChange("7")
        viewModel.onSave()
        assertTrue(measurements.added.isEmpty())
    }

    private fun measurement(
        id: String,
        date: LocalDate = LocalDate.of(2026, 10, 14),
        authorAccountId: String = ChildTestData.ACCOUNT_ID,
        lengthCm: Double? = null,
        lengthPosition: LengthPosition? = null,
    ) = Measurement(
        id = id,
        childId = baby.id,
        date = date,
        weightKg = 7.2,
        lengthCm = lengthCm,
        lengthPosition = lengthPosition,
        headCircumferenceCm = null,
        authorAccountId = authorAccountId,
        authorRole = AuthorRole.GUARDIAN,
        syncState = SyncState.SYNCED,
    )

    private fun form(childId: String, measurementId: String? = null) = MeasurementFormViewModel(
        SavedStateHandle(listOfNotNull("childId" to childId, measurementId?.let { "measurementId" to it }).toMap()),
        measurements,
        children,
        SignedInAuthRepository(),
        ChildTestData.clock,
    )

    private fun TestScope.collectState(viewModel: MeasurementFormViewModel): () -> MeasurementFormUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private fun TestScope.doneEvents(viewModel: MeasurementFormViewModel): () -> Int {
        var count = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.done.collect { count++ } }
        return { count }
    }
}
