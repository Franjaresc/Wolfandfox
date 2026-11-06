package dev.franjaresc.wolfandfox.route.adapter.out.persistence;

import dev.franjaresc.wolfandfox.route.domain.DriverCode;
import dev.franjaresc.wolfandfox.route.domain.Kilometres;
import dev.franjaresc.wolfandfox.route.domain.PackageCounts;
import dev.franjaresc.wolfandfox.route.domain.RouteClosure;
import dev.franjaresc.wolfandfox.route.domain.RouteClosureId;
import dev.franjaresc.wolfandfox.route.domain.RouteCode;
import dev.franjaresc.wolfandfox.route.domain.UserId;
import dev.franjaresc.wolfandfox.route.domain.VehicleCode;

final class RouteClosureJpaMapper {

    private RouteClosureJpaMapper() {
    }

    static RouteClosureJpaEntity toEntity(RouteClosure closure) {
        return new RouteClosureJpaEntity(
                closure.id().value(),
                closure.routeCode().value(),
                closure.closureDate(),
                closure.vehicleCode().value(),
                closure.driverCode().value(),
                closure.packages().delivered(),
                closure.packages().failed(),
                closure.packages().returned(),
                closure.kmDriven().value(),
                closure.notes(),
                closure.closedBy().value(),
                closure.closedAt());
    }

    static RouteClosure toDomain(RouteClosureJpaEntity entity) {
        return new RouteClosure(
                new RouteClosureId(entity.getId()),
                new RouteCode(entity.getRouteCode()),
                entity.getClosureDate(),
                new VehicleCode(entity.getVehicleCode()),
                new DriverCode(entity.getDriverCode()),
                new PackageCounts(
                        entity.getPackagesDelivered(), entity.getPackagesFailed(), entity.getPackagesReturned()),
                new Kilometres(entity.getKmDriven()),
                entity.getNotes(),
                new UserId(entity.getClosedBy()),
                entity.getClosedAt());
    }
}
