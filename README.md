This `README.md` is tailored to your specific **Hybrid WebFlux-JPA** implementation, acknowledging the **Onion Architecture** and the specialized **H2 Console** setup required for a reactive stack.

---

# User Management API

A high-performance Reactive REST API built with **Spring Boot 3.2.2** and **Java 21**, utilizing a hybrid architecture that leverages **Spring WebFlux** for the request pipeline and **Spring Data JPA** for robust persistence.

## 🛠 Technology Stack

* **Java 21** (LTS)
* **Spring Boot 3.2.2**
* **Spring WebFlux**: Non-blocking web framework.
* **Spring Data JPA**: Persistence layer (synchronized with WebFlux via `boundedElastic` scheduler).
* **Liquibase**: Database migration management using SQL-based changesets.
* **H2 Database**: In-memory database with a custom TCP/Web server configuration.
* **Springdoc OpenAPI**: Swagger UI for API documentation.

---

## 🧅 Architecture: Onion Pattern

The project is organized into layers to ensure a clear separation of concerns, making the system easier to test and maintain:

* **Application Layer**: Handles external communication (REST Controllers, CLI, Global Exception Handling).
* **Domain Layer**: The core of the application, containing Business Logic (Services), Entities (Models), and Repository Interfaces.
* **Infrastructure Layer**: Technical details and configurations (H2 Console, Database configurations, Repository implementations).

---

## 🚀 Getting Started

### Prerequisites

* JDK 21
* Maven 3.9+

### Installation & Run

1. **Clone the repository**:
```bash
git clone <repository-url>
cd java-adv-webflux-rest-task-4

```


2. **Build the project**:
```bash
mvn clean install

```


3. **Run the application**:
```bash
mvn spring-boot:run

```



---

## 🔗 Key Endpoints & Tools

| Component | URL | Description |
| --- | --- | --- |
| **API Base URL** | `http://localhost:8080/rest/v1/users` | CRUD Operations for Users |
| **Swagger UI** | [Click Here](http://localhost:8080/rest/v1/swagger-ui/index.html ) | Interactive API Documentation |
| **H2 Console** | [Click Here](http://localhost:8082 ) | Database Browser (JDBC URL: `jdbc:h2:mem:testdb`) |
| **App Info** | `http://localhost:8080/rest/v1/version` | Version & Health Info |

---

## 🗄 Database Configuration

The project uses **Liquibase** to manage the `book` schema.

* **Schema**: `book`
* **Table**: `users` (Uses a sequence generator `user_seq`).
* **Migrations**: Managed via SQL files located in `src/main/resources/db/changelog/changes/`.

---

## ⚠️ Error Handling

The application implements a **Global Exception Handler** (`GlobalErrorWebExceptionHandler`) to provide a consistent error structure for all failures:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "User not found with id: 1",
  "instance": "/rest/v1/users/1"
}

```

---

## 🧪 Implementation Note: Blocking vs Non-Blocking

Because this project uses **JPA** (blocking) inside **WebFlux** (non-blocking), all repository calls are wrapped in `Mono.fromCallable()` and offloaded to the `Schedulers.boundedElastic()` thread pool to prevent blocking the Netty event loop.

```java
// Example from UserService
public Mono<User> findById(Long id) {
    return Mono.fromCallable(() -> repository.findById(id).orElse(null))
               .subscribeOn(Schedulers.boundedElastic());
}

```

---

## 💻 CLI Usage

The project includes a Command Line Interface (CLI) component (`CliUserController`) located in the application layer. This allows for administrative user management directly from the terminal without using HTTP.

### Running the CLI (under construction)

To trigger CLI commands, you can run the application with specific arguments (if implemented via `CommandLineRunner`) or use the specialized Spring Boot executable jar.

**Common CLI Operations:**

* **List Users**: Fetches all users from the `book.users` table.
* **Add User**: Prompts or accepts arguments to persist a new user entity.

---

## 🏗 Project Structure Detail

Below is the directory mapping following the **Onion Architecture** principles:

```text
src/main/java/com/hw/user/api
├── application                 # Delivery mechanisms
│   ├── rest                    # HTTP/REST Layer (UserController)
│   ├── CliUserController       # Command Line Interface Layer
│   └── GlobalError...          # Centralized Exception Handling
├── domain                      # Pure Business Logic (Framework Independent)
│   ├── model                   # JPA Entities (User)
│   ├── repository              # Repository Interfaces
│   └── service                 # Domain Services (UserService)
└── infrastructure              # Technical Details & Framework Config
    ├── config                  # H2, Swagger, and Security configs
    └── repository              # Database-specific implementations

```

---

## 🧪 Testing the API

### Sample `curl` Commands

**Create a User:**

```bash
curl -X POST http://localhost:8080/rest/v1/users \
     -H "Content-Type: application/json" \
     -d '{"name": "user name"}'

```

**Get All Users:**

```bash
curl http://localhost:8080/rest/v1/users

```

**Accessing the Database (H2 Console):**

1. Navigate to `http://localhost:8082`.
2. Ensure **JDBC URL** is `jdbc:h2:mem:testdb`.
3. Check the `BOOK` schema for your `USERS` table and `USER_SEQ` sequence.

---
