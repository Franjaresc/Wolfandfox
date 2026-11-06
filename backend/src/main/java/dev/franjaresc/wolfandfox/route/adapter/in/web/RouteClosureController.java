package dev.franjaresc.wolfandfox.route.adapter.in.web;

import dev.franjaresc.wolfandfox.route.adapter.in.web.generated.api.RouteClosuresApi;
import dev.franjaresc.wolfandfox.route.adapter.in.web.generated.model.CloseRouteRequestDto;
import dev.franjaresc.wolfandfox.route.adapter.in.web.generated.model.RouteClosureDto;
import dev.franjaresc.wolfandfox.route.adapter.in.web.generated.model.RouteClosurePageDto;
import dev.franjaresc.wolfandfox.route.application.port.PageQuery;
import dev.franjaresc.wolfandfox.route.application.port.RouteClosureFilter;
import dev.franjaresc.wolfandfox.route.application.port.in.CloseRouteUseCase;
import dev.franjaresc.wolfandfox.route.application.port.in.FindRouteClosuresQuery;
import dev.franjaresc.wolfandfox.route.application.port.in.GetRouteClosureQuery;
import dev.franjaresc.wolfandfox.route.domain.RouteClosureId;
import dev.franjaresc.wolfandfox.route.domain.RouteCode;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Routes, parameters and validation come from the generated interface, so they follow the contract. */
@RestController
class RouteClosureController implements RouteClosuresApi {

    private final CloseRouteUseCase closeRoute;
    private final FindRouteClosuresQuery findRouteClosures;
    private final GetRouteClosureQuery getRouteClosure;

    RouteClosureController(
            CloseRouteUseCase closeRoute,
            FindRouteClosuresQuery findRouteClosures,
            GetRouteClosureQuery getRouteClosure) {
        this.closeRoute = closeRoute;
        this.findRouteClosures = findRouteClosures;
        this.getRouteClosure = getRouteClosure;
    }

    @Override
    public ResponseEntity<RouteClosureDto> closeRoute(CloseRouteRequestDto request) {
        var closure = closeRoute.close(RouteClosureWebMapper.toCommand(request));
        // Relative, so it stays correct behind a proxy and never echoes the Host header.
        var location = URI.create("/api/v1/route-closures/" + closure.id().value());
        return ResponseEntity.created(location).body(RouteClosureWebMapper.toDto(closure));
    }

    @Override
    public ResponseEntity<RouteClosureDto> getRouteClosure(UUID id) {
        return ResponseEntity.ok(RouteClosureWebMapper.toDto(getRouteClosure.get(new RouteClosureId(id))));
    }

    @Override
    public ResponseEntity<RouteClosurePageDto> listRouteClosures(
            LocalDate date, String routeCode, Integer page, Integer size) {
        var filter = new RouteClosureFilter(date, routeCode == null ? null : new RouteCode(routeCode));
        var result = findRouteClosures.find(filter, new PageQuery(page, size));
        return ResponseEntity.ok(RouteClosureWebMapper.toPageDto(result));
    }
}
