# Treasurer API

## Overview

Treasurer API is a RESTful web service for managing financial transactions and user accounts. Built with Scala and Akka HTTP, it provides secure user authentication with JWT, user management, and transaction tracking. The application uses PostgreSQL for data storage, Slick for database access, Flyway for migrations, and PureConfig for configuration management. It’s designed for scalability and maintainability, with separate execution contexts for HTTP, database, and CPU-intensive tasks.

## Features

- **User Management**: Register, log in, retrieve, update, and delete user accounts.
- **Transaction Management**: Create, retrieve, update, and delete financial transactions.
- **Authentication**: Secure JWT-based authentication for protected endpoints.
- **Database**: PostgreSQL with Slick for ORM and Flyway for schema migrations.
- **Configuration**: Managed via PureConfig with refined types for safety.
- **Logging**: SLF4J with Logback for comprehensive logging.
- **Testing**: Supports unit and property-based testing with ScalaTest and ScalaCheck.

## Prerequisites

- **Java**: JDK 11 or higher
- **Scala**: 2.13.x
- **SBT**: 1.10.x or higher
- **PostgreSQL**: 16.4 (provided via Docker)
- **Docker**: 20.10 or higher
- **Docker Compose**: 2.0 or higher
- **OS**: Linux, macOS, or Windows

## Setup

### 1. Clone the Repository

```bash
    git clone https://github.com/KimDencka/treasurer.git
    cd treasurer

```

### 2. Configure PostgreSQL

The application uses a PostgreSQL database, which is set up via Docker using the provided `docker-compose.yml` file.

1. Ensure Docker and Docker Compose are installed and running.
2. Start the PostgreSQL container:
    
    ```bash
    docker-compose up -d
    
    ```
    
    This will:
    
    - Pull the `postgres:16.4` image if not already present.
    - Create a database named `treasurer`.
    - Set the database user to `postgres` with password `postgres`.
    - Expose PostgreSQL on `localhost:5432`.
3. Verify the container is running:
    
    ```bash
    docker ps
    
    ```
    
4. Ensure `src/main/resources/application.conf` matches the database credentials:
    
    ```
    postgres {
      jdbcUrl = "jdbc:postgresql://localhost:5432/treasurer"
      user = "postgres"
      password = "postgres"
      driver = "org.postgresql.Driver"
      schema = "public"
      connections {
        poolSize = 10
      }
    }
    
    ```
    

### 3. Configure the Application

Edit `src/main/resources/application.conf` to match your environment:

```
http {
  host = "0.0.0.0"
  port = 8080
}

postgres {
  jdbcUrl = "jdbc:postgresql://localhost:5432/treasurer"
  user = "postgres"
  password = "postgres"
  driver = "org.postgresql.Driver"
  schema = "public"
  connections {
    poolSize = 10
  }
}

execution-contexts {
  http { threads = 10 }
  database { threads = 10 }
  cpu { threads = 4 }
}

jwt {
  secret = "your-secret-key"
  expiration = "1h"
}

```

### 4. Database Migrations

The application uses Flyway for database migrations. Migration scripts are located in `src/main/resources/db/migration`. Ensure the following files exist:

- `V1__create_tables.sql`
    
    ```sql
    CREATE TABLE users (
      id UUID PRIMARY KEY,
      username VARCHAR NOT NULL,
      password VARCHAR NOT NULL,
      created_at TIMESTAMP NOT NULL);
    
    CREATE TABLE transactions (
      id UUID PRIMARY KEY,
      user_id UUID NOT NULL,
      amount DECIMAL(15, 2) NOT NULL,
      type VARCHAR(50) NOT NULL,
      description TEXT,
      created_at TIMESTAMP NOT NULL);
    
    CREATE INDEX idx_transactions_user_id ON transactions(user_id);
    ALTER TABLE users ADD CONSTRAINT unique_username UNIQUE (username);
    ALTER TABLE transactions ADD CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    
    ```
    

