package dev.franjaresc.wolfandfox.route.domain;

import java.time.LocalDate;

/** A route can be closed for today or an earlier day, never for a day that hasn't happened yet. */
public class ClosureDateInFutureException extends InvalidRouteClosureException {

    public ClosureDateInFutureException(LocalDate closureDate, LocalDate today) {
        super("Closure date %s is after today, %s".formatted(closureDate, today));
    }
}
