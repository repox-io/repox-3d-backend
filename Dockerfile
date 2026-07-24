
# ============================================================================
# Application image for repox-3d-api.
#
# Builds the Spring Boot jar and layers it onto the stable "processing arsenal"
# base image (see Dockerfile.3dbase). Per-commit builds only recompile the jar —
# no Blender/TripoSR/torch/gltfpack downloads.
# ============================================================================
# Base image pin. Declared before the first FROM so it can be used in the runtime FROM below.
# In CI, override with the full registry path (bump deliberately when Dockerfile.3dbase is rebuilt):
#   --build-arg BASE_IMAGE=<registry>/<namespace>/repox-3d-base:<tag>
# NOTE: 0.0.3 adds libgomp1 (gfconvert OpenMP runtime)
ARG BASE_IMAGE=repox-3d-base:0.0.3
# ---------- Builder ----------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
ARG SKIP_TESTS=true
RUN mvn -B -DskipTests=${SKIP_TESTS} package
# ---------- Runtime (thin layer on the prebuilt arsenal base) ----------
FROM ${BASE_IMAGE} AS runtime
COPY --from=builder --chown=appuser:root /workspace/target/*.jar /app/app.jar
ENV JAVA_OPTS="" \
    SERVER_PORT=8080
EXPOSE 8080
USER appuser
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --server.port=${SERVER_PORT}"]
