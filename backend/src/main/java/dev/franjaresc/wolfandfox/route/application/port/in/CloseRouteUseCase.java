package dev.franjaresc.wolfandfox.route.application.port.in;

import dev.franjaresc.wolfandfox.route.domain.DriverCode;
import dev.franjaresc.wolfandfox.route.domain.Kilometres;
import dev.franjaresc.wolfandfox.route.domain.PackageCounts;
import dev.franjaresc.wolfandfox.route.domain.RouteClosure;
import dev.franjaresc.wolfandfox.route.domain.RouteCode;
import dev.franjaresc.wolfandfox.route.domain.VehicleCode;
import java.time.LocalDate;

public interface CloseRouteUseCase {

    /**
     * Closes a route for the day.
     *
     * @throws dev.franjaresc.wolfandfox.route.domain.DuplicateRouteClosureException if the route is
     *     already closed for that date
     */
    RouteClosure close(CloseRouteCommand command);

    record CloseRouteCommand(
            RouteCode routeCode,
            LocalDate closureDate,
            VehicleCode vehicleCode,
            DriverCode driverCode,
            PackageCounts packages,
            Kilometres kmDriven,
            String notes) {
    }
}
