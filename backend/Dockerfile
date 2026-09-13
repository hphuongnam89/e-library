FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl && addgroup -S elib && adduser -S elib -G elib
WORKDIR /app
COPY --from=build --chown=elib:elib /workspace/target/elib-0.1.0-SNAPSHOT.jar app.jar
USER elib
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
