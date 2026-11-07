package dev.franjaresc.wolfandfox.route.config;

import dev.franjaresc.wolfandfox.route.application.port.out.CurrentUserPort;
import dev.franjaresc.wolfandfox.route.application.port.out.RouteClosureRepositoryPort;
import dev.franjaresc.wolfandfox.route.application.service.RouteClosureService;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class RouteModuleConfig {

    /** Its zone defines the business day, for example whether a closure date is still in the future. */
    @Bean
    Clock clock(@Value("${wolfandfox.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }

    @Bean
    RouteClosureService routeClosureService(
            RouteClosureRepositoryPort repository, CurrentUserPort currentUser, Clock clock) {
        return new RouteClosureService(repository, currentUser, clock);
    }
}
