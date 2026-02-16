package com.hw.user.api.application.rest;

import com.hw.user.api.domain.service.VersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;

    @GetMapping(value = "/rest/v1/version", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> getVersion() {
        return versionService.getVersion();
    }
}

