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

    /**
     * Records a new closure with a fresh id.
     *
     * @param today the current business date, from the caller's clock. Passing it in keeps the domain
     *     deterministic and free of any clock or time zone.
     * @throws ClosureDateInFutureException if the closure date is after today
     */
    public static RouteClosure close(
            RouteCode routeCode,
            LocalDate closureDate,
            VehicleCode vehicleCode,
            DriverCode driverCode,
            PackageCounts packages,
            Kilometres kmDriven,
            String notes,
            UserId closedBy,
            Instant closedAt,
            LocalDate today) {
        requirePresent(today, "Today");
        // Only new closures are checked. A stored closure stays valid as the calendar moves on.
        if (closureDate != null && closureDate.isAfter(today)) {
            throw new ClosureDateInFutureException(closureDate, today);
        }
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
