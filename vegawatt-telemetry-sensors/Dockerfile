FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/vegawatt-telemetry-sensors-0.1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
