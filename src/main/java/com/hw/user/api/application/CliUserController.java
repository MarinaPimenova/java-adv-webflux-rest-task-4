package com.hw.user.api.application;

import com.hw.user.api.domain.model.User;
import com.hw.user.api.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CliUserController {
    private final UserService userService;

    public void createUser(User user) {
        userService.createUser(user);
    }
}
