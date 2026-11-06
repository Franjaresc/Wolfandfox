package dev.franjaresc.wolfandfox.route.application.port.in;

import dev.franjaresc.wolfandfox.route.domain.RouteClosure;
import dev.franjaresc.wolfandfox.route.domain.RouteClosureId;

public interface GetRouteClosureQuery {

    /**
     * Returns one closure.
     *
     * @throws dev.franjaresc.wolfandfox.route.domain.RouteClosureNotFoundException if none has that id
     */
    RouteClosure get(RouteClosureId id);
}
