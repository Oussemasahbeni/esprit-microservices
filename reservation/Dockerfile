# Build context MUST be the repository root (not this directory), because this is a
# multi-module Maven reactor: the parent pom.xml and every sibling module's pom.xml have
# to be present for Maven to resolve the reactor, even though only the `reservation`
# module is actually compiled here.
#
#   docker build -f reservation/Dockerfile -t reservation-service .
#
# (docker-compose.yml is already wired with the matching `context: .` / `dockerfile:`.)

# ---- Build stage -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Copy just the POMs first so Maven can resolve/cache dependencies in their own
# Docker layer — re-running this layer only when a pom.xml actually changes.
COPY pom.xml ./pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY reservation/pom.xml reservation/pom.xml
COPY employee-management/pom.xml employee-management/pom.xml
COPY menu-management/pom.xml menu-management/pom.xml
COPY delivery-management/pom.xml delivery-management/pom.xml

RUN mvn -q -pl reservation -am dependency:go-offline -DskipTests

COPY reservation/src reservation/src
RUN mvn -q -pl reservation -am package -DskipTests \
    && cp reservation/target/reservation-*.jar /workspace/app.jar

# ---- Runtime stage ----------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/app.jar app.jar
USER spring

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
