# Cómo contribuir

Las especificaciones y el plan vigentes están en
[Specs y plan de trabajo (Google Drive)](https://drive.google.com/drive/folders/1uXTYhwSPfYaTCs9BrH1HyrU7GV_3y0Y7).
Este archivo describe cómo trabajar con el repositorio.

## Antes de tomar una tarea

Personas y agentes de IA deben realizar e informar las verificaciones de
[32 · Proceso de desarrollo, §4](https://docs.google.com/document/d/1L7vJFUGmlVv4TrmARCOAzYEKt53A0tCr_314hcNZFM4).
La lista oficial se mantiene únicamente en Drive.

## Ramas y Pull Requests

- `develop` es la rama de integración y la base de las ramas de trabajo.
- `main` recibe promociones desde `develop` mediante PR.
- Una rama por tarea: `feature/42-alta-de-hijo`, `fix/42-descripcion` o `chore/42-descripcion`.
- No se permite push directo a `main` ni a `develop`: GitHub lo bloquea con un ruleset.

Después de las verificaciones previas:

1. Actualizar `develop` con `git switch develop` y `git pull --ff-only`.
2. Crear la rama de la tarea desde ese `develop` actualizado.
3. Publicarla y abrir un PR **en borrador** contra `develop` apenas se empieza.
4. Completar la plantilla del PR e incluir `Closes #42` para vincular su issue.
5. Al marcarlo listo para revisar, mover el issue a `En revisión` en el tablero.
6. Esperar la CI en verde y mergear. No hace falta la aprobación de otro integrante (GitHub no
   deja que el autor apruebe su propio PR). Pedir una revisión es opcional y se recomienda en
   PRs de autenticación, permisos o datos personales.
7. Eliminar la rama de trabajo después del merge.

No mover manualmente a `Hecho` una tarjeta con PR antes del merge: eso cierra el issue.

## Integración continua

**No se mergea un PR con la CI en rojo.** GitHub lo bloquea: `develop` y `main` tienen un ruleset
que exige un PR con los jobs de la CI en verde y no permite force push ni borrar la rama.

La CI corre en cada PR y en los pushes a `develop`:

- Cambios de Android: checksum del wrapper, control de zona horaria, compilación, pruebas
  unitarias y lint. Las pruebas de interfaz se ejecutan en emulador local.
- Cambios de Markdown: enlaces locales y anclas con lychee, sin consultar enlaces externos.
- Un cambio en `ci.yml` ejecuta todos los jobs disponibles. Un PR que solo cambia Markdown
  no compila Android.
- Si falla un job de Android, sus informes se conservan como artefactos durante siete días.

El backend y `shared` incorporarán sus jobs al crear sus builds en I0-04 e I0-02. El job de
Docker se incorpora con I0-09.

Usar la hora de Argentina para "hoy". El control temporal y su lista de excepciones están en
`.github/scripts/check-local-date-now.sh` y `local-date-now-allowlist.txt`: la lista solo puede
achicarse; al corregir un archivo hay que quitarlo de ella.

No bajar AGP o Gradle por una sugerencia del IDE: actualizar Android Studio y conservar
`distributionSha256Sum` en el wrapper. Para verificarlo:
`bash .github/scripts/check-gradle-wrapper.sh app`.

Dependabot revisa las dependencias los lunes a las 06:00 de Argentina. Las actualizaciones se
revisan en PRs propios; Kotlin, KSP y Hilt se actualizan juntos. Los cambios de especificación
se realizan en Drive según ADR-30, antes de implementar cambios de comportamiento.

## Commits

En español, describiendo qué cambia y por qué, con los prefijos `feat:`, `fix:`, `chore:`,
`docs:`, `test:` o `refactor:`. Ejemplo: `chore: incorporar CI por componente (I0-03)`.
