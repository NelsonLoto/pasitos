package com.ninosano.app.data.advice

import com.ninosano.app.data.advice.model.AdviceTip
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeAdviceRepository @Inject constructor() : AdviceRepository {

    override val tips: StateFlow<List<AdviceTip>> = MutableStateFlow(
        listOf(
            AdviceTip(
                id = "advice-1",
                category = "ALIMENTACIÓN",
                title = "Introducción de sólidos",
                summary = "Cómo empezar con papillas y purés a partir de los 6 meses.",
                emoji = "🥄",
            ),
            AdviceTip(
                id = "advice-2",
                category = "DESARROLLO MOTOR",
                title = "Sentado sin apoyo",
                summary = "Qué esperar en el control de los 8 meses.",
                emoji = "🚼",
            ),
            AdviceTip(
                id = "advice-3",
                category = "SUEÑO",
                title = "Rutina de sueño nocturno",
                summary = "Consejos para consolidar horarios de siesta y noche.",
                emoji = "😴",
            ),
            AdviceTip(
                id = "advice-4",
                category = "SALUD",
                title = "Primeros dientes",
                summary = "Cómo cuidar la higiene bucal desde la erupción dentaria.",
                emoji = "🦷",
            ),
        ),
    ).asStateFlow()
}
