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

# Copy common and mail-service source
COPY common/src ./common/src
COPY mail-service/src ./mail-service/src

# Build the application
RUN apk add --no-cache maven && \
    mvn -pl common,mail-service -am clean package -DskipTests && \
    apk del maven

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="thebuilders"

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /build/mail-service/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs

EXPOSE 8082

# Add healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
