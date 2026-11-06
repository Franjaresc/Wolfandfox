package dev.franjaresc.wolfandfox.route.adapter.in.web;

import dev.franjaresc.wolfandfox.route.adapter.in.web.generated.model.CloseRouteRequestDto;
import dev.franjaresc.wolfandfox.route.adapter.in.web.generated.model.RouteClosureDto;
import dev.franjaresc.wolfandfox.route.adapter.in.web.generated.model.RouteClosurePageDto;
import dev.franjaresc.wolfandfox.route.application.port.PageResult;
import dev.franjaresc.wolfandfox.route.application.port.in.CloseRouteUseCase.CloseRouteCommand;
import dev.franjaresc.wolfandfox.route.domain.DriverCode;
import dev.franjaresc.wolfandfox.route.domain.Kilometres;
import dev.franjaresc.wolfandfox.route.domain.PackageCounts;
import dev.franjaresc.wolfandfox.route.domain.RouteClosure;
import dev.franjaresc.wolfandfox.route.domain.RouteCode;
import dev.franjaresc.wolfandfox.route.domain.VehicleCode;
import java.time.ZoneOffset;

final class RouteClosureWebMapper {

    private RouteClosureWebMapper() {
    }

    static CloseRouteCommand toCommand(CloseRouteRequestDto request) {
        return new CloseRouteCommand(
                new RouteCode(request.getRouteCode()),
                request.getClosureDate(),
                new VehicleCode(request.getVehicleCode()),
                new DriverCode(request.getDriverCode()),
                new PackageCounts(
                        request.getPackagesDelivered(), request.getPackagesFailed(), request.getPackagesReturned()),
                new Kilometres(request.getKmDriven()),
                request.getNotes());
    }

    static RouteClosureDto toDto(RouteClosure closure) {
        return new RouteClosureDto()
                .id(closure.id().value())
                .routeCode(closure.routeCode().value())
                .closureDate(closure.closureDate())
                .vehicleCode(closure.vehicleCode().value())
                .driverCode(closure.driverCode().value())
                .packagesDelivered(closure.packages().delivered())
                .packagesFailed(closure.packages().failed())
                .packagesReturned(closure.packages().returned())
                .totalPackages(closure.packages().total())
                .kmDriven(closure.kmDriven().value())
                .notes(closure.notes())
                .closedBy(closure.closedBy().value())
                .closedAt(closure.closedAt().atOffset(ZoneOffset.UTC));
    }

    static RouteClosurePageDto toPageDto(PageResult<RouteClosure> result) {
        return new RouteClosurePageDto()
                .items(result.items().stream().map(RouteClosureWebMapper::toDto).toList())
                .page(result.query().page())
                .size(result.query().size())
                .totalElements(result.totalElements())
                .totalPages(result.totalPages());
    }
}
