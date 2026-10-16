package com.pasitos.app.core.security

import android.content.Context
import android.util.Log
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
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
class KeystoreCipher internal constructor(
    private val context: Context,
    private val keysetFile: String,
    private val masterKeyAlias: String,
) : LocalCipher {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context, KEYSET_FILE, MASTER_KEY_ALIAS)

    private var aead: Aead? = null

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
        withAead { it.encrypt(plaintext, associatedData) }

    override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
        withAead { it.decrypt(ciphertext, associatedData) }

    @Synchronized
    override fun reset() {
        aead = null
        context.deleteSharedPreferences(keysetFile)
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(masterKeyAlias)
    }

    // El Keystore también falla con IOException o ProviderException: se unifican
    private inline fun <T> withAead(block: (Aead) -> T): T = try {
        block(aead())
    } catch (e: IOException) {
        throw GeneralSecurityException(e)
    } catch (e: ProviderException) {
        throw GeneralSecurityException(e)
    }

    @Synchronized
    private fun aead(): Aead = aead ?: createAead().also { aead = it }

    private fun createAead(): Aead {
        AeadConfig.register()
        val manager = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, keysetFile)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://$masterKeyAlias")
            .build()
        if (!manager.isUsingKeystore) {
            // Tink deja el keyset sin cifrar en el almacenamiento privado de la app cuando el
            // Keystore no funciona en el teléfono. Riesgo aceptado (15 · App Android, sección 3)
            Log.w(TAG, "Android Keystore no disponible: el keyset se guarda sin su protección")
        }
        return manager.keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private companion object {
        const val TAG = "KeystoreCipher"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEYSET_NAME = "local_cipher"
        const val KEYSET_FILE = "pasitos_keyset"
        const val MASTER_KEY_ALIAS = "pasitos_master_key"
    }
}
