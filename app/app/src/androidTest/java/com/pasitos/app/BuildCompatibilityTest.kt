package com.pasitos.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Comprueba la integración real de Hilt, Compose y navegación después de actualizar el build. */
@RunWith(AndroidJUnit4::class)
class BuildCompatibilityTest {
    @Rule
    @JvmField
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun i0_12ConservaLoginAltaMedicionYCurva() {
        // Esta regresión comprueba Hilt y pantallas del prototipo; Google sigue simulado en I0.
        // El ingreso real y la renovación contra el backend se verifican en RealSessionTest.
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Continuar con Google").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("+ Agregar hijo/a").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("Continuar con Google").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Continuar con Google").performScrollTo().performClick()
        }
        awaitText("+ Agregar hijo/a")

        compose.onNodeWithText("+ Agregar hijo/a").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Prueba de versiones")
        compose.onNodeWithText("Fecha de nacimiento").performClick()
        compose.onNodeWithText("Confirmar").assertIsDisplayed()
        val today = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
        val dateLabel = today.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withLocale(compose.activity.resources.configuration.locales[0]),
        )
        compose.onNodeWithText(dateLabel, substring = true).performClick()
        compose.onNodeWithText("Confirmar").performClick()
        compose.onNodeWithText("Guardar y continuar").performScrollTo().performClick()
        awaitText("Registrar medición →")

        compose.onNodeWithText("Registrar medición →").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Peso (kg)")).performTextInput("3.5")
        compose.onNode(hasSetTextAction() and hasText("Talla / longitud (cm)")).performTextInput("50")
        compose.onNode(hasSetTextAction() and hasText("Perímetro cefálico (cm)"))
            .performScrollTo().performTextInput("35")
        compose.onNodeWithText("Guardar medición").performScrollTo().performClick()
        awaitText("Registrar medición →")

        compose.onNodeWithText("Crecimiento").performClick()
        awaitText("Última medición:", substring = true)
        compose.onNodeWithText("Última medición:", substring = true).assertIsDisplayed()

        compose.onNodeWithText("Perfil").performClick()
        awaitText("Cerrar sesión")
        compose.onNodeWithText("Cerrar sesión").performClick()
        awaitText("¿Cerrar sesión?")
        compose.onNodeWithText("Cancelar").performClick()
    }

    private fun awaitText(text: String, substring: Boolean = false) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
