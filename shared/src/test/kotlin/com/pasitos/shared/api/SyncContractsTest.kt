package com.pasitos.shared.api

import com.pasitos.shared.api.sync.*
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SyncContractsTest {
    @Test fun `I0-08 pagina y mutaciones conservan cuerpos genericos y codigos de dominio`() {
        val row = buildJsonObject { put("id", "synthetic-id"); put("privateValue", "synthetic-private") }
        val page = SyncChangesResponse(500, true, mapOf("measurements" to listOf(row)), listOf("child"), emptyList())
        assertEquals(page, Json.decodeFromString<SyncChangesResponse>(Json.encodeToString(page)))
        val request = SyncPushRequest(listOf(SyncMutation("mutation", "MEASUREMENT", "UPSERT", row)))
        assertEquals(request, Json.decodeFromString<SyncPushRequest>(Json.encodeToString(request)))
        val response = SyncPushResponse(listOf(SyncMutationResult("mutation", SyncMutationStatus.REJECTED,
            code = "measurement_not_found", rule = "RN-CRE-12", message = "No se encontró la medición")))
        assertEquals(response, Json.decodeFromString<SyncPushResponse>(Json.encodeToString(response)))
        assertFalse(page.toString().contains("synthetic-private"))
        assertFalse(request.toString().contains("synthetic-private"))
    }
}
