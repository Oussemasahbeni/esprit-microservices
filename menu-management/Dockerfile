# Build context MUST be the repository root — see reservation/Dockerfile for why.
#   docker build -f menu-management/Dockerfile -t menu-management-service .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY reservation/pom.xml reservation/pom.xml
COPY employee-management/pom.xml employee-management/pom.xml
COPY menu-management/pom.xml menu-management/pom.xml
COPY delivery-management/pom.xml delivery-management/pom.xml

RUN mvn -q -pl menu-management -am dependency:go-offline -DskipTests

COPY menu-management/src menu-management/src
RUN mvn -q -pl menu-management -am package -DskipTests \
    && cp menu-management/target/menu-management-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/app.jar app.jar
USER spring

EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
