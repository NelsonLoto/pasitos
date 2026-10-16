package com.pasitos.app.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Abre la base con SQLCipher (ADR-17) y pide la clave recién cuando Room abre el archivo, cosa que
 * siempre hace fuera del hilo principal. Así, leer la clave de DataStore y descifrarla con el
 * Keystore no traba la interfaz, aunque la base se cree desde una pantalla.
 */
internal class EncryptedOpenHelperFactory(
    private val key: () -> ByteArray,
) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper =
        DeferredKeyOpenHelper(configuration, key)
}

private class DeferredKeyOpenHelper(
    private val configuration: SupportSQLiteOpenHelper.Configuration,
    private val key: () -> ByteArray,
) : SupportSQLiteOpenHelper {

    private var delegate: SupportSQLiteOpenHelper? = null
    private var writeAheadLogging = false

    override val databaseName: String?
        get() = configuration.name

    override val writableDatabase: SupportSQLiteDatabase
        get() = open().writableDatabase

    override val readableDatabase: SupportSQLiteDatabase
        get() = open().readableDatabase

    // Room lo llama al construirse, antes de abrir el archivo
    @Synchronized
    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        writeAheadLogging = enabled
        delegate?.setWriteAheadLoggingEnabled(enabled)
    }

    @Synchronized
    override fun close() {
        delegate?.close()
    }

    @Synchronized
    private fun open(): SupportSQLiteOpenHelper = delegate ?: run {
        System.loadLibrary("sqlcipher")
        SupportOpenHelperFactory(key().toRawKey())
            .create(configuration)
            .also {
                it.setWriteAheadLoggingEnabled(writeAheadLogging)
                delegate = it
            }
    }
}

/**
 * La clave ya es aleatoria, así que se le pasa a SQLCipher como clave cruda (`x'…'` en
 * hexadecimal). Con una contraseña común, SQLCipher derivaría la clave con PBKDF2 en cada
 * apertura, lo que suma cientos de milisegundos en un teléfono de gama baja sin ganar seguridad.
 */
private fun ByteArray.toRawKey(): ByteArray =
    joinToString(separator = "", prefix = "x'", postfix = "'") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }.toByteArray(Charsets.US_ASCII)
