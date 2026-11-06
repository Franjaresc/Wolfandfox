package dev.franjaresc.wolfandfox.route.domain;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The end-of-day result of a route. Uniqueness per route code and date spans many closures, so the
 * application layer and the database enforce it rather than this record.
 */
public record RouteClosure(
        RouteClosureId id,
        RouteCode routeCode,
        LocalDate closureDate,
        VehicleCode vehicleCode,
        DriverCode driverCode,
        PackageCounts packages,
        Kilometres kmDriven,
        String notes,
        UserId closedBy,
        Instant closedAt) {

    private static final int MAX_NOTES_LENGTH = 1000;

    public RouteClosure {
        requirePresent(id, "Id");
        requirePresent(routeCode, "Route code");
        requirePresent(closureDate, "Closure date");
        requirePresent(vehicleCode, "Vehicle code");
        requirePresent(driverCode, "Driver code");
        requirePresent(packages, "Package counts");
        requirePresent(kmDriven, "Kilometres");
        requirePresent(closedBy, "Closed by");
        requirePresent(closedAt, "Closed at");
        if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
            throw new InvalidRouteClosureException(
                    "Notes must not be longer than %d characters".formatted(MAX_NOTES_LENGTH));
        }
    }

    /** Records a new closure with a fresh id. */
    public static RouteClosure close(
            RouteCode routeCode,
            LocalDate closureDate,
            VehicleCode vehicleCode,
            DriverCode driverCode,
            PackageCounts packages,
            Kilometres kmDriven,
            String notes,
            UserId closedBy,
            Instant closedAt) {
        return new RouteClosure(
                RouteClosureId.newId(),
                routeCode,
                closureDate,
                vehicleCode,
                driverCode,
                packages,
                kmDriven,
                notes,
                closedBy,
                closedAt);
    }

    private static void requirePresent(Object value, String label) {
        if (value == null) {
            throw new InvalidRouteClosureException(label + " is required");
        }
    }
}
