# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-noble@sha256:dc333cdac241f073941f043792f2a2f9b790bfe7cd98ef2843c08880986708de AS build
WORKDIR /workspace
COPY shared/ shared/
COPY backend/ backend/
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x backend/gradlew && \
    backend/gradlew -p backend installDist --no-daemon --max-workers=2 --console=plain

FROM eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70
RUN addgroup -S -g 10001 pasitos && adduser -S -D -H -u 10001 -G pasitos pasitos
WORKDIR /opt/pasitos
COPY --from=build /workspace/backend/build/install/pasitos-backend/ ./
RUN chmod -R a-w . && chmod 555 bin/pasitos-backend
ENV HOST=0.0.0.0 PORT=8080 JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
    CMD wget -q -O /dev/null "http://127.0.0.1:${PORT}/health" || exit 1
CMD ["/opt/pasitos/bin/pasitos-backend"]
