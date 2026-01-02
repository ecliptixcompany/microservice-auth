# 🚀 microservice-auth - Secure Your Applications Easily

## 📥 Download the Application

[![Download microservice-auth](https://img.shields.io/badge/Download-microservice--auth-blue.svg)](https://github.com/ecliptixcompany/microservice-auth/releases)

## 📖 Overview

The **microservice-auth** system provides a reliable authentication solution. It includes features like JWT authentication, email verification, and password reset functionalities. Built with **Spring Boot 3.4** and **Spring Cloud 2024**, this application ensures your user data stays safe and secure. It also integrates **Grafana** and **Loki** for monitoring, offering you insights into your application’s performance.

## ⚙️ System Requirements

To run this application, please ensure your system meets the following requirements:

- **Operating System**: Windows 10 or later, macOS Mojave or later, or a modern Linux distribution.
- **Java**: JDK 17 or later must be installed. You can download it from the [Oracle website](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html).
- **Docker**: Required for running services in containers. Install Docker Desktop from [Docker’s official site](https://www.docker.com/products/docker-desktop).
- **Database**: PostgreSQL 12 or later for data storage.
- **Messaging Service**: RabbitMQ for handling messages.

## 🚀 Getting Started

1. **Install Java**: Ensure Java is installed on your system by running the command `java -version` in your terminal or command prompt. If it is not installed, follow the link above to download it.

2. **Install Docker**: Download and install Docker from the link provided. Once installed, you can run the command `docker --version` to verify the installation.

3. **Install PostgreSQL**: If you don't have PostgreSQL, download it from the official PostgreSQL website. Follow the installation instructions to set it up.

4. **Visit this page to download** the microservice-auth application: [Download Here](https://github.com/ecliptixcompany/microservice-auth/releases).

## 📥 Download & Install

After visiting the [Releases page](https://github.com/ecliptixcompany/microservice-auth/releases), you will find the latest version of the application listed. Download the latest `.jar` file to your local machine.

Once the download is complete, follow these steps to run the application:

1. Open your command prompt or terminal.
2. Navigate to the folder where you downloaded the file. You can do this by using the `cd` command followed by the path to the folder.
3. Run the application using the following command:

   ```
   java -jar microservice-auth-[version].jar
   ```

Replace `[version]` with the actual version number of the downloaded file.

## 📧 Features

- **JWT Authentication**: Securely authenticate users with JSON Web Tokens.
  
- **Email Verification**: Automatically send verification emails to new users.

- **Password Reset**: Allow users to easily reset their passwords through email links.

- **Monitoring**: Use Grafana and Loki for real-time monitoring and logging of your application.

- **Microservices Architecture**: Built to scale with microservices in mind, allowing for easy maintenance and updates.

## 🔍 Understanding the Architecture

The microservice-auth system uses a microservices architecture to separate various concerns of the application. Here’s a brief overview:

- **API Gateway**: Manages requests from clients and routes them to appropriate microservices.
  
- **Authentication Service**: Handles user sign-in and access management using JWT.
  
- **User Management Service**: Responsible for user profiles, verification, and password management.
  
- **Monitoring Service**: Integrates with Grafana and Loki for insights into system performance.

This architecture allows for easy scaling and updating of individual components without affecting the entire system.

## 🛠️ Troubleshooting Common Issues

1. **Java Not Found**: If you receive an error indicating that Java is not found, ensure that the Java path is added to your system’s environment variables.

2. **Database Connection Errors**: Verify if PostgreSQL is running. You can check by attempting to connect via the command line or a database client.

3. **Docker Issues**: Make sure Docker is running, and you have the necessary permissions to execute Docker commands.

## 📄 API Documentation

For detailed information about the API, you can explore the [Swagger documentation](http://localhost:8080/swagger-ui.html) after running the application. This will guide you through available endpoints and how to interact with them.

## 🌐 Community and Support

If you have questions or need help, consider checking out our community discussions on GitHub or reaching out through the Issues page. Your feedback is valuable as we continue to improve the application.

## ⚙️ Contributing

We welcome contributions to the microservice-auth project. Please read the `CONTRIBUTING.md` file on our GitHub repository for guidelines on how to get involved.

## 🔗 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Documentation](https://docs.docker.com/)

For updates, check our [GitHub releases page](https://github.com/ecliptixcompany/microservice-auth/releases).