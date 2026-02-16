package com.hw.user.api.infrastructure.repository;

import com.hw.user.api.domain.repository.UserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public interface H2DbUserRepository extends UserRepository {
}
