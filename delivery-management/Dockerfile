# Build context MUST be the repository root — see reservation/Dockerfile for why.
#   docker build -f delivery-management/Dockerfile -t delivery-management-service .

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY reservation/pom.xml reservation/pom.xml
COPY employee-management/pom.xml employee-management/pom.xml
COPY menu-management/pom.xml menu-management/pom.xml
COPY delivery-management/pom.xml delivery-management/pom.xml

RUN mvn -q -pl delivery-management -am dependency:go-offline -DskipTests

COPY delivery-management/src delivery-management/src
RUN mvn -q -pl delivery-management -am package -DskipTests \
    && cp delivery-management/target/delivery-management-*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/app.jar app.jar
USER spring

EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
