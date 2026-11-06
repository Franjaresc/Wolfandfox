package dev.franjaresc.wolfandfox.route.config;

import dev.franjaresc.wolfandfox.route.application.port.out.CurrentUserPort;
import dev.franjaresc.wolfandfox.route.application.port.out.RouteClosureRepositoryPort;
import dev.franjaresc.wolfandfox.route.application.service.RouteClosureService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class RouteModuleConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RouteClosureService routeClosureService(
            RouteClosureRepositoryPort repository, CurrentUserPort currentUser, Clock clock) {
        return new RouteClosureService(repository, currentUser, clock);
    }
}
