package com.hw.user.api.application.rest;

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
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@Configuration
@Order(-2)
@Slf4j
public class GlobalErrorWebExceptionHandler
        extends DefaultErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties webProperties,     // Inject WebProperties
            ApplicationContext applicationContext,
            ServerProperties serverProperties, // Inject ServerProperties to get ErrorProperties
            ServerCodecConfigurer configurer
    ) {

        super(errorAttributes,
                webProperties.getResources(),
                serverProperties.getError(),
                applicationContext);
// 2. This is the crucial part that fixes the "Property 'messageWriters' is required" error
        this.setMessageWriters(configurer.getWriters());
        this.setMessageReaders(configurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(
            ErrorAttributes errorAttributes) {

        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {

        Throwable error = getError(request);
        ProblemDetail problem;
        log.error("Request failed: {} {}", request.method(), request.path(), error);
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
