package com.hw.user.api.application.rest;

import com.hw.user.api.domain.model.User;
import com.hw.user.api.domain.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/rest/v1/users")
@RequiredArgsConstructor
public class GetUserController {

    private final UserService userService;

    @Operation(summary = "Get all users")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<User>> getUsers() {
        return userService.findAll();
    }

    @Operation(summary = "Get user")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<User> getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

}