package dev.franjaresc.wolfandfox.route.domain;

import java.util.UUID;

public record RouteClosureId(UUID value) {

    public RouteClosureId {
        if (value == null) {
            throw new InvalidRouteClosureException("Route closure id is required");
        }
    }

    public static RouteClosureId newId() {
        return new RouteClosureId(UUID.randomUUID());
    }
}
