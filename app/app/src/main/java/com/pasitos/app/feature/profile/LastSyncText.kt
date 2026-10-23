package com.pasitos.app.feature.profile

import com.pasitos.app.core.time.ArgentinaClock
import java.time.Instant
import java.time.format.DateTimeFormatter

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val DAY = DateTimeFormatter.ofPattern("dd/MM")
private val DAY_AND_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * RNF-OFF-04, en hora argentina (ADR-26): "Última sincronización: hoy a las 14:32 · 2 cambios sin
 * sincronizar". Antes de la primera sincronización: "Todavía no se sincronizó".
 */
internal fun lastSyncText(lastSyncedAt: Instant?, pendingChanges: Int, now: Instant): String {
    val synced = lastSyncedAt?.let { "Última sincronización: ${whenText(it, now)}" } ?: "Todavía no se sincronizó"
    val pending = when (pendingChanges) {
        0 -> null
        1 -> "1 cambio sin sincronizar"
        else -> "$pendingChanges cambios sin sincronizar"
    }
    return listOfNotNull(synced, pending).joinToString(" · ")
}

private fun whenText(moment: Instant, now: Instant): String {
    val at = moment.atZone(ArgentinaClock.ZONE)
    val today = now.atZone(ArgentinaClock.ZONE).toLocalDate()
    val day = when (at.toLocalDate()) {
        today -> "hoy"
        today.minusDays(1) -> "ayer"
        else -> (if (at.year == today.year) DAY else DAY_AND_YEAR).format(at)
    }
    return "$day a las ${TIME.format(at)}"
}
