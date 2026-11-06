package dev.franjaresc.wolfandfox.route.application.port.out;

import dev.franjaresc.wolfandfox.route.application.port.PageQuery;
import dev.franjaresc.wolfandfox.route.application.port.PageResult;
import dev.franjaresc.wolfandfox.route.application.port.RouteClosureFilter;
import dev.franjaresc.wolfandfox.route.domain.RouteClosure;
import dev.franjaresc.wolfandfox.route.domain.RouteClosureId;
import dev.franjaresc.wolfandfox.route.domain.RouteCode;
import java.time.LocalDate;
import java.util.Optional;

public interface RouteClosureRepositoryPort {

    boolean existsByRouteCodeAndClosureDate(RouteCode routeCode, LocalDate closureDate);

    /**
     * Stores a new closure.
     *
     * @throws dev.franjaresc.wolfandfox.route.domain.DuplicateRouteClosureException if a concurrent
     *     request closed the same route and date first
     */
    RouteClosure save(RouteClosure closure);

    Optional<RouteClosure> findById(RouteClosureId id);

    /** Returns closures ordered by closure date (newest first), then by route code. */
    PageResult<RouteClosure> find(RouteClosureFilter filter, PageQuery page);
}
