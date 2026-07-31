package com.hw.user.api.application.rest;

import org.springframework.http.ProblemDetail;

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

