package com.pasitos.app.feature.childonboarding

import androidx.lifecycle.SavedStateHandle
import com.pasitos.app.core.network.NetworkMonitor
import com.pasitos.app.data.child.ChildTestData
import com.pasitos.app.data.child.FakeChildRepository
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.vaccination.FakeVaccinationRepository
import com.pasitos.app.data.vaccination.model.VaccinationCalendar
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Formulario de alta y edición (RF-HIJ-01 a 03). "Hoy" es el 19/10/2026. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChildFormViewModelTest {

    private val online = MutableStateFlow(true)
    private val networkMonitor = object : NetworkMonitor {
        override val isOnline = online
    }
    private val sofia = Child(
        id = ChildTestData.SOFIA,
        firstName = "Sofía",
        lastName = "Pérez",
        sex = ChildSex.Female,
        birthDate = LocalDate.of(2026, 3, 15),
        gestationalWeeks = 38,
    )
    private val repository = FakeChildRepository(listOf(sofia))
    private val vaccinationRepository = FakeVaccinationRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `RF-HIJ-01 CA1 con los datos obligatorios y la declaración guarda el perfil`() = runTest {
        val viewModel = newChild()
        val saved = savedEvents(viewModel)
        viewModel.fillRequired()
        viewModel.onAvatarSelected("bee")

        viewModel.onSubmit()

        val (input, declared) = repository.added.single()
        assertEquals("Lucía", input.firstName)
        assertEquals("Gómez", input.lastName)
        assertEquals(ChildSex.Female, input.sex)
        assertEquals(LocalDate.of(2026, 5, 2), input.birthDate)
        assertEquals(39, input.gestationalWeeks)
        assertEquals("bee", input.avatarKey)
        assertNull(input.allergies)
        assertTrue(declared)
        assertEquals(1, saved())
    }

    @Test
    fun `RF-HIJ-01 exige nombre, apellido, sexo, fecha, semanas y la declaración`() = runTest {
        val viewModel = newChild()

        viewModel.onSubmit()

        val errors = viewModel.uiState.value.errors
        assertNotNull(errors.firstName)
        assertNotNull(errors.lastName)
        assertNotNull(errors.sex)
        assertNotNull(errors.birthDate)
        assertNotNull(errors.gestationalWeeks)
        assertNotNull(errors.representation)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun `RF-HIJ-01 CA3 rechaza una fecha futura o de 15 años o más, con el motivo`() = runTest {
        val viewModel = newChild()
        viewModel.fillRequired()

        viewModel.onBirthDateSelected(LocalDate.of(2026, 10, 20))
        viewModel.onSubmit()
        assertEquals("La fecha de nacimiento no puede ser futura", viewModel.uiState.value.errors.birthDate)

        viewModel.onBirthDateSelected(LocalDate.of(2011, 10, 19))
        viewModel.onSubmit()
        assertEquals("Pasitos acompaña a niños y niñas menores de 15 años", viewModel.uiState.value.errors.birthDate)

        viewModel.onBirthDateSelected(LocalDate.of(2011, 10, 20))
        viewModel.onSubmit()
        assertEquals(1, repository.added.size)
    }

    @Test
    fun `RN-HIJ-03 las semanas de gestación van de 22 a 42`() = runTest {
        val viewModel = newChild()
        viewModel.fillRequired()

        listOf("21", "43").forEach { weeks ->
            viewModel.onGestationalWeeksChange(weeks)
            viewModel.onSubmit()
            assertEquals("Tienen que ser entre 22 y 42 semanas", viewModel.uiState.value.errors.gestationalWeeks)
        }
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun `RF-HIJ-01 CA5 con No lo sé se guarda sin semanas de gestación`() = runTest {
        val viewModel = newChild()
        viewModel.fillRequired()

        viewModel.onGestationalWeeksUnknownChange(true)
        viewModel.onSubmit()

        assertEquals("", viewModel.uiState.value.gestationalWeeks)
        assertNull(repository.added.single().first.gestationalWeeks)
    }

    @Test
    fun `las medidas al nacer aceptan coma o punto y quedan vacías si no se cargan`() = runTest {
        val viewModel = newChild()
        viewModel.fillRequired()

        viewModel.onBirthWeightChange("3,25")
        viewModel.onBirthLengthChange("50.5")
        viewModel.onSubmit()

        val input = repository.added.single().first
        assertEquals(3.25, input.birthWeightKg!!, 0.0)
        assertEquals(50.5, input.birthLengthCm!!, 0.0)
        assertNull(input.birthHeadCircumferenceCm)
    }

    @Test
    fun `una medida que no es un número no se guarda`() = runTest {
        val viewModel = newChild()
        viewModel.fillRequired()

        viewModel.onBirthWeightChange("tres")
        viewModel.onSubmit()

        assertEquals("Ingresá un número válido", viewModel.uiState.value.errors.birthWeightKg)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun `RF-HIJ-01 CA4 una medida poco habitual pide revisarla y se guarda si el tutor confirma`() = runTest {
        val viewModel = newChild()
        viewModel.fillRequired()
        viewModel.onBirthWeightChange("32")

        viewModel.onSubmit()

        assertEquals(listOf("Peso: 32 kg"), viewModel.uiState.value.implausibleMeasurements)
        assertTrue(repository.added.isEmpty())

        viewModel.onConfirmMeasurements()

        assertTrue(viewModel.uiState.value.implausibleMeasurements.isEmpty())
        assertEquals(32.0, repository.added.single().first.birthWeightKg!!, 0.0)
    }

    @Test
    fun `RF-HIJ-01 CA4 si el tutor elige revisar no se guarda`() = runTest {
        val viewModel = newChild()
        viewModel.fillRequired()
        viewModel.onBirthLengthChange("12")
        viewModel.onBirthHeadCircumferenceChange("60")

        viewModel.onSubmit()
        assertEquals(listOf("Longitud: 12 cm", "Perímetro cefálico: 60 cm"), viewModel.uiState.value.implausibleMeasurements)

        viewModel.onReviewMeasurements()

        assertTrue(viewModel.uiState.value.implausibleMeasurements.isEmpty())
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun `sin conexión no intenta guardar`() = runTest {
        val viewModel = newChild()
        viewModel.fillRequired()
        online.value = false

        viewModel.onSubmit()

        assertFalse(viewModel.uiState.value.isOnline)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun `si el servidor rechaza muestra su mensaje`() = runTest {
        repository.saveResult = Result.failure(Exception("El niño o niña tiene que tener menos de 15 años"))
        val viewModel = newChild()
        viewModel.fillRequired()

        viewModel.onSubmit()

        assertEquals("El niño o niña tiene que tener menos de 15 años", viewModel.uiState.value.generalError)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `al editar carga los datos actuales del niño`() = runTest {
        val viewModel = edit()

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals("Sofía", state.firstName)
        assertEquals("38", state.gestationalWeeks)
        assertTrue(state.isEdit)
    }

    @Test
    fun `RF-HIJ-02 CA1 cambiar la fecha, el sexo o las semanas pide confirmar el recálculo`() = runTest {
        val changes = listOf<ChildFormViewModel.() -> Unit>(
            { onBirthDateSelected(LocalDate.of(2026, 3, 1)) },
            { onSexSelected(ChildSex.Male) },
            { onGestationalWeeksUnknownChange(true) },
        )
        changes.forEach { change ->
            val viewModel = edit()
            viewModel.uiState.first { !it.isLoading }

            viewModel.change()
            viewModel.onSubmit()

            assertTrue(viewModel.uiState.value.confirmRecalculation)
            assertTrue(repository.updated.isEmpty())
        }
    }

    @Test
    fun `RF-HIJ-02 CA1 la edición se guarda después de la segunda confirmación`() = runTest {
        val viewModel = edit()
        viewModel.uiState.first { !it.isLoading }
        viewModel.onGestationalWeeksChange("34")
        viewModel.onSubmit()

        viewModel.onConfirmRecalculation()

        assertEquals(ChildTestData.SOFIA to 34, repository.updated.single().let { (id, input) -> id to input.gestationalWeeks })
    }

    @Test
    fun `cambiar solo el nombre o las alergias no pide confirmación`() = runTest {
        val viewModel = edit()
        viewModel.uiState.first { !it.isLoading }

        viewModel.onFirstNameChange("Sofi")
        viewModel.onAllergiesChange("Polen")
        viewModel.onSubmit()

        assertFalse(viewModel.uiState.value.confirmRecalculation)
        val input = repository.updated.single().second
        assertEquals("Sofi", input.firstName)
        assertEquals("Polen", input.allergies)
    }

    @Test
    fun `RF-HIJ-03 CA2 alergias o antecedentes de más de 280 caracteres indican el máximo`() = runTest {
        val viewModel = edit()
        viewModel.uiState.first { !it.isLoading }

        viewModel.onRelevantHistoryChange("a".repeat(281))
        viewModel.onSubmit()

        assertEquals("Máximo 280 caracteres", viewModel.uiState.value.errors.relevantHistory)
        assertTrue(repository.updated.isEmpty())
    }

    @Test
    fun `RF-HIJ-01 CA2 si por su edad ya le corresponden dosis, ofrece la carga retroactiva`() = runTest {
        val viewModel = newChild()
        val saved = lastSaved(viewModel)
        // Nacida el 02/05/2026: ya le correspondían las dosis del nacimiento y de los 2 meses
        viewModel.fillRequired()

        viewModel.onSubmit()

        assertEquals(SavedChild(childId = "new-child", offerPreviousDoses = true), saved())
    }

    @Test
    fun `RF-HIJ-01 CA2 sin dosis anteriores o sin calendario, va directo a Inicio`() = runTest {
        val newborn = newChild()
        val savedNewborn = lastSaved(newborn)
        newborn.fillRequired()
        newborn.onBirthDateSelected(LocalDate.of(2026, 10, 19))
        newborn.onSubmit()

        vaccinationRepository.calendar.value = VaccinationCalendar.EMPTY
        val withoutCalendar = newChild()
        val savedWithoutCalendar = lastSaved(withoutCalendar)
        withoutCalendar.fillRequired()
        withoutCalendar.onSubmit()

        assertEquals(false, savedNewborn()?.offerPreviousDoses)
        assertEquals(false, savedWithoutCalendar()?.offerPreviousDoses)
        assertEquals("Sin calendario, se pide en el momento", 1, vaccinationRepository.refreshCount)
    }

    private fun newChild() =
        ChildFormViewModel(SavedStateHandle(), repository, vaccinationRepository, networkMonitor, ChildTestData.clock)

    private fun edit() =
        ChildFormViewModel(SavedStateHandle(mapOf("childId" to ChildTestData.SOFIA)), repository, vaccinationRepository, networkMonitor, ChildTestData.clock)

    private fun ChildFormViewModel.fillRequired() {
        onFirstNameChange("Lucía")
        onLastNameChange("Gómez")
        onSexSelected(ChildSex.Female)
        onBirthDateSelected(LocalDate.of(2026, 5, 2))
        onGestationalWeeksChange("39")
        onRepresentationDeclaredChange(true)
    }

    /** El último aviso de que se guardó, con si ofrece la carga retroactiva. */
    private fun TestScope.lastSaved(viewModel: ChildFormViewModel): () -> SavedChild? {
        var last: SavedChild? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.saved.collect { last = it } }
        return { last }
    }

    /** Cuántas veces avisó que se guardó. */
    private fun TestScope.savedEvents(viewModel: ChildFormViewModel): () -> Int {
        var count = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.saved.collect { count++ } }
        return { count }
    }
}
