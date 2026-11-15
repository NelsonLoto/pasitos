package com.pasitos.backend.common

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.LoggerFactory
import java.util.UUID

internal fun loggablePath(path: String): String = path.split('/').joinToString("/") { segment ->
    if (segment.isEmpty() || segment.matches(Regex("[a-z]+(?:-[a-z]+)*|v[0-9]+|[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))) segment else "*"
}

fun Application.configureRequestLogging() {
    install(CallId) {
        generate { UUID.randomUUID().toString() }
        replyToHeader("X-Request-Id")
    }
    install(CallLogging) {
        logger = LoggerFactory.getLogger("com.pasitos.backend.Requests")
        filter { it.request.path() != "/health" }
        callIdMdc("call-id")
        format { call -> "${call.request.httpMethod.value} ${loggablePath(call.request.path())} ${call.response.status()?.value} ${call.processingTimeMillis()} ms" }
    }
}
