# control-nino-sano-app

App de seguimiento y control del niño sano (Práctica III).

Permite a padres, madres y tutores registrar mediciones antropométricas y hábitos
del niño desde el nacimiento hasta la edad escolar, visualizar la curva de
crecimiento por percentiles (estándar SAP/Argentina), gestionar el calendario de
vacunación oficial argentino y exportar reportes en PDF para la consulta pediátrica.

El detalle de objetivo, alcance, requerimientos funcionales/no funcionales y stack
técnico está en [`docs/Documento_Funcional_App_Nino_Sano.docx`](docs/Documento_Funcional_App_Nino_Sano.docx).

## Estructura del repositorio

- `app/` — Aplicación Android nativa (Kotlin + Jetpack Compose).
- `backend/` — API propia (Kotlin + Ktor) + base de datos PostgreSQL.
- `docs/` — Documentación funcional y técnica del proyecto.

## Stack

| Componente | Tecnología |
|---|---|
| App móvil | Kotlin, Jetpack Compose |
| Backend | Kotlin, Ktor |
| Base de datos | PostgreSQL |
| Autenticación | JWT + Google Sign-In (OAuth2) |
| Notificaciones push | Firebase Cloud Messaging (solo transporte) |
| IDE app | Android Studio |
| IDE backend | IntelliJ IDEA Community |
