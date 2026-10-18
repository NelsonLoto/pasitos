# Imagen del backend (30 · Infraestructura, ADR-05). Toda la configuración llega por variables de
# entorno: la imagen no tiene secretos ni archivos .env (ver .dockerignore en la raíz).
#
# Se construye desde la raíz del repositorio, porque el backend compila también shared:
#   docker build -f backend/Dockerfile -t pasitos-backend .
# Lo habitual es levantarla con `docker compose --profile full up` (backend/README.md).
#
# Las imágenes base van fijadas por digest (con la etiqueta como referencia), para que una etiqueta
# movida no cambie lo que se construye. Dependabot las actualiza.

# --- Compilación: JDK 21 compila shared y backend y arma la distribución con installDist
FROM eclipse-temurin:21-jdk-noble@sha256:107215954359f1ab0be058a247d7c64ed6e69c71c1682d471d38ef751ec8e964 AS build
WORKDIR /src
COPY shared/ shared/
COPY backend/ backend/
WORKDIR /src/backend
# Un gradlew con fin de línea CRLF (copia de Windows anterior a .gitattributes) no corre en Linux
RUN sed -i 's/\r$//' gradlew
# La caché de Gradle se conserva entre construcciones locales, pero no queda en la imagen
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon installDist

# --- Ejecución: solo el JRE 21 y la distribución del backend
FROM eclipse-temurin:21-jre-alpine@sha256:51ab5e3302e7141ce665ca3ea85e8b5cd648eafbc3c0c90dd79d6537684e4555
# Usuario sin privilegios. Los archivos de la aplicación quedan de root, así que el proceso no
# puede modificarlos
RUN addgroup -S -g 10001 pasitos && adduser -S -D -H -u 10001 -G pasitos pasitos
WORKDIR /app
COPY --from=build /src/backend/build/install/backend/ ./
USER pasitos
EXPOSE 8080
# La JVM usa hasta el 75 % de la memoria del contenedor (por defecto, solo el 25 %)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null "http://localhost:${PORT:-8080}/health" || exit 1
ENTRYPOINT ["/app/bin/backend"]
