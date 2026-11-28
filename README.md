# Pasitos

App Android para que tutores y pediatras sigan el crecimiento, las vacunas y el desarrollo de
niños y niñas de 0 a 15 años.

## Especificaciones y plan

La SPEC funcional, la SPEC técnica, las decisiones y el plan de trabajo están en la carpeta de
Google Drive [Specs y plan de trabajo](https://drive.google.com/drive/folders/1uXTYhwSPfYaTCs9BrH1HyrU7GV_3y0Y7)
(acceso para el equipo). En este repositorio quedan solo el código y lo necesario para trabajar
con él; las tareas están en los issues y en el tablero del proyecto.

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `app/` | App Android (Kotlin + Jetpack Compose). Cómo compilarla y correrla: [`app/README.md`](app/README.md) |
| `backend/` | API del backend (Kotlin + Ktor): [`backend/README.md`](backend/README.md) |
| `shared/` | Biblioteca Kotlin/JVM que usan la app y el backend como build compuesto: cálculos (edades, crecimiento, vacunas) y contratos de la API. Sus pruebas se corren con `./gradlew test` dentro de la carpeta; un cambio se ve en la app y en el backend al recompilarlos |
| `docs/` | Documento funcional original, solo como referencia histórica. Lo vigente está en Drive |
| `.github/` | Plantilla de Pull Request |

## Cómo contribuir

Ramas, Pull Requests y verificaciones antes de tomar una tarea: [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Entorno local con Docker

Requiere Docker Desktop iniciado, con contenedores Linux y Docker Compose 2.22 o superior.
Antes de iniciar, copiar `backend/.env.example` a `backend/.env` (en PowerShell:
`Copy-Item backend/.env.example backend/.env`). Completar `DB_PASSWORD` con una
contraseña local propia, cambiar `DB_PORT=5433` y completar `JWT_SECRET` con una
clave aleatoria de al menos 64 caracteres. No guardar ese archivo en Git.
Para generar la clave en PowerShell:

```powershell
$jwtBytes = New-Object byte[] 48
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtBytes)
[Convert]::ToBase64String($jwtBytes)
$jwtRng.Dispose()
```

Copiar el resultado a `JWT_SECRET` dentro de `backend/.env`. Compose no carga
automáticamente ese archivo: pasarlo explícitamente en todos los comandos.
Sin la clave, Compose detiene la configuración antes de crear contenedores;
el backend también comprueba que tenga al menos 64 caracteres.
Desde la raíz del repositorio:

```sh
docker compose --env-file backend/.env up -d --wait
```

Levanta PostgreSQL 16 en `127.0.0.1:5433` y Mailpit: correos de prueba en
<http://localhost:8025>, SMTP en `127.0.0.1:1025`. Mailpit captura los mensajes;
no los envía a destinatarios reales. La base usa `pasitos` como nombre y usuario y
la contraseña local indicada en `backend/.env`. Los puertos solo
aceptan conexiones desde esta computadora.

Para levantar también el backend y comprobar `/health`:

```sh
docker compose --env-file backend/.env --profile full up -d --wait
curl http://localhost:8080/health
```

En PowerShell, usar `Invoke-RestMethod http://localhost:8080/health`. Debe responder
`{"status":"ok","database":"connected"}`. El perfil `full` reconstruye la imagen
en cada `up` para usar el código actual. Compila `shared` y `backend` con JDK 21;
ejecuta solo la distribución con JRE 21 y un usuario sin privilegios. Los archivos
de la aplicación son de solo lectura para ese usuario y la JVM usa como máximo
el 75 % del límite de 512 MB del contenedor. No requiere Java instalado en el host.

Para comprobar la configuración sin iniciar los servicios (sin imprimir secretos):

```sh
docker compose --env-file backend/.env --profile full config --quiet
```

`DB_PORT` y `PORT` cambian los puertos publicados; dentro de Docker se usan siempre
`db:5432`, `mailpit:1025` y el backend escucha en 8080. El ejemplo actual del backend
indica 5432 para ejecución directa: al usarlo con este entorno, cambiarlo a 5433
o publicar la base en 5432 si ese puerto está libre. Para ejecutar desde IntelliJ,
usar esas mismas credenciales y `DB_HOST=localhost`, `DB_PORT=5433` (ver
[`backend/README.md`](backend/README.md)). `JWT_SECRET` es obligatorio desde #159.
El archivo de configuración no entra en la imagen.

Para conectar un emulador o teléfono por USB: `adb reverse tcp:8080 tcp:8080`.
Para ver el estado y detener el entorno conservando la base:

```sh
docker compose --env-file backend/.env --profile full ps
docker compose --env-file backend/.env --profile full down
```

Usar el mismo `--env-file` al detener si se usó al iniciar. El volumen `postgres-data`
conserva los datos; no usar `down -v` salvo que se quiera borrar esa base local.
