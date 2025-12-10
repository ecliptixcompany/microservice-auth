# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy parent pom and all module poms (for Maven reactor)
COPY pom.xml .
COPY discovery-server/pom.xml ./discovery-server/
COPY api-gateway/pom.xml ./api-gateway/
COPY auth-service/pom.xml ./auth-service/
COPY mail-service/pom.xml ./mail-service/
COPY common/pom.xml ./common/

# Copy only api-gateway source
COPY api-gateway/src ./api-gateway/src

# Build the application
RUN apk add --no-cache maven && \
    mvn -pl api-gateway -am clean package -DskipTests && \
    apk del maven

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="thebuilders"

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /build/api-gateway/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs

EXPOSE 8080

# Add healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
