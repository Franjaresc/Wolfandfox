package dev.franjaresc.wolfandfox.route.application.port.in;

import dev.franjaresc.wolfandfox.route.application.port.PageQuery;
import dev.franjaresc.wolfandfox.route.application.port.PageResult;
import dev.franjaresc.wolfandfox.route.application.port.RouteClosureFilter;
import dev.franjaresc.wolfandfox.route.domain.RouteClosure;

public interface FindRouteClosuresQuery {

    /** Returns closures ordered by closure date (newest first), then by route code. */
    PageResult<RouteClosure> find(RouteClosureFilter filter, PageQuery page);
}
