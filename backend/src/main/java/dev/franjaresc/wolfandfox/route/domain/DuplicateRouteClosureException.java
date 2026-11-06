package dev.franjaresc.wolfandfox.route.domain;

import java.time.LocalDate;

/** A route can be closed only once per date. */
public class DuplicateRouteClosureException extends RuntimeException {

    public DuplicateRouteClosureException(RouteCode routeCode, LocalDate closureDate) {
        super("Route %s is already closed for %s".formatted(routeCode.value(), closureDate));
    }
}
