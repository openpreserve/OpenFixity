# OpenFixity server image.
#
# Docker mode runs OpenFixity as a service, typically alongside the network storage it scans.
# The desktop app is the primary way to run OpenFixity; this is for the server/service use case.
#
# Multi-stage: the first stage builds the shaded jar (the Maven build downloads its own Node and
# builds the React frontend into the jar); the second stage is a small JRE runtime.

# --- build stage -------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Resolve dependencies first, as their own layer, so code changes do not re-download them.
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline || true

# Build. The frontend-maven-plugin fetches Node and builds the React app; skipTests keeps the
# image build fast (tests run in CI). SpotBugs is not run here either (it is bound to verify).
COPY src ./src
COPY frontend ./frontend
RUN mvn -q -B clean package -DskipTests

# --- runtime stage -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as a non-root user, and give it ownership of the data directory.
RUN useradd --system --uid 10001 --home /app openfixity \
    && mkdir -p /data \
    && chown -R openfixity:openfixity /app /data

COPY --from=build /build/target/open-fixity-*.jar /app/openfixity.jar
COPY docker-server.yml /app/server.yml

# Persistent data (the H2 database and logs) lives here. Mount directories to scan read-only,
# e.g. -v /srv/collections:/scan:ro, and browse to /scan in the UI.
VOLUME /data

# 8080 is the web UI and API; 8081 is the Dropwizard admin/healthcheck connector.
EXPOSE 8080 8081

USER openfixity

# Fails the container health check if the app stops serving.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/ || exit 1

ENTRYPOINT ["java", "-jar", "/app/openfixity.jar"]
CMD ["server", "/app/server.yml"]
