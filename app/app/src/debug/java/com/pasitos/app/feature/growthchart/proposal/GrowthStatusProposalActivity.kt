package com.pasitos.app.feature.growthchart.proposal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pasitos.app.ui.theme.PsTheme

/** Demostración de T-07. No se incluye en release ni forma parte de la navegación de la app. */
class GrowthStatusProposalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PsTheme {
                GrowthStatusProposalScreen(onBack = ::finish)
            }
        }
    }
}
