package dev.franjaresc.wolfandfox.route.domain;

public class RouteClosureNotFoundException extends RuntimeException {

    public RouteClosureNotFoundException(RouteClosureId id) {
        super("No route closure with id " + id.value());
    }
}