Flyway will automatically apply these migrations on startup.

## Running the Application

1. Compile and run the application:
    
    ```bash
    sbt run
    
    ```
    
2. The server will start at `http://localhost:8080/api` (or the configured host/port).

## API Endpoints

All endpoints are prefixed with `/api`.

### Authentication

- **POST /api/auth/register**
    - Register a new user.
    - Request Body: `{"username": "string", "password": "string"}`
    - Response: `201 Created` with `{"id": "UUID", "username": "string", "createdAt": "ISO-8601 timestamp"}`
    - Errors: `400 Bad Request` if username is taken or input is invalid.
- **POST /api/auth/login**
    - Log in and receive a JWT token.
    - Request Body: `{"username": "string", "password": "string"}`
    - Response: `200 OK` with `{"token": "JWT string"}`
    - Errors: `401 Unauthorized` for invalid credentials.

### Users (Protected)

- **GET /api/users**
    - Get the authenticated user's details.
    - Headers: `Authorization: Bearer <JWT>`
    - Response: `200 OK` with `{"id": "UUID", "username": "string", "createdAt": "ISO-8601 timestamp"}`
    - Errors: `401 Unauthorized` if token is invalid, `404 Not Found` if user not found.
- **GET /api/users/{username}**
    - Get user by username.
    - Headers: `Authorization: Bearer <JWT>`
    - Response: `200 OK` with `{"id": "UUID", "username": "string", "createdAt": "ISO-8601 timestamp"}`
    - Errors: `401 Unauthorized`, `404 Not Found`.
- **PUT /api/users/{id}**
    - Update user by ID.
    - Headers: `Authorization: Bearer <JWT>`
    - Request Body: `{"username": "string", "password": "string"}`
    - Response: `200 OK` with updated user
    - Errors: `401 Unauthorized`, `404 Not Found`.
- **DELETE /api/users/{id}**
    - Delete user by ID.
    - Headers: `Authorization: Bearer <JWT>`
    - Response: `204 No Content`
    - Errors: `401 Unauthorized`, `404 Not Found`.

### Transactions (Protected)

- **POST /api/transactions**
    - Create a new transaction.
    - Headers: `Authorization: Bearer <JWT>`
    - Request Body: `{"amount": number, "description": "string", "type": "income|expense"}`
    - Response: `201 Created` with `{"id": "UUID", "amount": number, "description": "string", "type": "string", "createdAt": "ISO-8601 timestamp"}`
    - Errors: `401 Unauthorized`, `400 Bad Request`.
- **GET /api/transactions**
    - Get all transactions for the authenticated user.
    - Headers: `Authorization: Bearer <JWT>`
    - Response: `200 OK` with array of transactions
    - Errors: `401 Unauthorized`.
- **GET /api/transactions/{id}**
    - Get transaction by ID.
    - Headers: `Authorization: Bearer <JWT>`
    - Response: `200 OK` with transaction
    - Errors: `401 Unauthorized`, `404 Not Found`.
- **PUT /api/transactions/{id}**
    - Update transaction by ID.
    - Headers: `Authorization: Bearer <JWT>`
    - Request Body: `{"amount": number, "description": "string", "type": "income|expense"}`
    - Response: `200 OK` with updated transaction
    - Errors: `401 Unauthorized`, `404 Not Found`.
- **DELETE /api/transactions/{id}**
    - Delete transaction by ID.
    - Headers: `Authorization: Bearer <JWT>`
    - Response: `204 No Content`
    - Errors: `401 Unauthorized`, `404 Not Found`.

## Testing

To run only IT tests we need `PostgreSQL`:

```bash
    docker-compose -f docker-compose-test.yml up -d
```

Run tests using ScalaTest and ScalaCheck:

```bash

# For Unit tests
sbt test

# For IT tests
sbt 'integration/test'

```

## Stopping the Application

The server runs until manually stopped (e.g., Ctrl+C). For graceful shutdown, implement additional logic in `Main.scala`.