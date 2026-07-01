# =============================================================
#  Multi-stage Dockerfile for Eureka Server
#
#  Stage 1 (build): Uses Maven to compile and package the JAR.
#  Stage 2 (runtime): Uses a minimal JRE image — no Maven, no
#  source code, no build tools in the final image.
#
#  Why multi-stage?
#    - Final image is ~100MB instead of ~500MB
#    - Source code is not shipped in the container
# =============================================================

# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
ENV MAVEN_OPTS="-Dhttps.protocols=TLSv1.2 -Djdk.tls.client.protocols=TLSv1.2 -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"

# Copy pom.xml first and download dependencies separately.
# Docker layer caches this step — if pom.xml hasn't changed,
# Maven won't re-download the internet on every build.
COPY pom.xml .
RUN mvn dependency:resolve -q || \
    mvn dependency:resolve -q || \
    mvn dependency:resolve -q

# Now copy source and build the JAR
COPY src ./src
RUN mvn package -DskipTests -q

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy only the fat JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the Eureka dashboard / registration port
EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
