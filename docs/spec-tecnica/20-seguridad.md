# 20 · Seguridad y privacidad

Medidas técnicas que implementan los requerimientos de seguridad (`RNF-SEG`) y privacidad
(`RNF-PRI`) de la SPEC funcional.

## Seguridad

| RNF | Medida técnica |
|---|---|
| RNF-SEG-01 | TLS 1.2+ terminado en un proxy inverso (Caddy) con certificados automáticos de Let's Encrypt. HSTS activado. La app de `pilot` y `release` no permite HTTP sin cifrar |
| RNF-SEG-02 | Servidor: disco cifrado del proveedor y copias de seguridad cifradas con `age` antes de salir del servidor. Teléfono: Room con SQLCipher y tokens cifrados con Tink y el Android Keystore ([15](15-android.md#3-base-local-cifrada-adr-17)) |
| RNF-SEG-03 | bcrypt con costo 12 (ya implementado en `PasswordHasher`) |
| RNF-SEG-04 | Sesiones en tabla `sessions`; cada request valida `sid`; rotación del token de renovación con detección de reutilización ([11](11-backend.md#3-autenticación)) |
| RNF-SEG-05 | `AccessPolicy` en cada servicio, 404 cuando no hay acceso, DTOs distintos según el rol (los de tutor no tienen campos privados), pruebas de matriz de permisos en la integración continua |
| RNF-SEG-06 | Tabla `audit_log` de solo inserción, escrita por los servicios; limpieza a los 2 años |
| RNF-SEG-07 | Plugin `RateLimit` de Ktor: 120 requests por minuto por cuenta; `/auth/login` y `/auth/password-reset` 10 por minuto por IP; búsqueda por matrícula 20 por hora por cuenta. Bloqueo por intentos fallidos (RN-CTA-05) |
| RNF-SEG-08 | `FLAG_SECURE` en la ventana cuando el bloqueo está activado |
| RNF-SEG-09 | `pg_dump` diario cifrado y enviado a un almacenamiento externo, con 14 copias diarias y 8 semanales. Restauración probada antes del piloto ([30](30-infraestructura.md#copias-de-seguridad)) |

## Privacidad

| RNF | Medida técnica |
|---|---|
| RNF-PRI-01, 02 | Tabla `consents` con tipo, versión y fecha. `GET /me` informa si hay términos nuevos sin aceptar y la app bloquea el uso hasta aceptarlos |
| RNF-PRI-03 | El esquema no tiene columnas de DNI ni de imágenes. Los textos libres tienen límites de longitud |
| RNF-PRI-04 | Acceso: el tutor ve todo en la app y en el PDF. Supresión: `DELETE /me`, `DELETE /children/{id}` y `/web/delete-account`, que aplican RN-CTA-08 y RN-HIJ-08 en una transacción |
| RNF-PRI-05 | Push sin datos ([17](17-notificaciones-y-emails.md#1-push-sin-datos-personales-adr-06)) |
| RNF-PRI-06 | El panel no consulta tablas de salud y una prueba lo verifica ([16](16-panel-admin.md)) |
| RNF-PRI-08 | Entorno `pilot` separado, con su propia base. Al terminar, se borran las cuentas que no pidieron conservarse (script documentado) |
| RNF-PRI-09 | Hosting en Argentina o en un país con nivel adecuado (ADR-05). Proveedores externos sin datos de salud: FCM recibe solo un UUID; email y Sentry en región UE |
| RNF-PRI-10 | Sin SDKs de publicidad ni de analítica de terceros en la app |

## Reglas para el código

- **Logs:** nunca cuerpos de requests o responses, tokens, emails, nombres ni datos de salud;
  solo identificadores (UUID), rutas, estados y tiempos.
- **Sentry:** `sendDefaultPii = false`; se eliminan cuerpos, encabezados `Authorization` y
  parámetros de consulta antes de enviar.
- **Secretos:** solo en variables de entorno o en los secretos de GitHub Actions. Nunca en el
  repositorio. `.env` está en `.gitignore`.
- **Consultas a la base:** siempre con parámetros (Exposed los usa por defecto). Nada de SQL
  armado concatenando texto.
- **Dependencias:** Dependabot semanal; las alertas de seguridad se atienden antes que cualquier
  otra tarea.
- **Observaciones privadas:** ningún DTO destinado a otro usuario que no sea el autor tiene el
  campo `privateNotes`; una prueba automatizada recorre las respuestas de tutor y de otro
  pediatra y verifica que no aparezca.

## Amenazas consideradas

| Amenaza | Mitigación |
|---|---|
| Robo de teléfono | Base local cifrada, bloqueo opcional con huella o PIN, revocación de sesiones desde otro dispositivo cambiando la contraseña |
| Robo de token de renovación | Rotación con detección de reutilización: el uso de un token viejo revoca la sesión |
| Persona que se hace pasar por pediatra | Validación manual de matrícula (RF-ADM-02); auditoría de accesos |
| Recorrer el padrón de pediatras | Búsqueda solo por jurisdicción y número exactos, con límite de requests |
| Enumerar cuentas por email | Respuestas idénticas en recuperación de contraseña; mensaje genérico en el inicio de sesión |
| Acceso a niños ajenos cambiando el UUID | `AccessPolicy` en cada operación y 404 sin acceso; pruebas de matriz |
| Abuso del administrador | No accede a datos de salud; sus cambios quedan en `admin_audit` |
