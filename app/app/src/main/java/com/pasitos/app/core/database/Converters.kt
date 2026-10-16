package com.pasitos.app.core.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * Los dos tipos de tiempo de ADR-26, sin mezclarlos: fechas del calendario como texto ISO
 * (`2026-10-14`, sin hora ni zona) y momentos como milisegundos desde 1970 en UTC.
 */
class Converters {

    @TypeConverter
    fun localDateToText(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun textToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
