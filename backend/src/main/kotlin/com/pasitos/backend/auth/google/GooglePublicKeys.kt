package com.pasitos.backend.auth.google

import com.auth0.jwk.JwkException
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwk.NetworkException
import com.auth0.jwt.interfaces.RSAKeyProvider
import com.pasitos.backend.common.ServiceUnavailableException
import com.pasitos.shared.api.ErrorCode
import java.net.URI
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

class GoogleUnavailableException : ServiceUnavailableException(
    ErrorCode.GOOGLE_UNAVAILABLE,
    "No pudimos conectar con Google. Probá de nuevo en unos minutos",
)

/**
 * Claves públicas con las que Google firma los ID tokens (11 · Backend §3). Solo verifica: no
 * firma nada, por eso no tiene clave privada.
 */
class GooglePublicKeys(private val jwks: JwkProvider) : RSAKeyProvider {

    /**
     * Nulo si Google no publica una clave con ese ID, o si se alcanzó el límite de pedidos: el token
     * se rechaza como inválido. Si Google no responde, lanza [GoogleUnavailableException] (503).
     */
    override fun getPublicKeyById(keyId: String?): RSAPublicKey? {
        if (keyId == null) return null
        return try {
            jwks.get(keyId).publicKey as? RSAPublicKey
        } catch (e: NetworkException) {
            throw GoogleUnavailableException()
        } catch (e: JwkException) {
            null
        }
    }

    override fun getPrivateKey(): RSAPrivateKey? = null

    override fun getPrivateKeyId(): String? = null

    companion object {
        private const val CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs"

        /**
         * Las baja de Google la primera vez que aparece cada ID de clave y las guarda un día. Cuando
         * Google rota sus claves, un token con un ID nuevo provoca otra descarga. El límite de
         * pedidos evita que tokens con IDs inventados hagan que el backend consulte a Google sin
         * parar.
         */
        fun remote(): GooglePublicKeys = GooglePublicKeys(
            JwkProviderBuilder(URI(CERTS_URL).toURL())
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .timeouts(5_000, 5_000)
                .build(),
        )
    }
}
