package com.pasitos.app.core.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [LocalCipher] con Tink: AES-256-GCM con una clave propia (keyset) que Tink guarda en el archivo
 * de preferencias `pasitos_keyset`, cifrada a su vez con una clave maestra del Android Keystore.
 * La clave maestra no se puede extraer del teléfono, así que sin ella el keyset no sirve.
 *
 * El keyset se crea o se lee la primera vez que se cifra algo, nunca al construir la clase: esa
 * primera vez usa el Keystore y no debe correr en el hilo principal.
 */
@Singleton
class KeystoreCipher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LocalCipher {

    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
        aead.encrypt(plaintext, associatedData)

    override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
        aead.decrypt(ciphertext, associatedData)

    private companion object {
        const val KEYSET_NAME = "local_cipher"
        const val KEYSET_FILE = "pasitos_keyset"
        const val MASTER_KEY_URI = "android-keystore://pasitos_master_key"
    }
}
