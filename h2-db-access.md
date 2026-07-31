# **Spring WebFlux with JPA (Blocking DB access)**
While this is technically possible, it negates some of the performance benefits of 
the reactive stack because JPA is inherently blocking.

However, since we want to proceed with this "Hybrid" approach, 
here is the complete configuration.

### 1. Is it OK to use JPA with WebFlux?

**Yes, with a caveat.** Since JPA is blocking, when you call a repository method from a WebFlux `Controller`, you must ensure that logic is wrapped in `publishOn(Schedulers.boundedElastic())` or handled on a separate thread pool. If you call JPA directly on the Netty event loop, you will "starve" the server, and performance will drop drastically.

---

### 2. Updated `pom.xml`

You need to add `spring-boot-starter-data-jpa` and ensure Liquibase is correctly included.

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-liquibase</artifactId>
    </dependency>

    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>

```

---

### 3. Updated `application.yml`

The JDBC URL uses `mem:testdb` with `DB_CLOSE_DELAY=-1` to keep the data alive across different connection sessions.

```yaml
spring:
  datasource:
    # Liquibase and JPA will use this connection
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: password
  jpa:
    hibernate:
      ddl-auto: validate # Let Liquibase handle schema, JPA only validates
    show-sql: true
    database-platform: org.hibernate.dialect.H2Dialect
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    enabled: true
  h2:
    console:
      # Note: This won't work automatically in WebFlux; see the custom filter below
      enabled: true
      path: /h2-console

```

---

### 4. Custom Configuration for H2 UI (WebFilter)

Because WebFlux doesn't use Servlets, the standard H2 Console doesn't work. Instead of a Filter, it is more reliable in Spring Boot 3 to start a **TCP Server** and use a **Redirect Controller** or a custom `RouterFunction`.

A cleaner way to access the H2 UI in WebFlux is starting a separate Web Server for it:

```java
@Configuration
@Profile("dev")
public class H2ConsoleConfig {
    private org.h2.tools.Server webServer;

    @EventListener(org.springframework.context.event.ContextRefreshedEvent.class)
    public void start() throws java.sql.SQLException {
        // Starts the H2 Console UI on port 8082
        this.webServer = org.h2.tools.Server.createWebServer("-webPort", "8082", "-tcpAllowOthers").start();
    }

    @EventListener(org.springframework.context.event.ContextClosedEvent.class)
    public void stop() {
        this.webServer.stop();
    }
}

```

*Access the UI at `http://localhost:8082`. Use JDBC URL: `jdbc:h2:mem:testdb`.*

---

### 5. Liquibase Changelog

Create `src/main/resources/db/changelog/db.changelog-master.xml`:

```xml
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="20240101-1" author="user">
        <createTable tableName="test_entity">
            <column name="id" type="BIGINT">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(255)"/>
        </createTable>
    </changeSet>
</databaseChangeLog>

```

---

### 6. Sample Repository & Test Logic

Here is how you bridge the blocking JPA calls into a WebFlux-friendly `Mono`.

**The Entity:**

```java
@Entity
@Table(name = "test_entity")
@Data
public class TestEntity {
    @Id
    private Long id;
    private String name;
}

```

**The Repository:**

```java
@Repository
public interface TestRepository extends JpaRepository<TestEntity, Long> {}

```

**The Service (How to call it safely):**

```java
@Service
@RequiredArgsConstructor
public class TestService {
    private final TestRepository repository;

    public Mono<TestEntity> findById(Long id) {
        // Wrap the blocking JPA call in a separate thread pool
        return Mono.fromCallable(() -> repository.findById(id).orElse(null))
                   .subscribeOn(Schedulers.boundedElastic());
    }
}

```

---
To verify that your H2 server is running correctly, you can use standard terminal tools to check the network port and the service status.

### 1. Check Port Activity

Since you configured the H2 Console to run on port **8082**, you can check if your machine is listening on that port.

**On Windows (PowerShell/CMD):**

```powershell
netstat -ano | findstr :8082

```

If the server is up, you will see a line with `LISTENING` and a Process ID (PID).

**On Linux/Mac:**

```bash
lsof -i :8082

```

---

### 2. Verify via cURL

You can send a simple HTTP request to the console endpoint. Even if it returns a login page or an error, a response means the server is alive.

```bash
curl -I http://localhost:8082

```

A successful response (usually a `200 OK` or `302 Found`) confirms the H2 Web Server started by your `H2ConsoleConfig` is active.

---

### 3. Log Inspection

Check your Spring Boot logs during startup. If you added the `System.out.println` or a `log.info` inside your `start()` method, you should see it in the console:

> `H2 Console started on port 8082`

---

### Summary of Component Connections

To ensure your **JPA/Liquibase** data and the **H2 UI** match, use these settings in the browser login screen:

| Field | Value |
| --- | --- |
| **Driver Class** | `org.h2.Driver` |
| **JDBC URL** | `jdbc:h2:mem:testdb` |
| **User Name** | `sa` |
| **Password** | `password` (as set in your YAML) |

[Building a Spring Boot App with TCP Server & H2 Database](https://www.youtube.com/watch?v=cbjfvUoiJAI)
This video demonstrates how to set up an H2 TCP server within a Spring Boot application, which is the exact method you are using to allow external access and UI visibility for your in-memory database.
