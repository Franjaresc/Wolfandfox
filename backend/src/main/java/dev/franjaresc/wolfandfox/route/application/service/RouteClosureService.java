package dev.franjaresc.wolfandfox.route.application.service;

import dev.franjaresc.wolfandfox.route.application.port.PageQuery;
import dev.franjaresc.wolfandfox.route.application.port.PageResult;
import dev.franjaresc.wolfandfox.route.application.port.RouteClosureFilter;
import dev.franjaresc.wolfandfox.route.application.port.in.CloseRouteUseCase;
import dev.franjaresc.wolfandfox.route.application.port.in.FindRouteClosuresQuery;
import dev.franjaresc.wolfandfox.route.application.port.in.GetRouteClosureQuery;
import dev.franjaresc.wolfandfox.route.application.port.out.CurrentUserPort;
import dev.franjaresc.wolfandfox.route.application.port.out.RouteClosureRepositoryPort;
import dev.franjaresc.wolfandfox.route.domain.DuplicateRouteClosureException;
import dev.franjaresc.wolfandfox.route.domain.RouteClosure;
import dev.franjaresc.wolfandfox.route.domain.RouteClosureId;
import dev.franjaresc.wolfandfox.route.domain.RouteClosureNotFoundException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.transaction.annotation.Transactional;

public class RouteClosureService implements CloseRouteUseCase, FindRouteClosuresQuery, GetRouteClosureQuery {

    private final RouteClosureRepositoryPort repository;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    public RouteClosureService(RouteClosureRepositoryPort repository, CurrentUserPort currentUser, Clock clock) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RouteClosure close(CloseRouteCommand command) {
        var closure = RouteClosure.close(
                command.routeCode(),
                command.closureDate(),
                command.vehicleCode(),
                command.driverCode(),
                command.packages(),
                command.kmDriven(),
                command.notes(),
                currentUser.currentUser(),
                // PostgreSQL stores microseconds. Truncating keeps create and read responses identical.
                clock.instant().truncatedTo(ChronoUnit.MICROS));

        // The database constraint is the real guarantee. This check only avoids a failed insert.
        if (repository.existsByRouteCodeAndClosureDate(closure.routeCode(), closure.closureDate())) {
            throw new DuplicateRouteClosureException(closure.routeCode(), closure.closureDate());
        }
        return repository.save(closure);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RouteClosure> find(RouteClosureFilter filter, PageQuery page) {
        return repository.find(filter, page);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteClosure get(RouteClosureId id) {
        return repository.findById(id).orElseThrow(() -> new RouteClosureNotFoundException(id));
    }
}
