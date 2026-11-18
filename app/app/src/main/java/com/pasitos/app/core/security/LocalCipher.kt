package com.pasitos.app.core.security

import android.content.Context
import android.util.Log
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/** Cifrado por uso: el dato asociado evita intercambiar una clave de base con un token. */
@Singleton
class LocalCipher internal constructor(
    private val context: Context,
    private val keysetPreferences: String,
    private val masterKeyAlias: String,
) {
    @Inject constructor(@ApplicationContext context: Context) : this(context, KEYSET_PREFERENCES, MASTER_KEY_ALIAS)
    private var cached: Aead? = null

    @Synchronized
    private fun aead(): Aead {
        cached?.let { return it }
        AeadConfig.register()
        val manager = AndroidKeysetManager.Builder()
            .withSharedPref(context, "keyset", keysetPreferences)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://$masterKeyAlias")
            .build()
        if (!manager.isUsingKeystore) {
            Log.w("LocalCipher", "Keystore no disponible: keyset protegido solo por el almacenamiento privado")
        }
        return manager.keysetHandle.getPrimitive(Aead::class.java).also { cached = it }
    }

    fun encrypt(value: ByteArray, associatedData: String): ByteArray =
        aead().encrypt(value, associatedData.toByteArray(Charsets.UTF_8))

    fun decrypt(value: ByteArray, associatedData: String): ByteArray =
        aead().decrypt(value, associatedData.toByteArray(Charsets.UTF_8))

    @Synchronized
    internal fun reset() {
        cached = null
        check(context.getSharedPreferences(keysetPreferences, Context.MODE_PRIVATE).edit().clear().commit())
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(masterKeyAlias) }
    }

    companion object {
        const val KEYSET_PREFERENCES = "pasitos_keyset"
        const val MASTER_KEY_ALIAS = "pasitos_master_key"
    }
}
