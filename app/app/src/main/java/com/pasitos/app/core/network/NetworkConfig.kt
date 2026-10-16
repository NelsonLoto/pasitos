package com.pasitos.app.core.network

object NetworkConfig {
    /**
     * Requiere correr `adb reverse tcp:8080 tcp:8080` una vez por sesión de depuración
     * (emulador o dispositivo físico, da igual — el túnel va por el cable/canal de adb,
     * no por red). Se prefiere esto sobre el alias `10.0.2.2` del emulador porque
     * funciona igual en dispositivo físico y evita por completo cualquier bloqueo de
     * firewall de Windows sobre el puerto (nos pasó en esta misma verificación).
     */
    const val BASE_URL = "http://localhost:8080"
}
