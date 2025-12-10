# 🔐 Microservice Auth System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.0-brightgreen.svg)](https://spring.io/projects/spring-cloud)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **Production-ready microservice authentication system** with JWT, Email verification, Monitoring (Grafana + Loki), and more. Clone and customize for your own projects!

## ✨ Features

- 🔐 **JWT Authentication** - Secure access & refresh token system
- 📧 **Email Verification** - Registration email verification flow
- 🔑 **Password Reset** - Forgot password with email link
- 👥 **Role-Based Access** - ADMIN / USER roles (easily extensible)
- 🌐 **API Gateway** - Single entry point with JWT validation
- 📊 **Monitoring Stack** - Grafana + Loki + Promtail for log visualization
- 📬 **Mail Service** - Async email sending via RabbitMQ
- 🔍 **Service Discovery** - Netflix Eureka for service registration
- 📝 **Swagger UI** - Interactive API documentation
- 🐳 **Docker Ready** - PostgreSQL, RabbitMQ, MailHog containers

## 🏗 Architecture

```
                                    ┌─────────────────┐
                                    │   Your Frontend │
                                    │  (React/Vue/etc)│
                                    └────────┬────────┘
                                             │
                                             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            API Gateway (8080)                                │
│                    • JWT Validation • Rate Limiting • Routing                │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │
                 ┌────────────────────┼────────────────────┐
                 │                    │                    │
                 ▼                    ▼                    ▼
    ┌────────────────────┐ ┌──────────────────┐ ┌──────────────────┐
    │   Auth Service     │ │   Mail Service   │ │  Your Services   │
    │      (8081)        │ │     (8082)       │ │    (Add here)    │
    │                    │ │                  │ │                  │
    │ • Register/Login   │ │ • Welcome Email  │ │ • Custom logic   │
    │ • JWT Generation   │ │ • Password Reset │ │ • Business APIs  │
    │ • Password Reset   │ │ • Notifications  │ │                  │
    └─────────┬──────────┘ └────────┬─────────┘ └──────────────────┘
              │                     │
              │     RabbitMQ        │
              ▼     (Events)        ▼
    ┌──────────────────┐  ┌──────────────────┐
    │   PostgreSQL     │  │    MailHog       │
    │   (Auth DB)      │  │  (Dev SMTP)      │
    └──────────────────┘  └──────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                    Monitoring Stack                               │
│  ┌──────────┐    ┌──────────┐    ┌──────────────────┐           │
│  │ Promtail │───▶│   Loki   │───▶│     Grafana      │           │
│  │(Collector)│   │(Storage) │    │  (Visualization) │           │
│  └──────────┘    └──────────┘    │   localhost:3001 │           │
│                                   └──────────────────┘           │
└──────────────────────────────────────────────────────────────────┘

                    ┌──────────────────────────┐
                    │   Discovery Server       │
                    │   (Eureka - 8761)        │
                    │   Service Registry       │
                    └──────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites

**Only Docker & Docker Compose required!** ✨
- Docker 20.10+
- Docker Compose 2.0+

**No Java or Maven installation needed** - everything runs in Docker containers!

### 1. Clone & Start

```bash
git clone https://github.com/YOUR_USERNAME/microservice-auth.git
cd microservice-auth

# Start EVERYTHING with one command (infrastructure + all services + monitoring)
make start
```

That's it! The first run will take ~5-10 minutes to build all Docker images and start services.

### 2. Alternative: Step-by-Step Start

```bash
# Start only infrastructure (DB, RabbitMQ, Redis, MailHog)
make start-infra

# Build all service Docker images
make build

# Start all microservices
make start

# Start monitoring stack
make start-monitoring
```

### 3. Access Points

| Service | URL | Description |
|---------|-----|-------------|
| **API Gateway** | http://localhost:8080 | Main API endpoint |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API Documentation |
| **Eureka Dashboard** | http://localhost:8761 | Service Registry |
| **Grafana** | http://localhost:3001 | Log Visualization (admin/admin123) |
| **MailHog** | http://localhost:8025 | Email Testing UI |
| **RabbitMQ** | http://localhost:15672 | Message Queue (guest/guest) |

## 📖 API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login and get tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Invalidate refresh token |
| GET | `/api/v1/auth/verify-email?token=xxx` | Verify email address |
| POST | `/api/v1/auth/forgot-password` | Request password reset |
| POST | `/api/v1/auth/reset-password` | Reset password with token |
| GET | `/api/v1/auth/me` | Get current user info |

### Example: Register

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Example: Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

## 👥 Roles

By default, the system includes two roles:

```java
public enum Role {
    ADMIN,  // System administrators with full access
    USER    // Regular users with standard access
}
```

### Extending Roles

To add custom roles, edit `common/src/main/java/.../enums/Role.java`:

```java
public enum Role {
    ADMIN,
    USER,
    MODERATOR,  // Add your custom roles
    PREMIUM_USER
}
```

## 🔧 Configuration

### JWT Settings (auth-service/application.yml)

```yaml
jwt:
  secret: your-256-bit-secret-key
  access-token-expiration: 900000    # 15 minutes
  refresh-token-expiration: 604800000 # 7 days
```

### Database (auth-service/application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: postgres
    password: postgres
```

## 📁 Project Structure

```
microservice-auth/
├── api-gateway/          # Spring Cloud Gateway
├── auth-service/         # Authentication service
├── mail-service/         # Email notification service
├── discovery-server/     # Netflix Eureka
├── common/               # Shared DTOs, Events, Enums
├── monitoring/           # Grafana, Loki, Promtail configs
├── docker-compose.dev.yml
├── Makefile              # Convenient commands
└── README.md
```

