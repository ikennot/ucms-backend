# University Concern Management System (UCMS) - Backend

This is the backend service for the University Concern Management System, built with Spring Boot.

## 🚀 Quick Start

1.  **Configure Database**: Update `src/main/resources/application.yaml` with your Supabase credentials.
2.  **Build**: Run `mvn clean install` (or `./mvnw clean install`).
3.  **Run**: Run `mvn spring-boot:run` (or `./mvnw spring-boot:run`).
4.  **Access**: Open `http://localhost:8080`.

## Prerequisites

Before you can run this project, ensure you have the following installed:

*   **Java Development Kit (JDK) 21**: This project requires Java 21.
*   **PostgreSQL (Supabase)**: A running PostgreSQL instance (Supabase) for data storage.
*   **Maven 3.9.12**: Required to build and manage project dependencies.
*   **IDE**: IntelliJ IDEA, Eclipse, or VS Code (with Lombok plugin installed).

## Setup Instructions

### 1. Database Configuration (Supabase)

This project uses **Supabase (PostgreSQL)**. Get your connection details from the Supabase Dashboard under **Project Settings > Database**.

Update `src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: ucms-backend
  datasource:
    url: jdbc:postgresql://db.[YOUR-PROJECT-REF].supabase.co:5432/postgres
    username: postgres
    password: [YOUR-PASSWORD]
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

> **Note:** If you are using Supabase's connection pooling, use port `6543` instead of `5432`.

### 2. Install Dependencies

Run the following command to download dependencies:

```bash
./mvnw clean install
```
(On Windows, use `mvnw.cmd clean install`)

## Running the Application

To start the application, run:

```bash
./mvnw spring-boot:run
```

The server will start on `http://localhost:8080` by default.

## Features Included

*   **Spring Data JPA**: For database interactions.
*   **Spring Security**: For authentication and authorization.
*   **Spring Validation**: For input data validation.
*   **Lombok**: To reduce boilerplate code.
*   **PostgreSQL**: As the primary database.

## Development

*   **Lombok**: Ensure your IDE has the Lombok plugin installed and annotation processing enabled.
*   **Tests**: You can run tests using `./mvnw test`.

### Keeping Your Code Up to Date

To get the latest changes from the development branch, run:

```bash
git pull origin development
```
