package com.pasitos.backend.child

import com.pasitos.backend.auth.AUTH_JWT
import com.pasitos.backend.common.caller
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.childRoutes(childService: ChildService) {
    authenticate(AUTH_JWT) {
        post("/children") {
            call.respond(HttpStatusCode.Created, childService.create(call.caller(), call.receive()))
        }

        get("/children") {
            call.respond(HttpStatusCode.OK, childService.list(call.caller()))
        }
    }
}
