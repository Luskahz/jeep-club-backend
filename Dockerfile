FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -g 10001 app && adduser -D -u 10001 -G app app \
    && mkdir -p /app/storage && chown -R app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/backend-0.0.1-SNAPSHOT.jar /app/backend.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]
