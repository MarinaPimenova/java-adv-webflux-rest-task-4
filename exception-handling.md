
# ✅ Best Practice Recommendation (Spring Boot 3.2.2 + WebFlux)

### **Use ONE centralized `ErrorWebExceptionHandler`**

✔ Handles **404 (unknown URLs)**
✔ Handles **Throwable, IllegalArgumentException, validation errors**
✔ Produces **consistent JSON / ProblemDetail**
✔ Works for **functional + annotated controllers**
✔ Replaces Whitelabel completely

> ❌ Avoid multiple ad-hoc handlers
> ❌ Avoid `@ControllerAdvice` (MVC-centric, limited in WebFlux)

---

# ⭐ Why this is the best approach

| Approach                                     | Verdict           | Why                                            |
| -------------------------------------------- | ----------------- | ---------------------------------------------- |
| `@ControllerAdvice`                          | ❌ Not recommended | Doesn’t catch routing & static resource errors |
| Multiple handlers                            | ❌ Bad             | Order conflicts, hard to debug                 |
| Simple `ErrorWebExceptionHandler`            | ⚠ OK              | Too low-level                                  |
| **Extend `DefaultErrorWebExceptionHandler`** | ✅ **Best**        | Official, future-proof, full coverage          |

Spring itself uses `DefaultErrorWebExceptionHandler` — you should **extend**, not replace it.

---

# 🧱 Architecture Overview

```
Incoming request
      ↓
Routing (WebFlux)
      ↓
Controller / Handler
      ↓
Exception thrown
      ↓
DefaultErrorWebExceptionHandler (YOUR override)
      ↓
Consistent API error response
```

---

# 🧩 Production-Ready Implementation

## 1️⃣ Define a standard error model (ProblemDetail)

Spring Boot 3.x supports RFC 7807 out of the box:

```java
public final class ApiError {

    public static ProblemDetail notFound(String path) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setTitle("Resource Not Found");
        problem.setDetail("No handler found for " + path);
        problem.setProperty("path", path);
        return problem;
    }

    public static ProblemDetail badRequest(String message) {
        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setTitle("Bad Request");
        problem.setDetail(message);
        return problem;
    }

    public static ProblemDetail internalError() {
        ProblemDetail problem = ProblemDetail.forStatus(500);
        problem.setTitle("Internal Server Error");
        problem.setDetail("Unexpected server error");
        return problem;
    }
}
```

---

## 2️⃣ Centralized WebFlux Error Handler (CORE PIECE)

```java
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

@Configuration
@Order(-2)
public class GlobalErrorWebExceptionHandler
        extends DefaultErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            ApplicationContext applicationContext) {

        super(errorAttributes, new WebProperties.Resources(), applicationContext);
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(
            ErrorAttributes errorAttributes) {

        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {

        Throwable error = getError(request);
        ProblemDetail problem;

        if (error instanceof IllegalArgumentException ex) {
            problem = ApiError.badRequest(ex.getMessage());
        }
        else if (error instanceof org.springframework.web.reactive.resource.NoResourceFoundException) {
            problem = ApiError.notFound(request.path());
        }
        else {
            problem = ApiError.internalError();
        }

        return ServerResponse
                .status(HttpStatus.valueOf(problem.getStatus()))
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyValue(problem);
    }
}
```

---

## 3️⃣ (Optional) Remove stacktraces from responses

```yaml
server:
  error:
    include-stacktrace: never
    include-message: never
```

---

## 4️⃣ (Optional) Log errors centrally

```java
private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
    Throwable error = getError(request);

    log.error("Request failed: {} {}", request.method(), request.path(), error);

    ...
}
```

---

# 🎯 What This Handles Automatically

✔ `IllegalArgumentException` → **400**
✔ `NoResourceFoundException` (unknown URL) → **404**
✔ `WebExchangeBindException` → **400**
✔ Any `Throwable` → **500**
✔ Swagger missing URLs
✔ Static resource errors

---

# 🚫 What NOT to do

```java
@ControllerAdvice   // ❌ MVC-centric
@ExceptionHandler   // ❌ Not global in WebFlux
```

They do **not** catch:

* Router errors
* Static resource 404
* WebFlux functional endpoints

---

# 🧪 Example Responses

### Unknown URL

```json
{
  "type": "about:blank",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "No handler found for /wrong",
  "path": "/wrong"
}
```

### Illegal Argument

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "id must be positive"
}
```

---

# 🏁 Final Verdict

> ✅ **Extend `DefaultErrorWebExceptionHandler`**
> ✅ **Return `ProblemDetail`**
> ✅ **One handler for everything**
> ❌ No Whitelabel
> ❌ No scattered handlers

This is the **cleanest, most future-proof** solution for Spring Boot **3.2.x WebFlux** apps.

--
```java
import org.springframework.boot.autoconfigure.web.ServerProperties;
// ... other imports

