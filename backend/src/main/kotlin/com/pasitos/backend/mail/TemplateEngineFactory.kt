package com.pasitos.backend.mail

import org.thymeleaf.TemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

/**
 * Un solo motor de Thymeleaf para las páginas públicas (`/web/...`) y los emails (ADR-09, ADR-27):
 * un resolvedor para plantillas HTML (`email/verify-email.html`) y otro para la versión de texto
 * plano de los emails (`email/verify-email.txt`, modo TEXT de Thymeleaf), ambas en
 * `src/main/resources/templates`. El nombre con el que se pide cada plantilla incluye su
 * extensión, así cada resolvedor atiende solo la suya.
 */
object TemplateEngineFactory {

    fun create(): TemplateEngine = TemplateEngine().apply(::configure)

    /** El plugin `Thymeleaf` de Ktor expone su propio [TemplateEngine] como configuración: se le agregan los mismos resolvedores. */
    fun configure(engine: TemplateEngine) {
        engine.addTemplateResolver(resolver(pattern = "*.html", mode = TemplateMode.HTML, order = 1))
        engine.addTemplateResolver(resolver(pattern = "*.txt", mode = TemplateMode.TEXT, order = 2))
    }

    private fun resolver(pattern: String, mode: TemplateMode, order: Int) = ClassLoaderTemplateResolver().apply {
        this.order = order
        prefix = "templates/"
        suffix = ""
        templateMode = mode
        characterEncoding = "UTF-8"
        // Cada resolvedor solo atiende las plantillas de su propia extensión.
        setResolvablePatterns(setOf(pattern))
    }
}
