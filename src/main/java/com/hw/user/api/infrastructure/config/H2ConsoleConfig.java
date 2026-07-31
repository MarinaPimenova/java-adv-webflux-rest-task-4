package com.hw.user.api.infrastructure.config;

import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import java.sql.SQLException;

@Configuration
@Profile("!prod")
public class H2ConsoleConfig {
    private Server webServer;

    @EventListener(org.springframework.context.event.ContextRefreshedEvent.class)
    public void start() throws java.sql.SQLException {
        // Starts the H2 Console UI on port 8082
        this.webServer = org.h2.tools.Server.createWebServer("-webPort", "8082", "-tcpAllowOthers").start();
    }

    @EventListener(org.springframework.context.event.ContextClosedEvent.class)
    public void stop() {
        this.webServer.stop();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @Profile("dev")
    public Server h2WebConsole() throws SQLException {
        // Access at http://localhost:8082
        return Server.createWebServer("-webPort", "8082", "-tcpAllowOthers");
    }
}