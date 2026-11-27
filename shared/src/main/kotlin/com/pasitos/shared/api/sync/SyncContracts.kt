package com.pasitos.shared.api.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SyncChangesResponse(
    val cursor: Long,
    val hasMore: Boolean,
    val changes: Map<String, List<JsonObject>>,
    val newChildIds: List<String>,
    val revokedChildIds: List<String>,
) {
    override fun toString() = "SyncChangesResponse(cursor=$cursor, hasMore=$hasMore, data=[redacted])"
}

@Serializable
data class SyncSnapshotResponse(val changes: Map<String, List<JsonObject>>) {
    override fun toString() = "SyncSnapshotResponse([redacted])"
}

@Serializable
data class SyncPushRequest(val mutations: List<SyncMutation>) {
    override fun toString() = "SyncPushRequest(count=${mutations.size}, data=[redacted])"
}

@Serializable
data class SyncMutation(val mutationId: String, val entity: String, val op: String, val data: JsonObject) {
    override fun toString() = "SyncMutation([redacted])"
}

@Serializable
enum class SyncMutationStatus { APPLIED, REJECTED }

@Serializable
data class SyncMutationResult(
    val mutationId: String,
    val status: SyncMutationStatus,
    val syncVersion: Long? = null,
    val code: String? = null,
    val rule: String? = null,
    val message: String? = null,
) {
    override fun toString() = "SyncMutationResult(status=$status, code=$code, data=[redacted])"
}

@Serializable
data class SyncPushResponse(val results: List<SyncMutationResult>)
