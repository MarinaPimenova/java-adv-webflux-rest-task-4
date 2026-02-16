package com.hw.user.api.domain.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class VersionService {
    private final String version;
    private final String name;

    public VersionService(@Value("${info.app.version}") String version,
                          @Value("${info.app.name}") String name) {
        this.version = version;
        this.name = name;
    }

    public Mono<String> getVersion() {
        return Mono.just(name + " : " + version);
    }

}

