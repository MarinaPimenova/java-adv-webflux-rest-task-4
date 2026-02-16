package com.hw.user.api.domain.service;

import com.hw.user.api.domain.model.User;
import com.hw.user.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;

    public Mono<User> findById(Long id) {
        return Mono.fromCallable(() -> repository.findById(id).orElse(null))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<User>> findAll() {
        return Mono.fromCallable(repository::findAll)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<User> save(User user) {
        return Mono.fromCallable(() -> repository.saveAndFlush(user))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<User> update(Long id, User userDetails) {
        return Mono.fromCallable(() -> {
            User existingUser = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
            existingUser.setName(userDetails.getName());
            return repository.saveAndFlush(existingUser);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteById(Long id) {
        return Mono.<Void>fromRunnable(() -> repository.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public void createUser(User user) {
    }
}
