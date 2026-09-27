# Build stage: compile the Spring Boot jar with the Gradle wrapper.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# Download dependencies in their own layer so code changes don't re-download them.
RUN ./gradlew dependencies --no-daemon -q > /dev/null
COPY src src
# Tests need a database, so they run outside the image build.
RUN ./gradlew bootJar --no-daemon -q

# Runtime stage: just the JRE and the jar.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
USER app
COPY --from=build /workspace/build/libs/open5e-backend-*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
