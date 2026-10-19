package com.pasitos.app.data.child.remote

import com.pasitos.app.core.network.toApiException
import com.pasitos.app.core.network.withConnection
import com.pasitos.shared.api.child.ChildResponse
import com.pasitos.shared.api.child.CreateChildRequest
import com.pasitos.shared.api.child.DeletionPreviewResponse
import com.pasitos.shared.api.child.PatchChildRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import javax.inject.Inject

/**
 * `/api/v1/children` (12 · API): alta, edición y eliminación, que solo funcionan en línea. Sin red
 * lanza `NoConnectionException`; si el servidor rechaza el pedido, `ApiException` con su mensaje.
 */
class ChildRemoteDataSource @Inject constructor(private val httpClient: HttpClient) {

    suspend fun create(request: CreateChildRequest): ChildResponse =
        send(HttpStatusCode.Created) {
            httpClient.post("/api/v1/children") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.body()

    suspend fun update(childId: String, request: PatchChildRequest): ChildResponse =
        send(HttpStatusCode.OK) {
            httpClient.patch("/api/v1/children/$childId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.body()

    suspend fun deletionPreview(childId: String): DeletionPreviewResponse =
        send(HttpStatusCode.OK) { httpClient.get("/api/v1/children/$childId/deletion-preview") }.body()

    suspend fun delete(childId: String) {
        send(HttpStatusCode.NoContent) { httpClient.delete("/api/v1/children/$childId") }
    }

    private suspend fun send(success: HttpStatusCode, request: suspend () -> HttpResponse): HttpResponse =
        withConnection {
            val response = request()
            if (response.status != success) throw response.toApiException()
            response
        }
}
