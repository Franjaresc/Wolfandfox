package dev.franjaresc.wolfandfox.route.application.port;

import dev.franjaresc.wolfandfox.route.domain.RouteCode;
import java.time.LocalDate;

/** Optional criteria for listing closures. A null field does not filter. */
public record RouteClosureFilter(LocalDate closureDate, RouteCode routeCode) {
}