@Configuration
@Order(-2)
@Slf4j
public class GlobalErrorWebExceptionHandler extends DefaultErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties webProperties,     // Inject WebProperties
            ServerProperties serverProperties, // Inject ServerProperties to get ErrorProperties
            ApplicationContext applicationContext) {

        super(errorAttributes, 
              webProperties.getResources(), 
              serverProperties.getError(), // This provides the ErrorProperties
              applicationContext);
    }

    // ... your renderErrorResponse logic remains the same
}
```
Why this works

    ServerProperties: This is a standard Spring Boot bean that holds configuration like server.port and server.error.include-stacktrace. By calling .getError(), you get the exact ErrorProperties the superclass expects.

    WebProperties: Since Spring Boot 2.4+, the resource handling was moved here. Injecting it ensures your exception handler is aware of your static resource configurations.

A Quick Reality Check on Your Approach

By extending DefaultErrorWebExceptionHandler and using @Order(-2), you are effectively replacing the default Spring Boot error logic.

    Pros: You have a single "Catch All" for the entire Netty pipeline.

    Cons: You lose the convenience of @ExceptionHandler methods in your controllers. If you add a @RestControllerAdvice later, it might be ignored because this handler has higher precedence (Order(-2)).

Pro-Tip: The "Lighter" Alternative

If you don't actually need to override the entire routing logic of the error handler, you can keep the default one and just provide a bean of type ErrorAttributes. Spring's default handler will then use your custom attributes to render the JSON.

However, since you've already written the renderErrorResponse logic with ProblemDetail, the constructor fix above is your fastest path to a working app.

--


---

# Custom Exception Handling in Spring WebFlux

This document outlines the implementation of a global, low-level exception handler using `DefaultErrorWebExceptionHandler` 
in a **Spring Boot 3.2.2** and **Java 21** environment.

## 1. Global Error Handler Implementation

The `GlobalErrorWebExceptionHandler` catches all errors across the entire Netty pipeline, including those occurring in WebFilters or due to missing routes (404).

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@Configuration
@Order(-2) // High priority to override the default handler
@Slf4j
public class GlobalErrorWebExceptionHandler extends DefaultErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties webProperties,
            ServerProperties serverProperties, // Injected to access ErrorProperties
            ApplicationContext applicationContext) {

        super(errorAttributes,
                webProperties.getResources(),
                serverProperties.getError(), 
                applicationContext);
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        ProblemDetail problem;

        log.error("Request failed: {} {}", request.method(), request.path(), error);

        // Map exceptions to RFC 7807 ProblemDetails
        if (error instanceof IllegalArgumentException ex) {
            problem = ApiError.badRequest(ex.getMessage());
        } else if (error instanceof org.springframework.web.reactive.resource.NoResourceFoundException) {
            problem = ApiError.notFound(request.path());
        } else {
            problem = ApiError.internalError();
        }

        return ServerResponse
                .status(HttpStatus.valueOf(problem.getStatus()))
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyValue(problem);
    }
}

```

---

## 2. Configuration (`application.yml`)

Use the `server.error` properties to control the visibility of sensitive information like stack traces and exception messages.

### **Development Profile**

Enable full transparency for easier debugging.

```yaml
server:
  error:
    include-stacktrace: always
    include-message: always
    include-binding-errors: always
    problem-details:
      enabled: true

```

### **Production Profile**

Strictly limit output to prevent information leakage.

```yaml
server:
  error:
    include-stacktrace: never
    include-message: never
    include-binding-errors: never
    problem-details:
      enabled: true

```

---

## 3. Configuration Properties Overview

| Property | Options | Description |
| --- | --- | --- |
| `include-stacktrace` | `never`, `always`, `on_param` | Controls if the Java stack trace is added to the error attributes. |
| `include-message` | `never`, `always`, `on_param` | Controls if the exception message (e.g., `ex.getMessage()`) is included. |
| `problem-details.enabled` | `true`, `false` | Enables support for the `application/problem+json` media type. |

---

## 4. Key Considerations

* **Order Preference**: By setting `@Order(-2)`, this handler takes precedence over the standard `@RestControllerAdvice`. If you wish to use both, ensure your business exceptions are handled here or adjust the order.
* **Security**: Always set `include-stacktrace: never` in production environments.
* **Logging**: While we hide details from the user, the `log.error` statement in `renderErrorResponse` ensures that the full stack trace is preserved in your server logs for troubleshooting.



