package com.pasitos.app.data.advice

import com.pasitos.app.data.advice.model.AdviceTip
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
                ageRangeLabel = "6 a 8 meses",
                title = "Introducción de sólidos",
                summary = "Cómo empezar con papillas y purés a partir de los 6 meses.",
                emoji = "🥄",
                body = listOf(
                    "A partir de los 6 meses, además de la leche materna o de fórmula, podés empezar a incorporar alimentos semisólidos: purés de frutas, verduras y cereales sin gluten.",
                    "Introducí un alimento nuevo por vez, con intervalos de 2 a 3 días, para poder identificar posibles alergias.",
                    "Respetá las señales de hambre y saciedad del bebé — no lo obligues a terminar el plato. La cantidad varía día a día.",
                    "Evitá agregar sal, azúcar y miel en el primer año de vida.",
                ),
            ),
            AdviceTip(
                id = "advice-2",
                category = "DESARROLLO MOTOR",
                ageRangeLabel = "7 a 9 meses",
                title = "Sentado sin apoyo",
                summary = "Qué esperar en el control de los 8 meses.",
                emoji = "🚼",
                body = listOf(
                    "Entre los 7 y 9 meses la mayoría de los bebés logra mantenerse sentado sin apoyo durante varios minutos, con la espalda relativamente erguida.",
                    "Este hito suele ir acompañado de más fuerza en el tronco y mayor interés en alcanzar objetos con ambas manos.",
                    "Si a los 9 meses todavía necesita apoyo constante para mantenerse sentado, es un buen tema para conversar en el próximo control.",
                    "Dale tiempo boca abajo y sentado con supervisión durante el juego diario — ayuda a fortalecer el tronco.",
                ),
            ),
            AdviceTip(
                id = "advice-3",
                category = "SUEÑO",
                ageRangeLabel = "6 a 9 meses",
                title = "Rutina de sueño nocturno",
                summary = "Consejos para consolidar horarios de siesta y noche.",
                emoji = "😴",
                body = listOf(
                    "A esta edad la mayoría de los bebés puede empezar a dormir períodos más largos de noche, aunque los despertares ocasionales siguen siendo normales.",
                    "Una rutina simple y constante antes de dormir (baño, upa, luz baja) ayuda a que el cuerpo asocie esas señales con el sueño.",
                    "Intentá mantener horarios de siesta y de acostarse parecidos todos los días, incluso los fines de semana.",
                    "Evitá pantallas y estimulación fuerte en la hora previa a dormir.",
                ),
            ),
            AdviceTip(
                id = "advice-4",
                category = "SALUD",
                ageRangeLabel = "6 a 10 meses",
                title = "Primeros dientes",
                summary = "Cómo cuidar la higiene bucal desde la erupción dentaria.",
                emoji = "🦷",
                body = listOf(
                    "Los primeros dientes suelen aparecer entre los 6 y 10 meses, generalmente los incisivos centrales inferiores.",
                    "Limpiá las encías y los primeros dientes con una gasa húmeda o un cepillo suave para bebés, sin necesidad de pasta dental todavía.",
                    "Es normal que la erupción dentaria venga con un poco de irritabilidad, babeo o necesidad de morder — un mordillo frío puede ayudar a calmar la molestia.",
                    "Fiebre alta o diarrea no son parte normal de la dentición — si aparecen, consultá al pediatra.",
                ),
            ),
        ),
    ).asStateFlow()
}
