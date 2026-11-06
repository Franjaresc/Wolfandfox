package dev.franjaresc.wolfandfox.route.domain;

/** A value breaks one of the route closure invariants. */
public class InvalidRouteClosureException extends RuntimeException {

    public InvalidRouteClosureException(String message) {
        super(message);
    }
}