## 🛠 Make Commands

```bash
# Main Commands
make help              # Show all available commands
make start             # Start everything (infra + services)
make stop              # Stop all services
make stop-all          # Stop everything (services + infra + monitoring)
make restart           # Restart all services
make status            # Check service health status

# Build Commands
make build             # Build all Docker images
make rebuild           # Rebuild without cache
make clean             # Clean all containers, images, volumes

# Infrastructure
make start-infra       # Start DB, RabbitMQ, Redis, MailHog
make stop-infra        # Stop infrastructure

# Monitoring
make start-monitoring  # Start Grafana + Loki
make stop-monitoring   # Stop monitoring stack

# Logs
make logs              # View all container logs
make logs-auth         # Follow Auth Service logs
make logs-mail         # Follow Mail Service logs
make logs-discovery    # Follow Discovery Server logs
make logs-gateway      # Follow API Gateway logs
make logs-errors       # View error logs
```

## 🆕 Adding New Services

### Quick Guide

Let's say you want to add a `notification-service` that uses the `common` module:

#### 1. Create Module Structure

```bash
mkdir notification-service
mkdir -p notification-service/src/main/java/com/thebuilders/notification
mkdir -p notification-service/src/main/resources
```

#### 2. Create `notification-service/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.microservice</groupId>
        <artifactId>microservice-auth</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>notification-service</artifactId>
    <name>Notification Service</name>

    <dependencies>
        <!-- Spring Boot dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Common module (if needed) -->
        <dependency>
            <groupId>com.microservice</groupId>
            <artifactId>common</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>  <!-- IMPORTANT! -->
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 3. Update Root `pom.xml`

Add your module to the `<modules>` section:

```xml
<modules>
    <module>discovery-server</module>
    <module>api-gateway</module>
    <module>auth-service</module>
    <module>mail-service</module>
    <module>common</module>
    <module>notification-service</module>  <!-- NEW -->
</modules>
```

#### 4. Create `notification-service/Dockerfile`

**If your service uses `common` module:**

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy all POM files (required for Maven reactor)
COPY pom.xml .
COPY discovery-server/pom.xml ./discovery-server/
COPY api-gateway/pom.xml ./api-gateway/
COPY auth-service/pom.xml ./auth-service/
COPY mail-service/pom.xml ./mail-service/
COPY notification-service/pom.xml ./notification-service/
COPY common/pom.xml ./common/

# Copy source code (common + your service)
COPY common/src ./common/src
COPY notification-service/src ./notification-service/src

# Build
RUN apk add --no-cache maven && \
    mvn -pl common,notification-service -am clean package -DskipTests && \
    apk del maven

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/notification-service/target/*.jar app.jar
RUN mkdir -p /app/logs

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**If your service does NOT use `common`:**

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy all POM files
COPY pom.xml .
COPY discovery-server/pom.xml ./discovery-server/
COPY api-gateway/pom.xml ./api-gateway/
COPY auth-service/pom.xml ./auth-service/
COPY mail-service/pom.xml ./mail-service/
COPY notification-service/pom.xml ./notification-service/
COPY common/pom.xml ./common/

# Copy only your service source
COPY notification-service/src ./notification-service/src

# Build (no common needed)
RUN apk add --no-cache maven && \
    mvn -pl notification-service -am clean package -DskipTests && \
    apk del maven

# Runtime stage (same as above)
```

#### 5. Add to `docker-compose.yml`

```yaml
  notification-service:
    build:
      context: .
      dockerfile: notification-service/Dockerfile
    container_name: notification-service
    ports:
      - "8083:8083"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-server:8761/eureka/
      - LOG_PATH=/app/logs
    volumes:
      - ./logs:/app/logs
    networks:
      - career-portal-network
    depends_on:
      discovery-server:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
```

#### 6. Update `Makefile`

```makefile
build: ## Docker image'larını build et
	@docker-compose build discovery-server api-gateway auth-service mail-service notification-service

start: start-infra
	@docker-compose up -d discovery-server api-gateway auth-service mail-service notification-service
```

#### 7. Build & Run

```bash
make build
make start
```

### Key Points

- **Always copy ALL module POM files** (Maven reactor needs them)
- **Add `repackage` goal** to Spring Boot Maven Plugin
- **Copy common/src** only if your service uses common module
- **Use `mvn -pl common,your-service`** to build both modules together

## 📊 Monitoring

### Grafana Dashboard

Access Grafana at http://localhost:3001 (admin/admin123)

Pre-configured dashboard shows:
- All service logs in real-time
- Filter by service, log level
- Error tracking and alerts

### Log Query Examples (Loki)

```
# All auth-service logs
{job="app-logs", filename=~".*auth-service.*"}

# Only ERROR level
{job="app-logs"} |= "ERROR"

# Specific user actions
{job="app-logs"} |~ "User registered|User logged in"
```

## 🐳 Docker Architecture

All services are containerized with **multi-stage builds**:

1. **Build Stage**: Compiles Java code with Maven inside Docker (no local Maven needed)
2. **Runtime Stage**: Lightweight JRE-only image for production

```bash
# Build all Docker images (Maven runs inside containers)
make build

# Start production stack
make start

# Or use docker-compose directly
docker-compose up -d

# Stop everything
make stop-all
```

### Benefits

- **Zero local dependencies**: Only Docker required
- **Consistent builds**: Same environment for all developers
- **Small runtime images**: JRE-only (no JDK/Maven in production)
- **Fast rebuilds**: Docker layer caching optimizes build times

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot & Spring Cloud teams
- Netflix OSS (Eureka)
- Grafana Labs (Loki, Grafana)

---

**⭐ Star this repo if you find it useful!**
