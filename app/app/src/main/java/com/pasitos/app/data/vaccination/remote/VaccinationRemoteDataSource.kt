package com.pasitos.app.data.vaccination.remote

import com.pasitos.app.core.network.toApiException
import com.pasitos.app.core.network.withConnection
import com.pasitos.shared.api.vaccination.AppliedDoseResponse
import com.pasitos.shared.api.vaccination.CreateAppliedDoseRequest
import com.pasitos.shared.api.vaccination.OtherVaccineRequest
import com.pasitos.shared.api.vaccination.OtherVaccineResponse
import com.pasitos.shared.api.vaccination.PatchAppliedDoseRequest
import com.pasitos.shared.api.vaccination.RetroactiveDosesRequest
import com.pasitos.shared.api.vaccination.RetroactiveDosesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import javax.inject.Inject

/** Resultado de pedir el calendario con el `ETag` de la copia local. */
sealed interface CalendarFetch {
    /** `304`: la copia local sigue vigente. */
    data object NotModified : CalendarFetch

    /** El calendario nuevo en JSON, con su `ETag`. */
    data class Updated(val json: String, val etag: String?) : CalendarFetch
}

/**
 * Vacunación en `/api/v1` (12 · API). El calendario se lee también sin cambios; las dosis y las
 * otras vacunas solo se escriben en línea (RNF-OFF-06). Sin red lanza `NoConnectionException`; si
 * el servidor rechaza el pedido, `ApiException` con su mensaje.
 */
class VaccinationRemoteDataSource @Inject constructor(private val httpClient: HttpClient) {

    suspend fun calendar(etag: String?): CalendarFetch = withConnection {
        val response = httpClient.get("/api/v1/vaccination-calendar") {
            etag?.let { header(HttpHeaders.IfNoneMatch, it) }
        }
        when (response.status) {
            HttpStatusCode.NotModified -> CalendarFetch.NotModified
            HttpStatusCode.OK -> CalendarFetch.Updated(response.bodyAsText(), response.headers[HttpHeaders.ETag])
            else -> throw response.toApiException()
        }
    }

    suspend fun createDose(childId: String, request: CreateAppliedDoseRequest): AppliedDoseResponse =
        send(HttpStatusCode.Created) {
            httpClient.post("/api/v1/children/$childId/applied-doses") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.body()

    suspend fun registerRetroactive(childId: String, request: RetroactiveDosesRequest): RetroactiveDosesResponse =
        send(HttpStatusCode.Created) {
            httpClient.post("/api/v1/children/$childId/applied-doses/retroactive") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.body()

    suspend fun updateDose(doseId: String, request: PatchAppliedDoseRequest): AppliedDoseResponse =
        send(HttpStatusCode.OK) {
            httpClient.patch("/api/v1/applied-doses/$doseId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.body()

    suspend fun deleteDose(doseId: String) {
        send(HttpStatusCode.NoContent) { httpClient.delete("/api/v1/applied-doses/$doseId") }
    }

    suspend fun createOtherVaccine(childId: String, request: OtherVaccineRequest): OtherVaccineResponse =
        send(HttpStatusCode.Created) {
            httpClient.post("/api/v1/children/$childId/other-vaccines") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.body()

    suspend fun updateOtherVaccine(vaccineId: String, request: OtherVaccineRequest): OtherVaccineResponse =
        send(HttpStatusCode.OK) {
            httpClient.patch("/api/v1/other-vaccines/$vaccineId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.body()

    suspend fun deleteOtherVaccine(vaccineId: String) {
        send(HttpStatusCode.NoContent) { httpClient.delete("/api/v1/other-vaccines/$vaccineId") }
    }

    private suspend fun send(success: HttpStatusCode, request: suspend () -> HttpResponse): HttpResponse =
        withConnection {
            val response = request()
            if (response.status != success) throw response.toApiException()
            response
        }
}
